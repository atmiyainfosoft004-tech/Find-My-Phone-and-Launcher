package com.example.findmyphonebyclaplauncher.data.cache

import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.collection.LruCache
import com.example.findmyphonebyclaplauncher.data.model.AppCategory
import com.example.findmyphonebyclaplauncher.data.model.AppInfo
import java.util.concurrent.ConcurrentHashMap

object AppIconCacheManager {

    private val maxMemoryKb = (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt().coerceAtLeast(4096)

    private val iconCache = object : LruCache<String, Bitmap>(maxMemoryKb) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    private val labelCache = ConcurrentHashMap<String, String>()
    private val appInfoCache = ConcurrentHashMap<String, AppInfo>()
    private val sizeCache = ConcurrentHashMap<String, String>()

    fun formatBytes(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(java.util.Locale.US, "%.1f GB", gb)
            mb >= 1.0 -> String.format(java.util.Locale.US, "%.1f MB", mb)
            kb >= 1.0 -> String.format(java.util.Locale.US, "%.0f KB", kb)
            else -> "$bytes B"
        }
    }

    fun cacheAppSizeFromSourceDir(packageName: String, sourceDir: String?) {
        if (sourceDir.isNullOrBlank()) return
        try {
            val file = java.io.File(sourceDir)
            if (file.exists()) {
                val sizeFormatted = formatBytes(file.length())
                sizeCache[packageName] = "Size: $sizeFormatted"
            }
        } catch (_: Exception) {
        }
    }

    fun getCachedAppSize(packageName: String): String? {
        return sizeCache[packageName]
    }

    fun getAppInfo(
        pm: PackageManager,
        info: ResolveInfo,
        favorites: Set<String>,
        canUninstallFunc: (String) -> Boolean,
        categorizeFunc: (String, String) -> AppCategory
    ): AppInfo? {
        val activityInfo = info.activityInfo ?: return null
        val key = "${activityInfo.packageName}/${activityInfo.name}"
        cacheAppSizeFromSourceDir(activityInfo.packageName, activityInfo.applicationInfo?.sourceDir)

        val cachedApp = appInfoCache[key]
        val isFav = activityInfo.packageName in favorites

        if (cachedApp != null) {
            return if (cachedApp.isFavorite == isFav) {
                cachedApp
            } else {
                val updated = cachedApp.copy(isFavorite = isFav)
                appInfoCache[key] = updated
                updated
            }
        }

        val label = labelCache.getOrPut(key) {
            info.loadLabel(pm)?.toString().orEmpty()
        }
        if (label.isBlank()) return null

        val drawableIcon = getOrLoadIcon(pm, info, key)
        val category = categorizeFunc(activityInfo.packageName, label)
        val canUninstall = canUninstallFunc(activityInfo.packageName)

        val appInfo = AppInfo(
            packageName = activityInfo.packageName,
            activityName = activityInfo.name,
            label = label,
            icon = drawableIcon,
            category = category,
            isFavorite = isFav,
            canUninstall = canUninstall
        )

        appInfoCache[key] = appInfo
        return appInfo
    }

    private fun getOrLoadIcon(pm: PackageManager, info: ResolveInfo, key: String): Drawable? {
        val cachedBitmap = iconCache.get(key)
        if (cachedBitmap != null && !cachedBitmap.isRecycled) {
            return BitmapDrawable(null, cachedBitmap)
        }

        val loadedDrawable = info.loadIcon(pm) ?: return null
        val bitmap = drawableToBitmap(loadedDrawable)
        if (bitmap != null) {
            iconCache.put(key, bitmap)
        }
        return loadedDrawable
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap? {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        return try {
            val width = drawable.intrinsicWidth.coerceAtLeast(1)
            val height = drawable.intrinsicHeight.coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        } catch (_: Exception) {
            null
        }
    }

    fun getCachedAppLabel(packageName: String): String? {
        val entry = appInfoCache.entries.firstOrNull { it.key.startsWith("$packageName/") }
        if (entry != null) return entry.value.label
        val labelEntry = labelCache.entries.firstOrNull { it.key.startsWith("$packageName/") }
        return labelEntry?.value
    }

    fun getCachedAppIconBitmap(packageName: String): Bitmap? {
        val entry = iconCache.snapshot().entries.firstOrNull { it.key.startsWith("$packageName/") }
        return entry?.value
    }

    fun saveIconBitmapToCacheFile(context: android.content.Context, packageName: String): String? {
        val bitmap = getCachedAppIconBitmap(packageName) ?: return null
        return try {
            val file = java.io.File(context.cacheDir, "app_icon_${packageName.replace('.', '_')}.png")
            java.io.FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            file.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    fun invalidatePackage(packageName: String) {
        val keysToRemove = appInfoCache.keys.filter { it.startsWith("$packageName/") }
        keysToRemove.forEach { key ->
            iconCache.remove(key)
            labelCache.remove(key)
            appInfoCache.remove(key)
        }
    }

    fun clear() {
        iconCache.evictAll()
        labelCache.clear()
        appInfoCache.clear()
    }
}
