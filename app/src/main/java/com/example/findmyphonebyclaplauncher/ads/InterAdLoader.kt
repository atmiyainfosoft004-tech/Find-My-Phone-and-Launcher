package com.example.findmyphonebyclaplauncher.ads

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import com.example.findmyphonebyclaplauncher.App
import com.example.findmyphonebyclaplauncher.ads.config.AdsConfigManager
import com.example.findmyphonebyclaplauncher.analytics.AnalyticsHelper.logAds
import com.example.findmyphonebyclaplauncher.util.NetworkUtil
import com.example.findmyphonebyclaplauncher.util.SystemUiHelper
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

class InterAdLoader {
    private val ads get() = AdsConfigManager.config
    var isInterstitialLoading: Boolean = false
    var isInterstitialShowing: Boolean = false
    private var interstitialAd: InterstitialAd? = null

    val isInterstitialReady: Boolean
        get() = interstitialAd != null

    private val pendingLoadCallbacks = mutableListOf<() -> Unit>()

    init {
        instance = this
    }

    fun loadInterstitialAds(activity: Activity) {
        if (!NetworkUtil.isNetworkAvailable(activity)) {
            Log.d("InterstitialAd", "loadInterstitialAds: Offline mode. Suppressing preload.")
            return
        }

        if (ads.canShowInter) {
            App.runWhenMobileAdsReady {
                if (!activity.isFinishing && !activity.isDestroyed && NetworkUtil.isNetworkAvailable(activity)) {
                    loadInterstitialAd(activity)
                }
            }
        }
    }

