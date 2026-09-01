package com.example.findmyphonebyclaplauncher.util

import android.app.Activity
import android.os.Build
import android.view.View
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.findmyphonebyclaplauncher.ads.config.AdsConfigManager

object SystemUiHelper {

    /**
     * Applies sticky immersive mode to hide the system navigation bar.
     * When the user swipes up from the bottom edge, the navigation bar is temporarily revealed
     * and automatically hides again after interaction.
     */
    fun applyStickyImmersiveMode(activity: Activity) {
        if (activity.isFinishing || activity.isDestroyed) return
        if (!AdsConfigManager.config.systemHideNavigationBarAuto) return

        val window = activity.window ?: return
        val decorView = window.decorView

        val controller = WindowInsetsControllerCompat(window, decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.navigationBars())

        @Suppress("DEPRECATION")
        val flags = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        )
        @Suppress("DEPRECATION")
        if (decorView.systemUiVisibility != flags) {
            decorView.systemUiVisibility = flags
        }
    }
}
