package com.example.findmyphonebyclaplauncher.data.repository

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
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
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setClassName(app.packageName, app.activityName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
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
            val slots = listOf(
                listOf("com.google.android.dialer", "com.samsung.android.dialer", "com.android.dialer"),
                listOf("com.google.android.apps.messaging", "com.samsung.android.messaging", "com.android.mms"),
                listOf("com.google.android.GoogleCamera", "com.sec.android.app.camera", "com.android.camera2"),
                listOf("com.android.settings"),
                listOf("com.android.chrome", "com.chrome.beta")
            )
            val labelFallbacks = listOf("phone", "message", "camera", "settings", "chrome")
            val picked = mutableListOf<AppInfo>()
            val used = mutableSetOf<String>()

            slots.forEachIndexed { index, packages ->
                val byPackage = packages.firstNotNullOfOrNull { pkg ->
                    list.firstOrNull { it.packageName.equals(pkg, true) }
                }
                val byLabel = list.firstOrNull { app ->
                    app.packageName !in used &&
                        app.label.contains(labelFallbacks[index], ignoreCase = true)
                }
                val match = byPackage ?: byLabel
                if (match != null && match.packageName !in used) {
                    picked += match
                    used += match.packageName
                }
            }
            if (picked.size >= 5) picked.take(5)
            else (picked + list.filter { it.packageName !in used }).distinctBy { it.packageName }.take(5)
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
