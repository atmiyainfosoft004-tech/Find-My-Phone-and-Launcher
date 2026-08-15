package com.example.findmyphonebyclaplauncher.util

import android.app.WallpaperManager
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.widget.ImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads the device's current home wallpaper.
 *
 * Prefer showing wallpaper via the window (`FLAG_SHOW_WALLPAPER` + transparent backgrounds).
 * Use [applyTo] only when an ImageView copy is needed (e.g. drawer blur target).
 */
object SystemWallpaperHelper {

    suspend fun loadDrawable(context: Context): Drawable? = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val wm = WallpaperManager.getInstance(appContext)

        // 1) Full drawable
        runCatching { wm.drawable }.getOrNull()?.let { return@withContext it }

        // 2) Faster peek path
        runCatching { wm.fastDrawable }.getOrNull()?.let { return@withContext it }

        // 3) System wallpaper file
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            runCatching {
                wm.getWallpaperFile(WallpaperManager.FLAG_SYSTEM)?.use { pfd ->
                    val bitmap = BitmapFactory.decodeFileDescriptor(pfd.fileDescriptor)
                    if (bitmap != null) BitmapDrawable(appContext.resources, bitmap) else null
                }
            }.getOrNull()?.let { return@withContext it }
        }

        null
    }

    suspend fun applyTo(imageView: ImageView) {
        val drawable = loadDrawable(imageView.context)
        if (drawable != null) {
            imageView.setImageDrawable(drawable)
        } else {
            imageView.setImageDrawable(null)
        }
    }
}
