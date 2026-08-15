package com.example.findmyphonebyclaplauncher.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.ActivityOptionsCompat
import com.example.findmyphonebyclaplauncher.R

/**
 * Standardized activity transition extension helpers for sliding animations across the app.
 */
fun Context.startActivityWithSlideAnimation(intent: Intent) {
    val options = ActivityOptionsCompat.makeCustomAnimation(
        this,
        R.anim.slide_in_right,
        R.anim.slide_out_left
    ).toBundle()
    startActivity(intent, options)
}

fun Activity.finishWithSlideAnimation() {
    finish()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        overrideActivityTransition(
            Activity.OVERRIDE_TRANSITION_CLOSE,
            R.anim.slide_in_left,
            R.anim.slide_out_right
        )
    } else {
        @Suppress("DEPRECATION")
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}
