package com.example.findmyphonebyclaplauncher

import android.app.Activity
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.findmyphonebyclaplauncher.ads.AppOpenAdLoader
import com.example.findmyphonebyclaplauncher.ads.config.AdsConfigManager
import com.example.findmyphonebyclaplauncher.analytics.AnalyticsHelper
import com.example.findmyphonebyclaplauncher.domain.manager.FindPhoneManager
import com.example.findmyphonebyclaplauncher.receiver.AppInstallReceiver
import com.example.findmyphonebyclaplauncher.ui.install.AppInstallSuccessActivity
import com.example.findmyphonebyclaplauncher.util.SystemUiHelper
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
    val appOpenAdLoader: AppOpenAdLoader by lazy { AppOpenAdLoader(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.d(Constants.TAG, "Application created")
        com.example.findmyphonebyclaplauncher.data.repository.AppRepository.init(this)
        com.example.findmyphonebyclaplauncher.config.FirebaseConfigManager.initialize(this)
        AnalyticsHelper.init(this)
        createNotificationChannels()

        AdsConfigManager.initialize(this)
        // Initialize AppOpenAdLoader
        appOpenAdLoader.preloadAppOpenAd()

        initMobileAds()
        registerAppInstallReceiver()
        registerPackageInstallerCallback()
        registerGlobalSystemUiController()
    }

    private fun registerGlobalSystemUiController() {
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                activity.window?.decorView?.viewTreeObserver?.addOnWindowFocusChangeListener { hasFocus ->
                    if (hasFocus) {
                        SystemUiHelper.applyStickyImmersiveMode(activity)
                    }
                }
            }

            override fun onActivityStarted(activity: Activity) {
                SystemUiHelper.applyStickyImmersiveMode(activity)
            }

            override fun onActivityResumed(activity: Activity) {
                SystemUiHelper.applyStickyImmersiveMode(activity)
            }

            override fun onActivityPaused(activity: Activity) {}

            override fun onActivityStopped(activity: Activity) {}

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    private fun registerAppInstallReceiver() {
        try {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addAction(Intent.ACTION_PACKAGE_REPLACED)
                addDataScheme("package")
            }
            val receiver = AppInstallReceiver()
            ContextCompat.registerReceiver(
                this,
                receiver,
                filter,
                ContextCompat.RECEIVER_EXPORTED
            )
            Log.d(Constants.TAG, "AppInstallReceiver dynamically registered")
        } catch (e: Exception) {
            Log.e(Constants.TAG, "Failed to register AppInstallReceiver dynamically", e)
        }
    }

    private fun registerPackageInstallerCallback() {
        try {
            val packageInstaller = packageManager.packageInstaller
            packageInstaller.registerSessionCallback(object : PackageInstaller.SessionCallback() {
                override fun onCreated(sessionId: Int) {}
                override fun onBadgingChanged(sessionId: Int) {}
                override fun onActiveChanged(sessionId: Int, active: Boolean) {}
                override fun onProgressChanged(sessionId: Int, progress: Float) {}
                override fun onFinished(sessionId: Int, success: Boolean) {
                    if (success) {
                        try {
                            val sessionInfo = packageManager.packageInstaller.getSessionInfo(sessionId)
                            val appPackageName = sessionInfo?.appPackageName
                            if (!appPackageName.isNullOrBlank() && appPackageName != packageName) {
                                Log.d(Constants.TAG, "PackageInstaller session finished: pkg=$appPackageName")
                                com.example.findmyphonebyclaplauncher.data.repository.AppRepository.get().invalidatePackageCache(appPackageName)
                                AppInstallSuccessActivity.start(
                                    this@App,
                                    appPackageName,
                                    AppInstallSuccessActivity.ACTION_TYPE_INSTALLED
                                )
                            }
                        } catch (e: Exception) {
                            Log.e(Constants.TAG, "Error handling PackageInstaller session finish", e)
                        }
                    }
                }
            })
            Log.d(Constants.TAG, "PackageInstaller SessionCallback registered")
        } catch (e: Exception) {
            Log.e(Constants.TAG, "Failed to register PackageInstaller SessionCallback", e)
        }
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
