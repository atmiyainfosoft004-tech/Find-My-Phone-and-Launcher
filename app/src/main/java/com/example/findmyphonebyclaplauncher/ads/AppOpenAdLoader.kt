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
    var currentContactActivity: Activity? = null
    var contactAppOpenActivity: Activity? = null
    var loadTimeLong: Long = 0
    var TAG = "AppOpenAdsStatus"

    init {
        app.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    fun preloadAppOpenAd(activity: Activity?) {
        if (AdsConfigManager.config.appOpenAdPreload) {
            contactAppOpenActivity = activity
            if (isAvailableAppOpenAd) {
                return
            }

            appOpenAdLoadCallback = object : AppOpenAdLoadCallback() {
                override fun onAdLoaded(appOpenAd: AppOpenAd) {
                    AnalyticsHelper.logAds(contactAppOpenActivity!!, "appopen_loaded")
                    this@AppOpenAdLoader.appOpenAd = appOpenAd
                    this@AppOpenAdLoader.loadTimeLong = Date().time
                    Log.e(TAG, "preload->onAdLoaded")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    AnalyticsHelper.logAds(
                        contactAppOpenActivity!!,
                        "appopen_failed_" + loadAdError.code
                    )
                    Log.e(TAG, "preload->onAdFailedToLoad")
                }
            }

            AnalyticsHelper.logAds(contactAppOpenActivity!!, "appopen_request")
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
        get() = appOpenAd != null

    fun contactAppOpenAdsRequest(activity: Activity?) {
        if (AdsConfigManager.config.canShowAppOpen) {
            if (!appOpenShowingBoolean && isAvailableAppOpenAd) {
                // already loaded
            } else {
                preloadAppOpenAd(activity)
            }
        }
    }

    fun showAppOpenAds(
        activity: Activity,
        onShowAdCompleteListener: ContactAppOpenAdsListener
    ) {
        contactAppOpenActivity = activity

        if (AdsConfigManager.config.canShowAppOpen) {
            if (!appOpenShowingBoolean && isAvailableAppOpenAd) {
                Log.e(TAG, "showAppOpenAds -> display")
                val fullScreenContentCallback: FullScreenContentCallback =
                    object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            AnalyticsHelper.logAds(contactAppOpenActivity!!, "appopen_dismissed")
                            this@AppOpenAdLoader.appOpenAd = null
                            appOpenShowingBoolean = false
                            preloadAppOpenAd(activity)
                            onShowAdCompleteListener.contactAppOpenAdsShow()
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            AnalyticsHelper.logAds(
                                contactAppOpenActivity!!,
                                "appopen_failed_" + adError.code
                            )
                            onShowAdCompleteListener.contactAppOpenAdsShow()
                        }

                        override fun onAdShowedFullScreenContent() {
                            AnalyticsHelper.logAds(contactAppOpenActivity!!, "appopen_showed")
                            appOpenShowingBoolean = true
                        }

                        override fun onAdClicked() {
                            super.onAdClicked()
                            AnalyticsHelper.logAds(contactAppOpenActivity!!, "appopen_clicked")
                        }
                    }
                appOpenAd!!.fullScreenContentCallback = fullScreenContentCallback
                appOpenAd!!.show(activity)
            } else {
                Log.e(TAG, "showAppOpenAds -> load -> display")
                appOpenAdLoadCallback = object : AppOpenAdLoadCallback() {
                    override fun onAdLoaded(appOpenAd: AppOpenAd) {
                        AnalyticsHelper.logAds(contactAppOpenActivity!!, "appopen_loaded")
                        this@AppOpenAdLoader.appOpenAd = appOpenAd
                        this@AppOpenAdLoader.loadTimeLong = Date().time

                        val fullScreenContentCallback: FullScreenContentCallback =
                            object : FullScreenContentCallback() {
                                override fun onAdDismissedFullScreenContent() {
                                    AnalyticsHelper.logAds(
                                        contactAppOpenActivity!!,
                                        "appopen_dismissed"
                                    )
                                    this@AppOpenAdLoader.appOpenAd = null
                                    appOpenShowingBoolean = false
                                    preloadAppOpenAd(activity)
                                    onShowAdCompleteListener.contactAppOpenAdsShow()
                                }

                                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                    AnalyticsHelper.logAds(
                                        contactAppOpenActivity!!,
                                        "appopen_failed_" + adError.code
                                    )
                                    onShowAdCompleteListener.contactAppOpenAdsShow()
                                }

                                override fun onAdShowedFullScreenContent() {
                                    AnalyticsHelper.logAds(
                                        contactAppOpenActivity!!,
                                        "appopen_showed"
                                    )
                                    appOpenShowingBoolean = true
                                }

                                override fun onAdClicked() {
                                    super.onAdClicked()
                                    AnalyticsHelper.logAds(
                                        contactAppOpenActivity!!,
                                        "appopen_clicked"
                                    )
                                }
                            }
                        this@AppOpenAdLoader.appOpenAd!!.fullScreenContentCallback =
                            fullScreenContentCallback
                        this@AppOpenAdLoader.appOpenAd!!.show(activity)
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        AnalyticsHelper.logAds(
                            contactAppOpenActivity!!,
                            "appopen_failed_" + loadAdError.code
                        )
                        onShowAdCompleteListener.contactAppOpenAdsShow()
                    }
                }
                AnalyticsHelper.logAds(contactAppOpenActivity!!, "appopen_request")
                val requestAds = AdRequest.Builder().build()
                AppOpenAd.load(
                    app,
                    AdsConfigManager.config.appOpenAdId,
                    requestAds,
                    appOpenAdLoadCallback as AppOpenAdLoadCallback
                )
            }
        } else {
            onShowAdCompleteListener.contactAppOpenAdsShow()
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
    }

    override fun onActivityStarted(activity: Activity) {
        currentContactActivity = activity
    }

    override fun onActivityResumed(activity: Activity) {
        currentContactActivity = activity
        Log.e("onActivityResumedCalled", activity.localClassName.toString())
    }

    override fun onActivityStopped(activity: Activity) {
    }

    override fun onActivityPaused(activity: Activity) {
    }

    override fun onActivitySaveInstanceState(
        activity: Activity,
        savedInstanceState: Bundle
    ) {
    }

    override fun onActivityDestroyed(activity: Activity) {
        currentContactActivity = null
    }

    override fun onStart(owner: LifecycleOwner) {
    }

    companion object {
        var appOpenShowingBoolean: Boolean = false
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
