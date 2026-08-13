package com.example.findmyphonebyclaplauncher.utils

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

/**
 * Handles OEM-specific workarounds for background execution restrictions.
 *
 * Several OEMs (Xiaomi, Oppo/Realme, Vivo, Huawei, Samsung, OnePlus) have
 * proprietary "autostart" or "background process manager" settings that are
 * NOT reachable via standard Android APIs.  This object:
 *
 *  1. Detects the device brand at runtime
 *  2. Attempts to open the OEM-specific settings screen
 *  3. Returns `false` gracefully when no OEM-specific screen is found
 *
 * Package names / class names can change between OEM firmware versions.
 * Always resolve the intent before launching to avoid crashes.
 */
object OemCompatibilityManager {

    private const val TAG = "OemCompatibilityManager"

    val deviceBrand: String
        get() = Build.MANUFACTURER.lowercase()

    val deviceModel: String
        get() = Build.MODEL

    // ─────────────────────────────────────────────────────────────────────────
    // Autostart settings
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Attempts to open the OEM autostart / background-app settings screen.
     *
     * @return `true` if an OEM-specific screen was opened, `false` otherwise.
     *         When `false` the caller should fall back to generic guidance.
     */
    fun openAutostartSettings(context: Context): Boolean {
        for (intent in autostartIntentsForBrand()) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                if (context.packageManager.resolveActivity(intent, 0) != null) {
                    context.startActivity(intent)
                    Log.d(TAG, "Opened autostart settings for brand: $deviceBrand")
                    return true
                }
            } catch (e: Exception) {
                Log.d(TAG, "Intent not resolvable: ${e.message}")
            }
        }
        Log.w(TAG, "No OEM autostart settings found for brand: $deviceBrand")
        return false
    }

    /** Returns `true` when this device brand has known autostart settings. */
    fun hasKnownAutostartSettings(): Boolean =
        autostartIntentsForBrand().isNotEmpty()

    // ─────────────────────────────────────────────────────────────────────────
    // OEM intent catalogue
    // ─────────────────────────────────────────────────────────────────────────

    private fun autostartIntentsForBrand(): List<Intent> = when {
        deviceBrand.containsAny("xiaomi", "redmi", "poco") -> listOf(
            Intent().setClassName(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity"
            )
        )
        deviceBrand.containsAny("oppo", "realme") -> listOf(
            Intent().setClassName(
                "com.coloros.safecenter",
                "com.coloros.safecenter.permission.startup.StartupAppListActivity"
            ),
            Intent().setClassName(
                "com.oppo.safe",
                "com.oppo.safe.permission.startup.StartupAppListActivity"
            ),
            Intent().setClassName(
                "com.coloros.oppoguardelf",
                "com.coloros.powermanager.fuelgaue.PowerUsageModelActivity"
            )
        )
        deviceBrand.containsAny("vivo") -> listOf(
            Intent().setClassName(
                "com.vivo.permissionmanager",
                "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
            )
        )
        deviceBrand.containsAny("huawei", "honor") -> listOf(
            Intent().setClassName(
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
            ),
            Intent().setClassName(
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.optimize.process.ProtectActivity"
            )
        )
        deviceBrand.containsAny("samsung") -> listOf(
            Intent().setClassName(
                "com.samsung.android.lool",
                "com.samsung.android.sm.ui.battery.BatteryActivity"
            ),
            Intent().setClassName(
                "com.samsung.android.sm",
                "com.samsung.android.sm.ui.battery.BatteryActivity"
            )
        )
        deviceBrand.containsAny("oneplus") -> listOf(
            Intent().setClassName(
                "com.oneplus.security",
                "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"
            )
        )
        deviceBrand.containsAny("asus") -> listOf(
            Intent().setClassName(
                "com.asus.mobilemanager",
                "com.asus.mobilemanager.entry.FunctionActivity"
            )
        )
        deviceBrand.containsAny("meizu") -> listOf(
            Intent().setClassName(
                "com.meizu.safe",
                "com.meizu.safe.permission.SmartPermissionActivity"
            )
        )
        else -> emptyList()
    }

    private fun String.containsAny(vararg substrings: String): Boolean =
        substrings.any { this.contains(it) }
}
