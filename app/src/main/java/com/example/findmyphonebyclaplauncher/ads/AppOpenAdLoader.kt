package com.example.findmyphonebyclaplauncher.ads

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.findmyphonebyclaplauncher.App
import com.example.findmyphonebyclaplauncher.ads.config.AdsConfigManager
import com.example.findmyphonebyclaplauncher.ads.listeners.ContactAppOpenAdsListener
import com.example.findmyphonebyclaplauncher.analytics.AnalyticsHelper
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.appopen.AppOpenAd.AppOpenAdLoadCallback
import java.util.Date

class AppOpenAdLoader(val app: App) :
    LifecycleObserver, ActivityLifecycleCallbacks, DefaultLifecycleObserver {

    var appOpenAd: AppOpenAd? = null
    private var appOpenAdLoadCallback: AppOpenAdLoadCallback? = null
    var currentActivity: Activity? = null
    var loadTimeLong: Long = 0
    private val TAG = "AppOpenAdsStatus"

    init {
        instance = this
        app.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    fun preloadAppOpenAd(activity: Activity? = null) {
        if (!AdsConfigManager.config.canShowAppOpen) return
        if (AdsConfigManager.config.appOpenAdPreload) {
            val act = activity ?: currentActivity
            if (isAvailableAppOpenAd) {
                return
            }

            appOpenAdLoadCallback = object : AppOpenAdLoadCallback() {
                override fun onAdLoaded(appOpenAd: AppOpenAd) {
                    act?.let { AnalyticsHelper.logAds(it, "appopen_loaded") }
                    this@AppOpenAdLoader.appOpenAd = appOpenAd
                    this@AppOpenAdLoader.loadTimeLong = Date().time
                    Log.d(TAG, "preload->onAdLoaded")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    act?.let {
                        AnalyticsHelper.logAds(
                            it,
                            "appopen_failed_" + loadAdError.code
                        )
                    }
                    Log.e(TAG, "preload->onAdFailedToLoad: ${loadAdError.message}")
                }
            }

            act?.let { AnalyticsHelper.logAds(it, "appopen_request") }
            val requestAds = AdRequest.Builder().build()
            AppOpenAd.load(
                app,
                AdsConfigManager.config.appOpenAdId,
                requestAds,
                appOpenAdLoadCallback as AppOpenAdLoadCallback
            )
        }
    }

    val isAvailableAppOpenAd: Boolean
        get() = appOpenAd != null && (Date().time - loadTimeLong < 4 * 3600 * 1000)

    fun showAppOpenAds(
        activity: Activity,
        onShowAdCompleteListener: ContactAppOpenAdsListener
    ) {
        showAppOpenAd(activity) {
            onShowAdCompleteListener.contactAppOpenAdsShow()
        }
    }

    fun showAppOpenAd(
        activity: Activity,
        onDone: () -> Unit
    ) {
        currentActivity = activity

        if (!AdsConfigManager.config.canShowAppOpen) {
            onDone()
            return
        }

        if (!appOpenShowingBoolean && isAvailableAppOpenAd) {
            Log.d(TAG, "showAppOpenAd -> display cached ad")
            val fullScreenContentCallback: FullScreenContentCallback =
                object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        AnalyticsHelper.logAds(activity, "appopen_dismissed")
                        this@AppOpenAdLoader.appOpenAd = null
                        appOpenShowingBoolean = false
                        com.example.findmyphonebyclaplauncher.util.SystemUiHelper.applyStickyImmersiveMode(activity)
                        preloadAppOpenAd(activity)
                        onDone()
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        AnalyticsHelper.logAds(
                            activity,
                            "appopen_failed_" + adError.code
                        )
                        this@AppOpenAdLoader.appOpenAd = null
                        appOpenShowingBoolean = false
                        com.example.findmyphonebyclaplauncher.util.SystemUiHelper.applyStickyImmersiveMode(activity)
                        onDone()
                    }

                    override fun onAdShowedFullScreenContent() {
                        AnalyticsHelper.logAds(activity, "appopen_showed")
                        appOpenShowingBoolean = true
                    }

                    override fun onAdClicked() {
                        super.onAdClicked()
                        AnalyticsHelper.logAds(activity, "appopen_clicked")
                    }
                }
            appOpenAd!!.fullScreenContentCallback = fullScreenContentCallback
            appOpenAd!!.show(activity)
        } else {
            Log.d(TAG, "showAppOpenAd -> load on demand -> display")
            val loadingDialog = AdLoadingDialog(activity)
            var actionExecuted = false

            fun safeDismissAndContinue() {
                if (actionExecuted) return
                actionExecuted = true
                loadingDialog.dismiss()
                onDone()
            }

            loadingDialog.show(timeoutMs = 2500L) {
                Log.d(TAG, "showAppOpenAd: Loading dialog TIMEOUT -> proceeding")
                safeDismissAndContinue()
            }

            val requestAds = AdRequest.Builder().build()
            AnalyticsHelper.logAds(activity, "appopen_request")
            AppOpenAd.load(
                app,
                AdsConfigManager.config.appOpenAdId,
                requestAds,
                object : AppOpenAdLoadCallback() {
                    override fun onAdLoaded(appOpenAd: AppOpenAd) {
                        AnalyticsHelper.logAds(activity, "appopen_loaded")
                        this@AppOpenAdLoader.appOpenAd = appOpenAd
                        this@AppOpenAdLoader.loadTimeLong = Date().time

                        if (actionExecuted || activity.isFinishing || activity.isDestroyed) {
                            loadingDialog.dismiss()
                            return
                        }
                        loadingDialog.dismiss()

                        val fullScreenContentCallback = object : FullScreenContentCallback() {
                            override fun onAdDismissedFullScreenContent() {
                                AnalyticsHelper.logAds(activity, "appopen_dismissed")
                                this@AppOpenAdLoader.appOpenAd = null
                                appOpenShowingBoolean = false
                                com.example.findmyphonebyclaplauncher.util.SystemUiHelper.applyStickyImmersiveMode(activity)
                                preloadAppOpenAd(activity)
                                safeDismissAndContinue()
                            }

                            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                AnalyticsHelper.logAds(activity, "appopen_failed_" + adError.code)
                                this@AppOpenAdLoader.appOpenAd = null
                                appOpenShowingBoolean = false
                                com.example.findmyphonebyclaplauncher.util.SystemUiHelper.applyStickyImmersiveMode(activity)
                                safeDismissAndContinue()
                            }

                            override fun onAdShowedFullScreenContent() {
                                AnalyticsHelper.logAds(activity, "appopen_showed")
                                appOpenShowingBoolean = true
                            }

                            override fun onAdClicked() {
                                super.onAdClicked()
                                AnalyticsHelper.logAds(activity, "appopen_clicked")
                            }
                        }
                        appOpenAd.fullScreenContentCallback = fullScreenContentCallback
                        appOpenAd.show(activity)
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        AnalyticsHelper.logAds(activity, "appopen_failed_" + loadAdError.code)
                        safeDismissAndContinue()
                    }
                }
            )
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, savedInstanceState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity == activity) {
            currentActivity = null
        }
    }

    override fun onStart(owner: LifecycleOwner) {}

    companion object {
        var appOpenShowingBoolean: Boolean = false
        var instance: AppOpenAdLoader? = null
            private set

        private const val KEY_FAILED_COUNT_APP_OPEN = "KeyFailedCountAppOpen"

        fun resetCounter() {
            resetFailedCountAppOpen()
        }

        private val preference: SharedPreferences
            get() = App.getInstance().getSharedPreferences(
                App.getInstance().packageName, Context.MODE_PRIVATE
            )

        fun resetFailedCountAppOpen() {
            val preferences: SharedPreferences = preference
            preferences.edit().putInt(KEY_FAILED_COUNT_APP_OPEN, 0).apply()
        }

        fun increaseFailedCountAppOpen() {
            val preferences: SharedPreferences = preference
            preferences.edit().putInt(KEY_FAILED_COUNT_APP_OPEN, failedCountAppOpen + 1).apply()
        }

        val failedCountAppOpen: Int
            get() {
                val preferences: SharedPreferences = preference
                return preferences.getInt(KEY_FAILED_COUNT_APP_OPEN, 0)
            }
    }
}
