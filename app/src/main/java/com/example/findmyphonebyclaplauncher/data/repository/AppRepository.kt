package com.example.findmyphonebyclaplauncher.data.repository

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import android.provider.Telephony
import android.telecom.TelecomManager
import android.util.Log
import com.example.findmyphonebyclaplauncher.data.model.AppCategory
import com.example.findmyphonebyclaplauncher.data.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Singleton launcher app repository — no Hilt needed.
 * Call [init] once from [App.onCreate], then use [get] everywhere.
 */
object AppRepository {

    private lateinit var impl: AppRepositoryImpl

    fun init(context: Context) {
        if (!::impl.isInitialized) {
            impl = AppRepositoryImpl(context.applicationContext)
        }
    }

    fun get(): AppRepositoryImpl = impl
}

class AppRepositoryImpl(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    private val favoritePackages = MutableStateFlow(loadFavoritePackages())

    val apps: Flow<List<AppInfo>> = combine(_apps, favoritePackages) { list, favorites ->
        list.map { app -> app.copy(isFavorite = app.packageName in favorites) }
    }

    val hasFavorites: Flow<Boolean> = favoritePackages.map { it.isNotEmpty() }

    suspend fun refreshApps() = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolveInfos = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        val favorites = favoritePackages.value
        val mapped = resolveInfos
            .asSequence()
            .mapNotNull { info ->
                com.example.findmyphonebyclaplauncher.data.cache.AppIconCacheManager.getAppInfo(
                    pm = pm,
                    info = info,
                    favorites = favorites,
                    canUninstallFunc = { pkg -> canUninstall(pm, pkg) },
                    categorizeFunc = { pkg, label -> categorize(pm, pkg, label) }
                )
            }
            .distinctBy { it.packageName + it.activityName }
            .sortedBy { it.label.lowercase() }
            .toList()
        _apps.value = mapped
        pruneMissingFavorites(mapped.map { it.packageName }.toSet())
    }

    private val repositoryScope = kotlinx.coroutines.CoroutineScope(Dispatchers.IO + kotlinx.coroutines.SupervisorJob())

    fun refreshAppsAsync() {
        repositoryScope.launch {
            refreshApps()
        }
    }

    fun invalidatePackageCache(packageName: String) {
        com.example.findmyphonebyclaplauncher.data.cache.AppIconCacheManager.invalidatePackage(packageName)
    }

    fun searchApps(query: String): Flow<List<AppInfo>> =
        apps.map { list ->
            if (query.isBlank()) list
            else list.filter { it.label.contains(query, ignoreCase = true) }
        }

    fun appsByCategory(category: AppCategory): Flow<List<AppInfo>> =
        apps.map { list ->
            when (category) {
                AppCategory.ALL -> list
                AppCategory.FAVORITES -> list.filter { it.isFavorite }
                else -> list.filter { it.category == category }
            }
        }

    fun launchApp(app: AppInfo) {
        val intent = if (app.activityName.isNotBlank()) {
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setClassName(app.packageName, app.activityName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        } else {
            context.packageManager.getLaunchIntentForPackage(app.packageName)
                ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ?: Intent(Intent.ACTION_DIAL).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
            .recoverCatching {
                val fallback = context.packageManager.getLaunchIntentForPackage(app.packageName)
                    ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    ?: Intent(Intent.ACTION_DIAL).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(fallback)
            }
    }

    fun openAppInfo(app: AppInfo) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.fromParts("package", app.packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    fun uninstallApp(app: AppInfo): Boolean {
        return uninstallApp(context, app.packageName)
    }

    fun uninstallApp(ctx: Context, packageName: String): Boolean {
        return try {
            Log.d("LauncherUninstall", "Attempting uninstall for: $packageName")
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.fromParts("package", packageName, null)
                putExtra(Intent.EXTRA_RETURN_RESULT, true)
                if (ctx !is android.app.Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            ctx.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e("LauncherUninstall", "Failed to launch uninstaller for package: $packageName", e)
            try {
                @Suppress("DEPRECATION")
                val fallbackIntent = Intent(Intent.ACTION_UNINSTALL_PACKAGE).apply {
                    data = Uri.fromParts("package", packageName, null)
                    putExtra(Intent.EXTRA_RETURN_RESULT, true)
                    if (ctx !is android.app.Activity) {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
                ctx.startActivity(fallbackIntent)
                true
            } catch (fallbackError: Exception) {
                Log.e("LauncherUninstall", "Fallback uninstaller also failed for: $packageName", fallbackError)
                false
            }
        }
    }

    suspend fun toggleFavorite(app: AppInfo) = withContext(Dispatchers.IO) {
        val updated = favoritePackages.value.toMutableSet()
        if (app.packageName in updated) updated.remove(app.packageName)
        else updated.add(app.packageName)
        favoritePackages.value = updated
        prefs.edit().putStringSet(KEY_FAVORITES, HashSet(updated)).apply()
    }

    fun getDockApps(): Flow<List<AppInfo>> =
        apps.map { list ->
            resolveDockApps(list)
        }

    private fun resolveDockApps(list: List<AppInfo>): List<AppInfo> {
        val pm = context.packageManager
        val myPkg = context.packageName
        val picked = mutableListOf<AppInfo>()
        val used = mutableSetOf<String>()

        // 1. Phone App (Slot 0)
        val phoneApp = resolvePhoneApp(pm, list, myPkg)
        if (phoneApp != null) {
            picked += phoneApp
            used += phoneApp.packageName
        }

        // 2. Messaging App (Slot 1)
        val messagingApp = resolveMessagingApp(pm, list, myPkg, used)
        if (messagingApp != null) {
            picked += messagingApp
            used += messagingApp.packageName
        }

        // 3. Camera App (Slot 2)
        val cameraApp = resolveCameraApp(pm, list, myPkg, used)
        if (cameraApp != null) {
            picked += cameraApp
            used += cameraApp.packageName
        }

        // 4. Settings App (Slot 3)
        val settingsApp = resolveSettingsApp(pm, list, myPkg, used)
        if (settingsApp != null) {
            picked += settingsApp
            used += settingsApp.packageName
        }

        // 5. Browser App (Slot 4)
        val browserApp = resolveBrowserApp(pm, list, myPkg, used)
        if (browserApp != null) {
            picked += browserApp
            used += browserApp.packageName
        }

        // Fill remaining slots up to 5 if needed, strictly excluding myPkg
        if (picked.size < 5) {
            val fill = list.filter { it.packageName !in used && it.packageName != myPkg }
            for (app in fill) {
                if (picked.size >= 5) break
                picked += app
                used += app.packageName
            }
        }

        return picked.take(5)
    }

    private fun resolvePhoneApp(pm: PackageManager, list: List<AppInfo>, myPkg: String): AppInfo? {
        // Step 1: Query TelecomManager default dialer package (Android 6.0+)
        var candidatePkg: String? = null
        try {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            candidatePkg = telecomManager?.defaultDialerPackage
        } catch (_: Exception) {}

        // Step 2: Intent resolution for ACTION_DIAL or ACTION_CALL_BUTTON
        if (candidatePkg.isNullOrBlank() || candidatePkg == myPkg) {
            runCatching {
                val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:")
                }
                val resolved = pm.resolveActivity(dialIntent, PackageManager.MATCH_DEFAULT_ONLY)
                    ?: pm.resolveActivity(Intent(Intent.ACTION_DIAL), PackageManager.MATCH_DEFAULT_ONLY)
                    ?: pm.resolveActivity(Intent(Intent.ACTION_CALL_BUTTON), PackageManager.MATCH_DEFAULT_ONLY)
                val pkg = resolved?.activityInfo?.packageName
                if (!pkg.isNullOrBlank() && pkg != myPkg) {
                    candidatePkg = pkg
                }
            }
        }

        // Check if candidatePkg matches an AppInfo in list or create AppInfo for candidatePkg
        if (!candidatePkg.isNullOrBlank() && candidatePkg != myPkg) {
            val appInList = list.firstOrNull { it.packageName.equals(candidatePkg, ignoreCase = true) }
            if (appInList != null) return appInList

            val customAppInfo = createAppInfoForPackage(pm, candidatePkg!!)
            if (customAppInfo != null) return customAppInfo
        }

        // Step 3: Known Dialer Package names fallback across major Android OEMs
        val knownPackages = listOf(
            "com.google.android.dialer",
            "com.samsung.android.dialer",
            "com.android.dialer",
            "com.miui.dialer",
            "com.coloros.selectpage",
            "com.oneplus.dialer",
            "com.vivo.phone",
            "com.transsion.phone",
            "com.asus.dialer",
            "com.sh.smartdialer",
            "com.android.incallui",
            "com.samsung.android.incallui",
            "com.huawei.contacts",
            "com.android.contacts",
            "com.lge.dialer"
        )
        for (pkg in knownPackages) {
            val match = list.firstOrNull { it.packageName.equals(pkg, ignoreCase = true) }
            if (match != null && match.packageName != myPkg) return match
        }

        // Step 4: Label matching (STRICTLY exclude myPkg!)
        val labelExact = listOf("phone", "dialer", "telephone", "calls", "phone call")
        val exactMatch = list.firstOrNull { app ->
            app.packageName != myPkg && labelExact.any { app.label.equals(it, ignoreCase = true) }
        }
        if (exactMatch != null) return exactMatch

        val substringMatch = list.firstOrNull { app ->
            app.packageName != myPkg &&
                (app.label.contains("phone", ignoreCase = true) || app.label.contains("dialer", ignoreCase = true)) &&
                !app.packageName.equals(myPkg, ignoreCase = true)
        }
        return substringMatch
    }

    private fun resolveMessagingApp(pm: PackageManager, list: List<AppInfo>, myPkg: String, used: Set<String>): AppInfo? {
        var candidatePkg: String? = null
        runCatching {
            candidatePkg = Telephony.Sms.getDefaultSmsPackage(context)
        }
        if (candidatePkg.isNullOrBlank() || candidatePkg == myPkg) {
            runCatching {
                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"))
                val resolved = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
                val pkg = resolved?.activityInfo?.packageName
                if (!pkg.isNullOrBlank() && pkg != myPkg) candidatePkg = pkg
            }
        }
        if (!candidatePkg.isNullOrBlank() && candidatePkg != myPkg && candidatePkg !in used) {
            val match = list.firstOrNull { it.packageName.equals(candidatePkg, true) }
            if (match != null) return match
            val custom = createAppInfoForPackage(pm, candidatePkg!!)
            if (custom != null) return custom
        }

        val knownPackages = listOf("com.google.android.apps.messaging", "com.samsung.android.messaging", "com.android.mms", "com.miui.mms", "com.coloros.mms", "com.vivo.mms", "com.oneplus.mms", "com.transsion.messaging")
        for (pkg in knownPackages) {
            val match = list.firstOrNull { it.packageName.equals(pkg, true) && it.packageName !in used }
            if (match != null && match.packageName != myPkg) return match
        }

        return list.firstOrNull { app ->
            app.packageName !in used && app.packageName != myPkg &&
                listOf("message", "messaging", "sms").any { app.label.contains(it, ignoreCase = true) }
        }
    }

    private fun resolveCameraApp(pm: PackageManager, list: List<AppInfo>, myPkg: String, used: Set<String>): AppInfo? {
        var candidatePkg: String? = null
        runCatching {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val resolved = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            val pkg = resolved?.activityInfo?.packageName
            if (!pkg.isNullOrBlank() && pkg != myPkg) candidatePkg = pkg
        }
        if (!candidatePkg.isNullOrBlank() && candidatePkg != myPkg && candidatePkg !in used) {
            val match = list.firstOrNull { it.packageName.equals(candidatePkg, true) }
            if (match != null) return match
            val custom = createAppInfoForPackage(pm, candidatePkg!!)
            if (custom != null) return custom
        }

        val knownPackages = listOf("com.google.android.GoogleCamera", "com.sec.android.app.camera", "com.android.camera", "com.android.camera2", "com.miui.camera", "com.oppo.camera", "com.oneplus.camera", "com.vivo.camera", "com.transsion.camera")
        for (pkg in knownPackages) {
            val match = list.firstOrNull { it.packageName.equals(pkg, true) && it.packageName !in used }
            if (match != null && match.packageName != myPkg) return match
        }

        return list.firstOrNull { app ->
            app.packageName !in used && app.packageName != myPkg &&
                app.label.contains("camera", ignoreCase = true)
        }
    }

    private fun resolveSettingsApp(pm: PackageManager, list: List<AppInfo>, myPkg: String, used: Set<String>): AppInfo? {
        var candidatePkg: String? = null
        runCatching {
            val intent = Intent(Settings.ACTION_SETTINGS)
            val resolved = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            val pkg = resolved?.activityInfo?.packageName
            if (!pkg.isNullOrBlank() && pkg != myPkg) candidatePkg = pkg
        }
        if (!candidatePkg.isNullOrBlank() && candidatePkg != myPkg && candidatePkg !in used) {
            val match = list.firstOrNull { it.packageName.equals(candidatePkg, true) }
            if (match != null) return match
            val custom = createAppInfoForPackage(pm, candidatePkg!!)
            if (custom != null) return custom
        }

        val knownPackages = listOf("com.android.settings", "com.miui.securitycenter")
        for (pkg in knownPackages) {
            val match = list.firstOrNull { it.packageName.equals(pkg, true) && it.packageName !in used }
            if (match != null && match.packageName != myPkg) return match
        }

        return list.firstOrNull { app ->
            app.packageName !in used && app.packageName != myPkg &&
                app.label.contains("setting", ignoreCase = true)
        }
    }

    private fun resolveBrowserApp(pm: PackageManager, list: List<AppInfo>, myPkg: String, used: Set<String>): AppInfo? {
        var candidatePkg: String? = null
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
            val resolved = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            val pkg = resolved?.activityInfo?.packageName
            if (!pkg.isNullOrBlank() && pkg != myPkg) candidatePkg = pkg
        }
        if (!candidatePkg.isNullOrBlank() && candidatePkg != myPkg && candidatePkg !in used) {
            val match = list.firstOrNull { it.packageName.equals(candidatePkg, true) }
            if (match != null) return match
            val custom = createAppInfoForPackage(pm, candidatePkg!!)
            if (custom != null) return custom
        }

        val knownPackages = listOf("com.android.chrome", "com.chrome.beta", "org.mozilla.firefox", "com.opera.browser", "com.sec.android.app.sbrowser", "com.mi.globalbrowser", "com.heytap.browser", "com.vivo.browser")
        for (pkg in knownPackages) {
            val match = list.firstOrNull { it.packageName.equals(pkg, true) && it.packageName !in used }
            if (match != null && match.packageName != myPkg) return match
        }

        return list.firstOrNull { app ->
            app.packageName !in used && app.packageName != myPkg &&
                listOf("chrome", "browser", "internet").any { app.label.contains(it, ignoreCase = true) }
        }
    }

    private fun createAppInfoForPackage(pm: PackageManager, packageName: String): AppInfo? {
        return try {
            val launchIntent = pm.getLaunchIntentForPackage(packageName)
            val activityName = launchIntent?.component?.className.orEmpty()
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val label = pm.getApplicationLabel(appInfo).toString()
            val icon = pm.getApplicationIcon(appInfo)
            AppInfo(
                packageName = packageName,
                activityName = activityName,
                label = label.ifBlank { packageName },
                icon = icon,
                category = AppCategory.OTHERS,
                isFavorite = false,
                canUninstall = false
            )
        } catch (_: Exception) {
            null
        }
    }

    fun getRecentApps(): Flow<List<AppInfo>> =
        apps.map { list ->
            if (list.isEmpty()) return@map emptyList()
            val recentPkgs = getRecentPackagesFromUsageStats(limit = 16)
            val fromUsage = recentPkgs.mapNotNull { pkg -> list.firstOrNull { it.packageName == pkg } }
            if (fromUsage.isNotEmpty()) fromUsage.distinctBy { it.packageName }.take(4)
            else list.filter { it.isFavorite }.take(4).ifEmpty { list.take(4) }
        }

    fun getSuggestedApps(): Flow<List<AppInfo>> =
        apps.map { list ->
            if (list.isEmpty()) return@map emptyList()
            val recent = getRecentPackagesFromUsageStats(16).toSet()
            list.filter { it.packageName !in recent }.take(4).ifEmpty { list.drop(4).take(4) }
        }

    private fun getRecentPackagesFromUsageStats(limit: Int): List<String> {
        return try {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val now = System.currentTimeMillis()
            val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - 7 * 86_400_000L, now)
            stats?.sortedByDescending { it.lastTimeUsed }
                ?.map { it.packageName }
                ?.filter { it != context.packageName }
                ?.distinct()
                ?.take(limit)
                ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun canUninstall(pm: PackageManager, packageName: String): Boolean {
        if (packageName == context.packageName) return false
        return try {
            val info = pm.getApplicationInfo(packageName, 0)
            val isSystem = info.flags and ApplicationInfo.FLAG_SYSTEM != 0
            val isUpdatedSystem = info.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0
            !isSystem || isUpdatedSystem
        } catch (_: Exception) {
            false
        }
    }

    private fun loadFavoritePackages(): Set<String> =
        prefs.getStringSet(KEY_FAVORITES, emptySet())?.toSet().orEmpty()

    private fun pruneMissingFavorites(installedPackages: Set<String>) {
        val current = favoritePackages.value
        val pruned = current.filter { it in installedPackages }.toSet()
        if (pruned != current) {
            favoritePackages.value = pruned
            prefs.edit().putStringSet(KEY_FAVORITES, HashSet(pruned)).apply()
        }
    }

    private fun categorize(pm: PackageManager, packageName: String, label: String): AppCategory {
        return try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            when (appInfo.category) {
                ApplicationInfo.CATEGORY_GAME -> AppCategory.GAMES
                ApplicationInfo.CATEGORY_AUDIO -> AppCategory.AUDIO
                ApplicationInfo.CATEGORY_VIDEO -> AppCategory.VIDEO
                ApplicationInfo.CATEGORY_IMAGE -> AppCategory.PHOTOGRAPHY
                ApplicationInfo.CATEGORY_SOCIAL -> AppCategory.SOCIAL
                ApplicationInfo.CATEGORY_PRODUCTIVITY -> AppCategory.PRODUCTIVITY
                else -> categorizeByKeywords(packageName, label)
            }
        } catch (_: Exception) {
            categorizeByKeywords(packageName, label)
        }
    }

    private fun categorizeByKeywords(packageName: String, label: String): AppCategory {
        val lower = "$packageName $label".lowercase()
        return when {
            listOf("music", "spotify", "audio", "podcast", "radio", "gaana", "wynk").any { lower.contains(it) } -> AppCategory.AUDIO
            listOf("game", "pubg", "cod", "freefire", "roblox", "minecraft", "clash").any { lower.contains(it) } -> AppCategory.GAMES
            listOf("camera", "gallery", "photo", "picsart", "snapseed", "lightroom").any { lower.contains(it) } -> AppCategory.PHOTOGRAPHY
            listOf("office", "docs", "sheets", "word", "excel", "notion", "calendar", "drive", "mail", "gmail", "zoom", "meet").any { lower.contains(it) } -> AppCategory.PRODUCTIVITY
            listOf("whatsapp", "instagram", "facebook", "telegram", "twitter", "snapchat", "discord", "tiktok").any { lower.contains(it) } -> AppCategory.SOCIAL
            listOf("youtube", "netflix", "hotstar", "prime video", "mx player", "vlc").any { lower.contains(it) } -> AppCategory.VIDEO
            else -> AppCategory.OTHERS
        }
    }

    private companion object {
        const val PREFS_NAME = "find_my_phone_launcher_prefs"
        const val KEY_FAVORITES = "favorite_packages"
    }
}
