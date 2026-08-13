package com.example.findmyphonebyclaplauncher.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log

/**
 * Helpers for battery optimisation guidance.
 *
 * Continuous microphone detection is affected by aggressive OEM battery
 * management. This object helps guide the user to disable battery restrictions
 * without using hidden or undocumented APIs.
 */
object BatteryOptimizationManager {

    private const val TAG = "BatteryOptimizationMgr"

    // ─────────────────────────────────────────────────────────────────────────
    // State
    // ─────────────────────────────────────────────────────────────────────────

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            pm.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true // No restriction on older versions
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Settings navigation
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Opens the system dialog asking the user to allow unrestricted battery
     * usage for this app.  Fails gracefully on OEMs that block this intent.
     */
    fun requestIgnoreBatteryOptimizations(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                ).apply {
                    data  = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "requestIgnoreBatteryOptimizations failed: ${e.message}")
                openBatteryOptimizationList(context)
            }
        }
    }

    /**
     * Opens the device's battery optimisation list (fallback when the direct
     * per-app request is not available).
     */
    fun openBatteryOptimizationList(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS not available: ${e.message}")
        }
    }
}
