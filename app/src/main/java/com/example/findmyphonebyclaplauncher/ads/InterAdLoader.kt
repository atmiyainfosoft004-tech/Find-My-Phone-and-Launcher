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
        if (ads.canShowInter) {
            App.runWhenMobileAdsReady {
                if (!activity.isFinishing && !activity.isDestroyed) {
                    loadInterstitialAd(activity)
                }
            }
        }
    }

    fun loadInterstitialAd(
        activity: Activity,
        onComplete: (() -> Unit)? = null
    ) {
        // Strict Guard: Never make an ad load network request if Interstitial is disabled or ID is missing
        if (!ads.canShowInter) {
            Log.d("InterstitialDebug", "loadInterstitialAd: Aborted. is_inter_ad_enabled is false or ID is empty")
            onComplete?.invoke()
            return
        }

        if (interstitialAd != null) {
            Log.d("InterstitialDebug", "loadInterstitialAd: Ad already cached and ready")
            onComplete?.invoke()
            return
        }
        if (onComplete != null) pendingLoadCallbacks.add(onComplete)
        if (isInterstitialLoading) {
            Log.d("InterstitialDebug", "loadInterstitialAd: Ad is currently loading, callback attached")
            return
        }
        isInterstitialLoading = true
        val adRequest = AdRequest.Builder().build()
        logAds(activity, "inter_req")
        Log.d("InterstitialDebug", "loadInterstitialAd: Requesting ad with ID '${ads.interAdId}'")
        InterstitialAd.load(
            activity.applicationContext,
            ads.interAdId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    this@InterAdLoader.interstitialAd = interstitialAd
                    isInterstitialLoading = false
                    logAds(activity, "inter_loaded")
                    Log.d("InterstitialDebug", "loadInterstitialAd: SUCCESS -> Ad loaded")
                    resetFailedCountInterstitial()
                    flushLoadCallbacks()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    increaseFailedCountInterstitial()
                    logAds(activity, "inter_failed_to_load_" + loadAdError.code)
                    Log.e("InterstitialDebug", "loadInterstitialAd: FAILED -> ${loadAdError.message} (code ${loadAdError.code})")
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

    fun showInterstitialDirect(
        activity: Activity,
        onDone: () -> Unit
    ) {
        showOrLoadInterstitial(activity, isFromBack = false) {
            onDone()
        }
    }

    fun showOrLoadInterstitial(
        activity: Activity,
        isFromBack: Boolean,
        listener: FullScreenDismissListener
    ) {
        App.runWhenMobileAdsReady {
            if (activity.isFinishing || activity.isDestroyed || !ads.canShowInter) {
                Log.d("InterstitialDebug", "showOrLoadInterstitial: Activity finishing/destroyed or canShowInter is false")
                listener.onDismiss()
                return@runWhenMobileAdsReady
            }
            if (interstitialAd != null) {
                Log.d("InterstitialDebug", "showOrLoadInterstitial: Ad ready, presenting now")
                presentInterstitial(activity, isFromBack, listener)
                return@runWhenMobileAdsReady
            }

            Log.d("InterstitialDebug", "showOrLoadInterstitial: Ad not ready -> showing loading overlay")
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
                Log.d("InterstitialDebug", "showOrLoadInterstitial: Loading dialog TIMEOUT (2.5s) -> proceeding")
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
                    Log.d("InterstitialDebug", "showOrLoadInterstitial: Ad load failed -> proceeding")
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
            Log.d("InterstitialDebug", "presentInterstitial: skipped, canShowInter is false")
            listener.onDismiss()
            return
        }
        val ad = interstitialAd ?: run {
            Log.d("InterstitialDebug", "presentInterstitial: interstitialAd is null")
            listener.onDismiss()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d("InterstitialDebug", "onAdDismissedFullScreenContent -> resetting state and reloading")
                interstitialAd = null
                isInterstitialLoading = false
                isInterstitialShowing = false
                SystemUiHelper.applyStickyImmersiveMode(activity)
                if (ads.canShowInter) {
                    loadInterstitialAd(activity)
                }
                logAds(activity, "inter_dismissed")
                listener.onDismiss()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e("InterstitialDebug", "onAdFailedToShowFullScreenContent: ${adError.message} (code ${adError.code})")
                interstitialAd = null
                isInterstitialLoading = false
                isInterstitialShowing = false
                SystemUiHelper.applyStickyImmersiveMode(activity)
                logAds(activity, "inter_failed_to_show_" + adError.code)
                if (ads.canShowInter) {
                    loadInterstitialAd(activity)
                }
                listener.onDismiss()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d("InterstitialDebug", "onAdShowedFullScreenContent: Ad displayed on screen")
                interstitialAd = null
                isInterstitialShowing = true
                logAds(activity, "inter_showed")
            }

            override fun onAdClicked() {
                super.onAdClicked()
                logAds(activity, "inter_clicked")
            }
        }
        ad.show(activity)
    }

    fun showAppClickInterstitial(activity: Activity, listener: FullScreenDismissListener) {
        if (!canShowAppClickInter()) {
            Log.d("InterstitialDebug", "showAppClickInterstitial: skipped, canShowAppClickInter is false")
            listener.onDismiss()
            return
        }
        showInterstitialAd(activity, false, listener)
    }

    fun showSwipeInterstitial(activity: Activity, listener: FullScreenDismissListener) {
        if (!canShowSwipeInter()) {
            Log.d("InterstitialDebug", "showSwipeInterstitial: skipped, canShowSwipeInter is false")
            listener.onDismiss()
            return
        }
        showInterstitialAd(activity, false, listener)
    }

    private fun canShowAppClickInter(): Boolean = ads.canShowAppClickInter

    private fun canShowSwipeInter(): Boolean = ads.canShowSwipeInter

    fun showInterstitialAd(
        activity: Activity,
        isFromBack: Boolean,
        listener: FullScreenDismissListener
    ) {
        if (ads.canShowInter) {
            val currentInterval: Int =
                if (isFromBack) interstitialBackwardCount else interstitialForwardCount
            val regularInterval =
                if (isFromBack) ads.interBackCount.coerceAtLeast(1)
                else ads.interCount.coerceAtLeast(1)

            Log.d(
                "InterstitialDebug",
                "showInterstitialAd: isFromBack=$isFromBack, currentInterval=$currentInterval, regularInterval=$regularInterval, isAdReady=${interstitialAd != null}"
            )

            if (currentInterval >= regularInterval) {
                if (isFromBack) {
                    resetInterstitialBackwardCount()
                } else {
                    resetInterstitialForwardCount()
                }
                showOrLoadInterstitial(activity, isFromBack, listener)
            } else {
                if (isFromBack) {
                    increaseInterstitialBackwardCount()
                } else {
                    increaseInterstitialForwardCount()
                }
                Log.d(
                    "InterstitialDebug",
                    "Frequency cap skipped ad: currentInterval=$currentInterval, regularInterval=$regularInterval"
                )
                listener.onDismiss()
            }
        } else {
            Log.d("InterstitialDebug", "showInterstitialAd: skipped, ads.canShowInter is false")
            listener.onDismiss()
        }
    }

    fun showInterstitialImmediate(
        activity: Activity,
        listener: FullScreenDismissListener
    ) {
        if (ads.canShowInter) {
            showOrLoadInterstitial(activity, false, listener)
        } else {
            listener.onDismiss()
        }
    }

    fun interface FullScreenDismissListener {
        fun onDismiss()
    }

    companion object {
        private const val KEY_INTERSTITIAL_BACKWARD_COUNT = "interstitial_backward_count"
        private const val KEY_INTERSTITIAL_FORWARD_COUNT = "interstitial_forward_count"
        private const val KEY_CLICK_COUNT = "app_click_ad_count"
        private const val KEY_FAILED_COUNT_INTERSTITIAL = "KeyFailedCountInterstitial"

        var instance: InterAdLoader? = null
            get() {
                if (field == null) {
                    field = InterAdLoader()
                }
                return field
            }
            private set

        fun resetInterstitialForwardCount() {
            preference.edit().putInt(KEY_INTERSTITIAL_FORWARD_COUNT, 0).apply()
        }

        fun resetInterstitialBackwardCount() {
            preference.edit().putInt(KEY_INTERSTITIAL_BACKWARD_COUNT, 0).apply()
        }

        fun resetClickCount() {
            preference.edit().putInt(KEY_CLICK_COUNT, 0).apply()
        }

        fun resetCounter() {
            resetInterstitialForwardCount()
            resetInterstitialBackwardCount()
            resetClickCount()
            resetFailedCountInterstitial()
        }

        fun increaseInterstitialForwardCount(): Int {
            val next = interstitialForwardCount + 1
            preference.edit().putInt(KEY_INTERSTITIAL_FORWARD_COUNT, next).apply()
            return next
        }

        fun increaseInterstitialBackwardCount(): Int {
            val next = interstitialBackwardCount + 1
            preference.edit().putInt(KEY_INTERSTITIAL_BACKWARD_COUNT, next).apply()
            return next
        }

        fun increaseClickCount(): Int {
            val next = clickCount + 1
            preference.edit().putInt(KEY_CLICK_COUNT, next).apply()
            return next
        }

        fun resetForwardCount() = resetInterstitialForwardCount()
        fun resetBackwardCount() = resetInterstitialBackwardCount()
        fun increaseForwardCount() = increaseInterstitialForwardCount()
        fun increaseBackwardCount() = increaseInterstitialBackwardCount()

        val interstitialForwardCount: Int
            get() = preference.getInt(KEY_INTERSTITIAL_FORWARD_COUNT, 0)

        val interstitialBackwardCount: Int
            get() = preference.getInt(KEY_INTERSTITIAL_BACKWARD_COUNT, 0)

        val clickCount: Int
            get() = preference.getInt(KEY_CLICK_COUNT, 0)

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
