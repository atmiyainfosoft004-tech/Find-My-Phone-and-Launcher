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
    var isLoadingAppOpenAd = false
    private var appOpenAdLoadCallback: AppOpenAdLoadCallback? = null
    var currentActivity: Activity? = null
    var loadTimeLong: Long = 0
    private val TAG = "AdManagerDebug"

    init {
        instance = this
        app.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    fun preloadAppOpenAd(context: Context? = null) {
        val config = AdsConfigManager.config
        if (!config.canShowAppOpen) return
        val adId = config.appOpenAdId
        if (adId.isBlank()) return

        if (isLoadingAppOpenAd || isAvailableAppOpenAd) {
            Log.d(TAG, "preloadAppOpenAd: Already loading or cached ad is available (appOpenAd!=null: ${appOpenAd != null})")
            return
        }

        isLoadingAppOpenAd = true
        val ctx = context?.applicationContext ?: app.applicationContext

        val requestAds = AdRequest.Builder().build()
        Log.d(TAG, "preloadAppOpenAd: Loading App Open Ad with ID $adId")
        AppOpenAd.load(
            ctx,
            adId,
            requestAds,
            object : AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    this@AppOpenAdLoader.appOpenAd = ad
                    this@AppOpenAdLoader.isLoadingAppOpenAd = false
                    this@AppOpenAdLoader.loadTimeLong = Date().time
                    Log.d(TAG, "App Open Ad successfully loaded.")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    this@AppOpenAdLoader.appOpenAd = null
                    this@AppOpenAdLoader.isLoadingAppOpenAd = false
                    Log.e(TAG, "App Open Ad failed to load: ${loadAdError.message} (code: ${loadAdError.code})")
                }
            }
        )
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
        if (activity.isFinishing || activity.isDestroyed) {
            onDone()
            return
        }

        currentActivity = activity
        val config = AdsConfigManager.config
        if (!config.canShowAppOpen) {
            Log.d(TAG, "showAppOpenAd: canShowAppOpen is false -> proceeding directly")
            onDone()
            return
        }

        val adToShow = appOpenAd
        if (adToShow != null && isAvailableAppOpenAd) {
            Log.d(TAG, "Presenting cached App Open Ad to user...")
            adToShow.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "App Open Ad dismissed")
                    AnalyticsHelper.logAds(activity, "appopen_dismissed")
                    appOpenAd = null
                    com.example.findmyphonebyclaplauncher.util.SystemUiHelper.applyStickyImmersiveMode(activity)
                    preloadAppOpenAd(activity) // Preload next instance
                    onDone()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    appOpenAd = null
                    Log.e(TAG, "Failed to show App Open Ad: ${adError.message}")
                    AnalyticsHelper.logAds(
                        activity,
                        "appopen_failed_" + adError.code
                    )
                    com.example.findmyphonebyclaplauncher.util.SystemUiHelper.applyStickyImmersiveMode(activity)
                    preloadAppOpenAd(activity)
                    onDone()
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "App Open Ad is currently showing.")
                    AnalyticsHelper.logAds(activity, "appopen_showed")
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    AnalyticsHelper.logAds(activity, "appopen_clicked")
                }
            }
            adToShow.show(activity)
        } else {
            Log.d(TAG, "appOpenAd was not cached -> showing loading dialog and loading App Open Ad on demand...")
            val loadingDialog = AdLoadingDialog(activity)
            var actionExecuted = false

            fun safeDismissAndContinue() {
                if (actionExecuted) return
                actionExecuted = true
                loadingDialog.dismiss()
                com.example.findmyphonebyclaplauncher.util.SystemUiHelper.applyStickyImmersiveMode(activity)
                onDone()
            }

            loadingDialog.show(timeoutMs = 2500L) {
                Log.d(TAG, "App Open loading dialog timeout (2.5s) -> proceeding directly")
                safeDismissAndContinue()
            }

            val adId = config.appOpenAdId
            val requestAds = AdRequest.Builder().build()
            AppOpenAd.load(
                activity.applicationContext,
                adId,
                requestAds,
                object : AppOpenAdLoadCallback() {
                    override fun onAdLoaded(ad: AppOpenAd) {
                        if (actionExecuted || activity.isFinishing || activity.isDestroyed) {
                            appOpenAd = ad
                            loadTimeLong = Date().time
                            return
                        }
                        loadingDialog.dismiss()
                        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                            override fun onAdDismissedFullScreenContent() {
                                Log.d(TAG, "On-demand App Open Ad dismissed")
                                AnalyticsHelper.logAds(activity, "appopen_dismissed")
                                appOpenAd = null
                                com.example.findmyphonebyclaplauncher.util.SystemUiHelper.applyStickyImmersiveMode(activity)
                                preloadAppOpenAd(activity)
                                safeDismissAndContinue()
                            }

                            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                Log.e(TAG, "Failed to show on-demand App Open Ad: ${adError.message}")
                                AnalyticsHelper.logAds(activity, "appopen_failed_" + adError.code)
                                appOpenAd = null
                                com.example.findmyphonebyclaplauncher.util.SystemUiHelper.applyStickyImmersiveMode(activity)
                                preloadAppOpenAd(activity)
                                safeDismissAndContinue()
                            }

                            override fun onAdShowedFullScreenContent() {
                                Log.d(TAG, "On-demand App Open Ad showing.")
                                AnalyticsHelper.logAds(activity, "appopen_showed")
                            }

                            override fun onAdClicked() {
                                super.onAdClicked()
                                AnalyticsHelper.logAds(activity, "appopen_clicked")
                            }
                        }
                        ad.show(activity)
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        Log.e(TAG, "On-demand App Open Ad failed to load: ${loadAdError.message}")
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
