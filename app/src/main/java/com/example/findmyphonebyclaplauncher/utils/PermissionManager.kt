package com.example.findmyphonebyclaplauncher.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * Centralised helper for runtime permission checks.
 * Never call [ContextCompat.checkSelfPermission] directly in Activities/ViewModels —
 * use this object instead.
 */
object PermissionManager {

    // ─────────────────────────────────────────────────────────────────────────
    // Permission constants
    // ─────────────────────────────────────────────────────────────────────────

    val RECORD_AUDIO_PERMISSION: String
        get() = Manifest.permission.RECORD_AUDIO

    val NOTIFICATION_PERMISSION: String?
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.POST_NOTIFICATIONS
        } else {
            null
        }

    // ─────────────────────────────────────────────────────────────────────────
    // Checks
    // ─────────────────────────────────────────────────────────────────────────

    fun hasRecordAudioPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Not required below Android 13
        }
    }

    fun hasCameraPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

    fun hasReadPhoneStatePermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

    fun hasReadCallLogPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED

    fun hasReadContactsPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

    fun requiredAfterCallPermissions(): Array<String> {
        return arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_CONTACTS
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Navigation helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Opens the system app-details screen so the user can manually grant
     * a permission that was permanently denied.
     */
    fun openAppPermissionSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data  = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    /**
     * Returns the list of permissions that should be requested when the user
     * enables sound detection for the first time.
     */
    fun requiredDetectionPermissions(): Array<String> {
        val perms = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        return perms.toTypedArray()
    }
}