    fun loadInterstitialAd(
        activity: Activity,
        onComplete: (() -> Unit)? = null
    ) {
        if (!NetworkUtil.isNetworkAvailable(activity)) {
            Log.d("InterstitialAd", "loadInterstitialAd: Offline mode. Aborting request.")
            onComplete?.invoke()
            return
        }

        // Strict Guard: Never make an ad load network request if Interstitial is disabled or ID is missing
        if (!ads.canShowInter) {
            Log.d("InterstitialAd", "loadInterstitialAd: Aborted. is_inter_ad_enabled is false or ID is empty")
            onComplete?.invoke()
            return
        }

        if (interstitialAd != null) {
            Log.d("InterstitialAd", "loadInterstitialAd: Ad already cached and ready")
            onComplete?.invoke()
            return
        }
        if (onComplete != null) pendingLoadCallbacks.add(onComplete)
        if (isInterstitialLoading) {
            Log.d("InterstitialAd", "loadInterstitialAd: Ad is currently loading, callback attached")
            return
        }
        isInterstitialLoading = true
        val adRequest = AdRequest.Builder().build()
        logAds(activity, "inter_req")
        Log.d("InterstitialAd", "loadInterstitialAd: Requesting ad with ID '${ads.interAdId}'")
        InterstitialAd.load(
            activity.applicationContext,
            ads.interAdId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    this@InterAdLoader.interstitialAd = interstitialAd
                    isInterstitialLoading = false
                    logAds(activity, "inter_loaded")
                    Log.d("InterstitialAd", "loadInterstitialAd: SUCCESS -> Ad loaded")
                    resetFailedCountInterstitial()
                    flushLoadCallbacks()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    increaseFailedCountInterstitial()
                    logAds(activity, "inter_failed_to_load_" + loadAdError.code)
                    Log.e("InterstitialAd", "loadInterstitialAd: FAILED -> ${loadAdError.message} (code ${loadAdError.code})")
                    this@InterAdLoader.interstitialAd = null
                    isInterstitialLoading = false
                    flushLoadCallbacks()
                }
            })
    }

    private fun flushLoadCallbacks() {
        val callbacks = pendingLoadCallbacks.toList()
        pendingLoadCallbacks.clear()
        callbacks.forEach { it.invoke() }
    }

    fun showOrLoadInterstitial(
        activity: Activity,
        isFromBack: Boolean,
        listener: FullScreenDismissListener
    ) {
        if (!NetworkUtil.isNetworkAvailable(activity) || !ads.canShowInter || activity.isFinishing || activity.isDestroyed) {
            Log.d("InterstitialAd", "showOrLoadInterstitial: Offline mode, activity finishing/destroyed, or canShowInter is false")
            listener.onDismiss()
            return
        }

        App.runWhenMobileAdsReady {
            if (activity.isFinishing || activity.isDestroyed || !ads.canShowInter || !NetworkUtil.isNetworkAvailable(activity)) {
                Log.d("InterstitialAd", "showOrLoadInterstitial: Activity finishing/destroyed or canShowInter/network is false")
                listener.onDismiss()
                return@runWhenMobileAdsReady
            }
            if (ads.preloadAdInterstitial && interstitialAd != null) {
                Log.d("InterstitialAd", "showOrLoadInterstitial: Preloaded ad ready, presenting immediately without loading dialog")
                presentInterstitial(activity, isFromBack, listener)
                return@runWhenMobileAdsReady
            }

            Log.d("InterstitialAd", "showOrLoadInterstitial: On-demand loading required (preloadAdInterstitial=${ads.preloadAdInterstitial}) -> showing loading dialog")
            val loadingDialog = AdLoadingDialog(activity)
            var actionExecuted = false

            fun safeDismissAndContinue() {
                if (actionExecuted) return
                actionExecuted = true
                loadingDialog.dismiss()
                SystemUiHelper.applyStickyImmersiveMode(activity)
                listener.onDismiss()
            }

            loadingDialog.show(timeoutMs = 2500L) {
                Log.d("InterstitialAd", "showOrLoadInterstitial: Loading dialog TIMEOUT (2.5s) -> proceeding")
                safeDismissAndContinue()
            }

            loadInterstitialAd(activity) {
                if (actionExecuted) return@loadInterstitialAd
                if (activity.isFinishing || activity.isDestroyed || !ads.canShowInter) {
                    safeDismissAndContinue()
                    return@loadInterstitialAd
                }
                if (interstitialAd != null) {
                    loadingDialog.dismiss()
                    presentInterstitial(activity, isFromBack, listener)
                } else {
                    Log.d("InterstitialAd", "showOrLoadInterstitial: Ad load failed -> proceeding")
                    safeDismissAndContinue()
                }
            }
        }
    }

    private fun presentInterstitial(
        activity: Activity,
        isFromBack: Boolean,
        listener: FullScreenDismissListener
    ) {
        if (!ads.canShowInter) {
            Log.d("InterstitialAd", "presentInterstitial: skipped, canShowInter is false")
            listener.onDismiss()
            return
        }
        val ad = interstitialAd ?: run {
            Log.d("InterstitialAd", "presentInterstitial: interstitialAd is null")
            listener.onDismiss()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d("InterstitialAd", "onAdDismissedFullScreenContent -> resetting state")
                interstitialAd = null
                isInterstitialLoading = false
                isInterstitialShowing = false
                SystemUiHelper.applyStickyImmersiveMode(activity)
                if (ads.canShowInter && ads.preloadAdInterstitial && NetworkUtil.isNetworkAvailable(activity)) {
                    loadInterstitialAd(activity)
                }
                logAds(activity, "inter_dismissed")
                listener.onDismiss()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e("InterstitialAd", "onAdFailedToShowFullScreenContent: ${adError.message} (code ${adError.code})")
                interstitialAd = null
                isInterstitialLoading = false
                isInterstitialShowing = false
                SystemUiHelper.applyStickyImmersiveMode(activity)
                logAds(activity, "inter_failed_to_show_" + adError.code)
                if (ads.canShowInter && ads.preloadAdInterstitial && NetworkUtil.isNetworkAvailable(activity)) {
                    loadInterstitialAd(activity)
                }
                listener.onDismiss()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d("InterstitialAd", "onAdShowedFullScreenContent: Ad is visible on screen")
                isInterstitialShowing = true
                logAds(activity, "inter_showed")
            }

            override fun onAdClicked() {
                super.onAdClicked()
                Log.d("InterstitialAd", "interstitial ad clicked")
                logAds(activity, "inter_clicked")
            }
        }
        ad.show(activity)
    }

    fun showAppClickInterstitial(activity: Activity, listener: FullScreenDismissListener) {
        if (!canShowAppClickInter() || !NetworkUtil.isNetworkAvailable(activity)) {
            Log.d("InterstitialAd", "showAppClickInterstitial: skipped, canShowAppClickInter or network is false")
            listener.onDismiss()
            return
        }
        showOrLoadInterstitial(activity, isFromBack = false, listener)
    }

    fun showSwipeInterstitial(activity: Activity, listener: FullScreenDismissListener) {
        if (!canShowSwipeInter() || !NetworkUtil.isNetworkAvailable(activity)) {
            Log.d("InterstitialAd", "showSwipeInterstitial: skipped, canShowSwipeInter or network is false")
            listener.onDismiss()
            return
        }
        showOrLoadInterstitial(activity, isFromBack = false, listener)
    }

    fun showBackInterstitial(activity: Activity, listener: FullScreenDismissListener) {
        if (!canShowBackAd() || !NetworkUtil.isNetworkAvailable(activity)) {
            Log.d("InterstitialAd", "showBackInterstitial: skipped, canShowBackAd or network is false")
            listener.onDismiss()
            return
        }
        showOrLoadInterstitial(activity, isFromBack = true, listener)
    }

    fun showInterstitialAd(
        activity: Activity,
        isFromBack: Boolean,
        listener: FullScreenDismissListener
    ) {
        showOrLoadInterstitial(activity, isFromBack, listener)
    }

    fun showInterstitialImmediate(
        activity: Activity,
        listener: FullScreenDismissListener
    ) {
        showOrLoadInterstitial(activity, isFromBack = false, listener)
    }

    fun canShowAppClickInter(): Boolean = ads.canShowAppClickInter
    fun canShowSwipeInter(): Boolean = ads.canShowSwipeInter
    fun canShowBackAd(): Boolean = ads.canShowBackAd

    fun interface FullScreenDismissListener {
        fun onDismiss()
    }

    companion object {
        var instance: InterAdLoader? = null
            get() {
                if (field == null) {
                    field = InterAdLoader()
                }
                return field
            }
            private set

        private const val KEY_INTERSTITIAL_FORWARD_COUNT = "KeyInterstitialForwardCount"
        private const val KEY_INTERSTITIAL_BACKWARD_COUNT = "KeyInterstitialBackwardCount"
        private const val KEY_CLICK_COUNT = "KeyClickCount"
        private const val KEY_FAILED_COUNT_INTERSTITIAL = "KeyFailedCountInterstitial"

        fun resetInterstitialForwardCount() {
            Log.d("AdCounter", "Resetting forward/swipe counter to 0")
            preference.edit().putInt(KEY_INTERSTITIAL_FORWARD_COUNT, 0).apply()
        }

        fun resetInterstitialBackwardCount() {
            preference.edit().putInt(KEY_INTERSTITIAL_BACKWARD_COUNT, 0).apply()
            resetInAppBackCount()
        }

        fun resetCounter() {
            Log.d("AdCounter", "Resetting all ad counters to 0")
            resetInterstitialForwardCount()
            resetInAppBackCount()
            resetLauncherClickCount()
            resetFailedCountInterstitial()
        }

        private const val KEY_LAUNCHER_CLICK_COUNT = "KeyLauncherClickCount"
        private const val KEY_IN_APP_BACK_COUNT = "KeyInAppBackCount"

        fun increaseInterstitialForwardCount(): Int {
            val next = interstitialForwardCount + 1
            preference.edit().putInt(KEY_INTERSTITIAL_FORWARD_COUNT, next).apply()
            Log.d("AdCounter", "Forward/swipe count incremented: $next")
            return next
        }

        fun increaseInterstitialBackwardCount(): Int {
            return increaseInAppBackCount()
        }

        fun increaseInAppBackCount(): Int {
            val next = inAppBackCount + 1
            preference.edit().putInt(KEY_IN_APP_BACK_COUNT, next).apply()
            Log.d("AdCounter", "In-app back press count incremented: $next")
            return next
        }

        fun resetInAppBackCount() {
            Log.d("AdCounter", "Resetting in-app back press counter to 0")
            preference.edit().putInt(KEY_IN_APP_BACK_COUNT, 0).apply()
        }

        fun increaseLauncherClickCount(): Int {
            val next = launcherClickCount + 1
            preference.edit().putInt(KEY_LAUNCHER_CLICK_COUNT, next).apply()
            Log.d("AdCounter", "Launcher click count incremented: $next")
            return next
        }

        fun resetLauncherClickCount() {
            Log.d("AdCounter", "Resetting launcher click counter to 0")
            preference.edit().putInt(KEY_LAUNCHER_CLICK_COUNT, 0).apply()
        }

        fun increaseClickCount(): Int = increaseLauncherClickCount()
        fun resetClickCount() = resetLauncherClickCount()

        fun resetForwardCount() = resetInterstitialForwardCount()
        fun resetBackwardCount() = resetInAppBackCount()
        fun increaseForwardCount() = increaseInterstitialForwardCount()
        fun increaseBackwardCount() = increaseInAppBackCount()

        val interstitialForwardCount: Int
            get() = preference.getInt(KEY_INTERSTITIAL_FORWARD_COUNT, 0)

        val inAppBackCount: Int
            get() = preference.getInt(KEY_IN_APP_BACK_COUNT, 0)

        val interstitialBackwardCount: Int
            get() = inAppBackCount

        val launcherClickCount: Int
            get() = preference.getInt(KEY_LAUNCHER_CLICK_COUNT, 0)

        val clickCount: Int
            get() = launcherClickCount

        private val preference: SharedPreferences
            get() = App.getInstance().getSharedPreferences(
                App.getInstance().packageName, Context.MODE_PRIVATE
            )

        fun resetFailedCountInterstitial() {
            preference.edit().putInt(KEY_FAILED_COUNT_INTERSTITIAL, 0).apply()
        }

        private fun increaseFailedCountInterstitial() {
            preference.edit().putInt(KEY_FAILED_COUNT_INTERSTITIAL, failedCountInterstitial + 1).apply()
        }

        private val failedCountInterstitial: Int
            get() = preference.getInt(KEY_FAILED_COUNT_INTERSTITIAL, 0)

        fun startActivityWithAd(activity: Activity, intent: Intent?) {
            instance!!.showInterstitialAd(activity, false, FullScreenDismissListener {
                activity.startActivity(intent)
            })
        }

        fun startActivityWithAdLauncher(
            activity: Activity,
            intent: Intent?,
            activityResultLauncher: ActivityResultLauncher<Intent>
        ) {
            instance!!.showInterstitialAd(activity, false) {
                activityResultLauncher.launch(intent!!)
            }
        }

        fun finishWithAd(activity: Activity) {
            instance!!.showInterstitialAd(
                activity, true, FullScreenDismissListener { activity.finish() })
        }

        fun finishWithResultAfterAd(
            activity: Activity, resultKey: String, resultValue: String
        ) {
            instance?.showInterstitialAd(activity, true, FullScreenDismissListener {
                val resultIntent = Intent().apply {
                    putExtra(resultKey, resultValue)
                }
                activity.setResult(Activity.RESULT_OK, resultIntent)
                activity.finish()
            })
        }
    }
}
