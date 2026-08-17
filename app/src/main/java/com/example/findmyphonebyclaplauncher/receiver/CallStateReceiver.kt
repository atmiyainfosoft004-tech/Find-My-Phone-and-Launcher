package com.example.findmyphonebyclaplauncher.receiver

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import com.example.findmyphonebyclaplauncher.ui.aftercall.AfterCallActivity
import com.example.findmyphonebyclaplauncher.ui.findphone.FindPhoneActivity
import com.example.findmyphonebyclaplauncher.utils.PermissionManager

class CallStateReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "CallStateReceiver"
        private var lastState = TelephonyManager.CALL_STATE_IDLE
        private var isIncoming = false
        private var savedNumber: String? = null
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val stateStr = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
            val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

            if (!number.isNullOrEmpty()) {
                savedNumber = number
            }

            val state = when (stateStr) {
                TelephonyManager.EXTRA_STATE_IDLE -> TelephonyManager.CALL_STATE_IDLE
                TelephonyManager.EXTRA_STATE_OFFHOOK -> TelephonyManager.CALL_STATE_OFFHOOK
                TelephonyManager.EXTRA_STATE_RINGING -> TelephonyManager.CALL_STATE_RINGING
                else -> TelephonyManager.CALL_STATE_IDLE
            }

            Log.d(TAG, "Call state changed: $stateStr (mapped: $state), lastState: $lastState, number: $number")

            if (state == TelephonyManager.CALL_STATE_RINGING) {
                isIncoming = true
            } else if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
                if (lastState != TelephonyManager.CALL_STATE_RINGING) {
                    isIncoming = false
                }
            }

            if (lastState != state) {
                if (state == TelephonyManager.CALL_STATE_IDLE) {
                    if (lastState == TelephonyManager.CALL_STATE_OFFHOOK || lastState == TelephonyManager.CALL_STATE_RINGING) {
                        Log.d(TAG, "Call ended or missed/rejected (transition from $lastState to $state). Launching After Call Screen.")
                        launchAfterCallScreen(context)
                    }
                }
                lastState = state
            }
        }
    }

    private fun launchAfterCallScreen(context: Context) {
        // Optional permission check logging for diagnostics
        if (!PermissionManager.hasReadPhoneStatePermission(context)) {
            Log.w(TAG, "READ_PHONE_STATE permission not granted")
        }

        val numberToPass = savedNumber
        val incomingToPass = isIncoming

        savedNumber = null
        isIncoming = false

        val i = Intent(context, AfterCallActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("EXTRA_NUMBER", numberToPass)
            putExtra("EXTRA_IS_INCOMING", incomingToPass)
        }

        try {
            if (Build.VERSION.SDK_INT >= 34) {
                val pendingIntent = PendingIntent.getActivity(
                    context, 0, i,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val options = ActivityOptions.makeBasic()
                @Suppress("DEPRECATION")
                options.pendingIntentBackgroundActivityStartMode = ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                pendingIntent.send(context, 0, i, null, null, null, options.toBundle())
            } else if (Build.VERSION.SDK_INT >= 29) {
                val launcherIntent = Intent(context, FindPhoneActivity::class.java).apply {
                    action = Intent.ACTION_MAIN
                    addCategory(Intent.CATEGORY_HOME)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    putExtra("LAUNCH_AFTER_CALL", true)
                    putExtra("EXTRA_NUMBER", numberToPass)
                    putExtra("EXTRA_IS_INCOMING", incomingToPass)
                }
                context.startActivity(launcherIntent)
            } else {
                context.startActivity(i)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start AfterCallActivity via PendingIntent/Launcher", e)
            try {
                context.startActivity(i)
            } catch (ex: Exception) {
                Log.e(TAG, "Failed to launch AfterCallActivity via direct startActivity fallback", ex)
            }
        }
    }
}
