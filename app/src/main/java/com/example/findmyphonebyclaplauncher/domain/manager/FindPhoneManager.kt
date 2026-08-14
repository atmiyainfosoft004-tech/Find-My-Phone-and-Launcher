package com.example.findmyphonebyclaplauncher.domain.manager

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.findmyphonebyclaplauncher.R
import com.example.findmyphonebyclaplauncher.data.local.UserPreferencesDataSource
import com.example.findmyphonebyclaplauncher.ui.alert.AlertActivity
import com.example.findmyphonebyclaplauncher.utils.Constants

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Centralized alert manager for Find My Phone.
 * Coordinates Sound, Flashlight, and Vibration alerts based on user preferences and handles auto-stop timers.
 */
class FindPhoneManager(private val context: Context) {

    private val tag = "FindPhoneManager"

    private var mediaPlayer: MediaPlayer? = null
    @Volatile
    private var isAlertActive = false

    private val _isAlertActiveState = MutableStateFlow(false)
    val isAlertActiveState: StateFlow<Boolean> = _isAlertActiveState.asStateFlow()

    private val flashlightManager = FlashlightManager(context)

    private val autoStopHandler = Handler(Looper.getMainLooper())
    private var autoStopRunnable: Runnable? = null

    fun isAlertActive(): Boolean = isAlertActive

    /**
     * Begins the full phone-finding alert using saved preferences.
     * Safe to call from background service or detector thread.
     */
    @Synchronized
    fun triggerFindPhone(flashlightEnabledOverride: Boolean? = null) {
        if (isAlertActive) {
            Log.d(tag, "Alert already active — ignoring duplicate trigger")
            return
        }

        val prefs = UserPreferencesDataSource(context)
        val soundEnabled = prefs.isSoundAlertEnabled
        val flashEnabled = flashlightEnabledOverride ?: prefs.isFlashlightEnabled
        val vibrationEnabled = prefs.isVibrationEnabled
        val durationSeconds = prefs.selectedAlertDuration

        Log.d(tag, "Find Phone triggered (sound=$soundEnabled, flash=$flashEnabled, vib=$vibrationEnabled, duration=$durationSeconds s)")

        isAlertActive = true
        _isAlertActiveState.value = true

        if (soundEnabled) startAlertSound()
        if (vibrationEnabled) startVibration()
        if (flashEnabled) flashlightManager.startFlashing()

        showAlertNotification()
        launchAlertActivity()

        // Schedule auto-stop timer
        autoStopRunnable?.let { autoStopHandler.removeCallbacks(it) }
        autoStopRunnable = Runnable {
            Log.d(tag, "Auto-stopping alert after $durationSeconds seconds")
            stopFindPhone()
        }
        autoStopHandler.postDelayed(autoStopRunnable!!, durationSeconds * 1000L)
    }

    /** Stops all active alert signals (sound, flashlight, vibration). */
    @Synchronized
    fun stopFindPhone() {
        autoStopRunnable?.let { autoStopHandler.removeCallbacks(it) }
        autoStopRunnable = null

        if (!isAlertActive) return
        Log.d(tag, "Stopping Find Phone alert")
        isAlertActive = false
        _isAlertActiveState.value = false

        stopAlertSound()
        stopVibration()
        flashlightManager.stopFlashing()
        cancelAlertNotification()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Sound
    // ─────────────────────────────────────────────────────────────────────────

    private fun getSoundRawResId(soundId: String): Int? {
        return when (soundId.lowercase()) {
            "airhorn"               -> R.raw.airhorn
            "babylaugh", "baby"     -> R.raw.baby
            "cat"                   -> R.raw.cat
            "dog"                   -> R.raw.dog
            "doorbell", "door_bell" -> R.raw.door_bell
            "train"                 -> R.raw.train
            "hello"                 -> R.raw.hello
            "horn", "car"           -> R.raw.car
            else                    -> null
        }
    }

    private fun startAlertSound() {
        try {
            val prefs = UserPreferencesDataSource(context)
            val selectedSound = prefs.selectedAlertSound
            val volumePercent = prefs.alertSoundVolume
            val rawResId = getSoundRawResId(selectedSound)

            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            val targetStreamVol = (maxVol * (volumePercent / 100.0f)).toInt().coerceAtLeast(1)
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, targetStreamVol, 0)

            mediaPlayer = if (rawResId != null) {
                MediaPlayer.create(context, rawResId)
            } else {
                val ringtoneUri = android.provider.Settings.System.DEFAULT_RINGTONE_URI
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                MediaPlayer().apply {
                    setDataSource(context, ringtoneUri)
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    prepare()
                }
            }

            val volFloat = volumePercent / 100.0f
            mediaPlayer?.apply {
                setVolume(volFloat, volFloat)
                isLooping = true
                start()
            }
            Log.d(tag, "Alert sound started: sound=$selectedSound, volume=$volumePercent%")
        } catch (e: Exception) {
            Log.e(tag, "Failed to start alert sound: ${e.message}")
        }
    }

    private fun stopAlertSound() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
            mediaPlayer = null
            Log.d(tag, "Alert sound stopped")
        } catch (e: Exception) {
            Log.e(tag, "Failed to stop alert sound: ${e.message}")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Vibration
    // ─────────────────────────────────────────────────────────────────────────

    private fun startVibration() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                manager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (!vibrator.hasVibrator()) {
                Log.w(tag, "Device does not have a vibrator")
                return
            }

            val pattern = longArrayOf(0, 500, 500)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, 0)
            }
            Log.d(tag, "Vibration started")
        } catch (e: Exception) {
            Log.e(tag, "Failed to start vibration: ${e.message}")
        }
    }

    private fun stopVibration() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                manager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            vibrator.cancel()
            Log.d(tag, "Vibration stopped")
        } catch (e: Exception) {
            Log.e(tag, "Failed to stop vibration: ${e.message}")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Notification
    // ─────────────────────────────────────────────────────────────────────────

    private fun showAlertNotification() {
        val alertIntent = Intent(context, AlertActivity::class.java).apply {
            action = Constants.ACTION_STOP_ALERT
            flags  = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val fullScreenPi = PendingIntent.getActivity(
            context, 0, alertIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopPi = PendingIntent.getActivity(
            context, 1, alertIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, Constants.CHANNEL_ID_ALERT)
            .setSmallIcon(R.drawable.ic_notification_phone)
            .setContentTitle(context.getString(R.string.alert_notification_title))
            .setContentText(context.getString(R.string.alert_notification_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPi, true)
            .setContentIntent(fullScreenPi)
            .addAction(
                R.drawable.ic_stop,
                context.getString(R.string.stop_alert),
                stopPi
            )
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(Constants.NOTIFICATION_ID_ALERT, notification)
    }

    private fun cancelAlertNotification() {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(Constants.NOTIFICATION_ID_ALERT)
    }

    private fun launchAlertActivity() {
        try {
            val intent = Intent(context, AlertActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(tag, "Failed to launch AlertActivity: ${e.message}")
        }
    }
}
