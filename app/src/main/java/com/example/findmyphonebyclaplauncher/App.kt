package com.example.findmyphonebyclaplauncher

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.findmyphonebyclaplauncher.ads.config.AdsConfigManager
import com.example.findmyphonebyclaplauncher.analytics.AnalyticsHelper
import com.example.findmyphonebyclaplauncher.domain.manager.FindPhoneManager
import com.example.findmyphonebyclaplauncher.utils.Constants
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.initialization.InitializationStatus

class App : Application() {

    /**
     * Application-level FindPhoneManager singleton so both SoundDetectionService and
     * AlertActivity can share the same alert state without IPC.
     */
    val findPhoneManager: FindPhoneManager by lazy { FindPhoneManager(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.d(Constants.TAG, "Application created")
        com.example.findmyphonebyclaplauncher.data.repository.AppRepository.init(this)
        com.example.findmyphonebyclaplauncher.config.FirebaseConfigManager.initialize(this)
        AnalyticsHelper.init(this)
        createNotificationChannels()

        AdsConfigManager.initialize(this)
        initMobileAds()
    }

    private fun initMobileAds() {
        val testDeviceIds = listOf(
            "A60DCA743B4F66FE88947C10E6F2A6FE",
            "354F0D1BAAB2126CB5257CCE81BAB494",
            "5FE801EA0777271580034DB208EB7B64",
            "C1146CEFBF6A749CE2A7AF4B6C972348"
        )
        val requestConfiguration = RequestConfiguration.Builder()
            .setTestDeviceIds(testDeviceIds)
            .build()

        MobileAds.setRequestConfiguration(requestConfiguration)
        MobileAds.initialize(this) { status: InitializationStatus? ->
            status?.adapterStatusMap?.forEach { (adapter, adapterStatus) ->
                Log.d(
                    "MobileAdsInit",
                    "adapter=$adapter state=${adapterStatus.initializationState} desc=${adapterStatus.description}"
                )
            }
            notifyMobileAdsReady()
        }
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

    companion object {
        private lateinit var instance: App
        fun getInstance(): App = instance

        @Volatile
        var isMobileAdsInitialized: Boolean = false
            private set

        private val mainHandler = Handler(Looper.getMainLooper())
        private val pendingMobileAdsReady = mutableListOf<() -> Unit>()

        fun runWhenMobileAdsReady(action: () -> Unit) {
            if (isMobileAdsInitialized) {
                if (Looper.myLooper() == Looper.getMainLooper()) action()
                else mainHandler.post(action)
            } else {
                synchronized(pendingMobileAdsReady) {
                    if (isMobileAdsInitialized) {
                        mainHandler.post(action)
                    } else {
                        pendingMobileAdsReady.add(action)
                    }
                }
            }
        }

        fun notifyMobileAdsReadyIfNeeded() {
            if (!isMobileAdsInitialized) {
                notifyMobileAdsReady()
            }
        }

        private fun notifyMobileAdsReady() {
            isMobileAdsInitialized = true
            val pending = synchronized(pendingMobileAdsReady) {
                pendingMobileAdsReady.toList().also { pendingMobileAdsReady.clear() }
            }
            pending.forEach { action ->
                if (Looper.myLooper() == Looper.getMainLooper()) action()
                else mainHandler.post(action)
            }
        }
    }
}
