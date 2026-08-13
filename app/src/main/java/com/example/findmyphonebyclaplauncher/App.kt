package com.example.findmyphonebyclaplauncher

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.util.Log
import com.example.findmyphonebyclaplauncher.domain.manager.FindPhoneManager
import com.example.findmyphonebyclaplauncher.utils.Constants

class App : Application() {

    /**
     * Application-level FindPhoneManager singleton so both SoundDetectionService and
     * AlertActivity can share the same alert state without IPC.
     */
    val findPhoneManager: FindPhoneManager by lazy { FindPhoneManager(this) }

    override fun onCreate() {
        super.onCreate()
        Log.d(Constants.TAG, "Application created")
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val notificationManager = getSystemService(NotificationManager::class.java)

        // Low-importance foreground service channel
        val detectionChannel = NotificationChannel(
            Constants.CHANNEL_ID_DETECTION,
            getString(R.string.channel_detection_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.channel_detection_description)
        }

        // High-importance alert channel
        val alertChannel = NotificationChannel(
            Constants.CHANNEL_ID_ALERT,
            getString(R.string.channel_alert_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.channel_alert_description)
        }

        notificationManager.createNotificationChannel(detectionChannel)
        notificationManager.createNotificationChannel(alertChannel)
        Log.d(Constants.TAG, "Notification channels created")
    }
}
