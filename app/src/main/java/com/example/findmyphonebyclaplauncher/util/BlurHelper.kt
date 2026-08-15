package com.example.findmyphonebyclaplauncher.util

import android.app.Activity
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.View
import android.view.ViewGroup
import eightbitlab.com.blurview.BlurView
import eightbitlab.com.blurview.RenderScriptBlur

object BlurHelper {

    @Suppress("DEPRECATION")
    fun setup(blurView: BlurView, root: ViewGroup, overlayColor: Int, radius: Float = 16f) {
        val windowBackground: Drawable? = (root.context as? Activity)?.window?.decorView?.background
        val algorithm = RenderScriptBlur(blurView.context)
        blurView.setupWith(root, algorithm)
            .setFrameClearDrawable(windowBackground)
            .setBlurRadius(radius.coerceIn(1f, 25f))
            .setOverlayColor(overlayColor)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            blurView.clipToOutline = true
        }
    }

    fun setVisibleAnimated(view: View, visible: Boolean) {
        if (visible && view.visibility != View.VISIBLE) {
            view.alpha = 0f
            view.visibility = View.VISIBLE
            view.animate().alpha(1f).setDuration(180).start()
        } else if (!visible && view.visibility == View.VISIBLE) {
            view.animate().alpha(0f).setDuration(150).withEndAction {
                view.visibility = View.GONE
                view.alpha = 1f
            }.start()
        }
    }
}
