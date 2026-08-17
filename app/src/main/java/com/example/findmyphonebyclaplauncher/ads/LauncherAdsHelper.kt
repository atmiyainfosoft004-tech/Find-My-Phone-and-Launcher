package com.example.findmyphonebyclaplauncher.ads

import android.app.Activity
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import androidx.cardview.widget.CardView
import com.example.findmyphonebyclaplauncher.ads.config.AdsConfigManager
import com.example.findmyphonebyclaplauncher.util.SystemUiHelper
import com.google.android.gms.ads.nativead.NativeAd

object LauncherAdsHelper {

    private const val TAG = "LauncherAdsHelper"

    enum class ActionType {
        CLICK,
        SWIPE,
        BACK
    }

    fun preloadInterstitial(activity: Activity) {
        if (AdsConfigManager.config.preloadAdInterstitial && AdsConfigManager.config.canShowInter) {
            InterAdLoader.instance?.loadInterstitialAds(activity)
        }
    }

    fun preloadAppOpen(activity: Activity) {
        if (AdsConfigManager.config.preloadAdAppOpen && AdsConfigManager.config.canShowAppOpen) {
            AppOpenAdLoader.instance?.preloadAppOpenAd(activity)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Dynamic Action Type Dispatcher (Guarded by Action-Level & Master Switches)
    // ─────────────────────────────────────────────────────────────────────────

    fun showAppClickInterThen(activity: Activity, onContinue: () -> Unit) {
        showAdForAction(activity, ActionType.CLICK, onContinue)
    }

    fun showAppClickAdThen(activity: Activity, onContinue: () -> Unit) {
        showAdForAction(activity, ActionType.CLICK, onContinue)
    }

    fun showSwipeInter(activity: Activity, onDone: () -> Unit = {}) {
        showAdForAction(activity, ActionType.SWIPE, onDone)
    }

    fun showSwipeAd(activity: Activity, onDone: () -> Unit = {}) {
        showAdForAction(activity, ActionType.SWIPE, onDone)
    }

    fun showBackAd(activity: Activity, onDone: () -> Unit) {
        showAdForAction(activity, ActionType.BACK, onDone)
    }

    fun showInterThen(activity: Activity, onContinue: () -> Unit) {
        showAdForAction(activity, ActionType.CLICK, onContinue)
    }

    fun showBlogReturnInter(activity: Activity) {
        if (activity.isFinishing || activity.isDestroyed || !AdsConfigManager.config.canShowInter) return
        InterAdLoader.instance?.showInterstitialImmediate(activity) {}
    }

    /**
     * Dispatches ads strictly based on the triggering action context.
     * Evaluates independent counter registers and dynamically evaluates Boolean flags:
     * - ActionType.CLICK -> isClickAdEnabled & isClickAdInterstitial
     * - ActionType.SWIPE -> isSwipeAdEnabled & isSwipeAdInterstitial
     * - ActionType.BACK  -> isBackAdEnabled & isClickAdInterstitial
     */
    fun showAdForAction(
        activity: Activity,
        actionType: ActionType,
        onProceed: () -> Unit
    ) {
        if (activity.isFinishing || activity.isDestroyed) {
            onProceed()
            return
        }

        val config = AdsConfigManager.config
        val isActionEnabled = when (actionType) {
            ActionType.CLICK -> config.isClickAdEnabled
            ActionType.SWIPE -> config.isSwipeAdEnabled
            ActionType.BACK  -> config.isBackAdEnabled
        }

        if (!isActionEnabled) {
            // Strict Rule: Skip ad display and counter increment completely
            Log.d(TAG, "showAdForAction: action=$actionType is disabled by Remote Config -> proceeding directly")
            onProceed()
            return
        }

        val (counter, trigger, showInterstitial) = when (actionType) {
            ActionType.CLICK -> {
                val count = InterAdLoader.increaseClickCount()
                val trig = config.clickAdCounterTrigger.coerceAtLeast(1)
                val showInter = config.isClickAdInterstitial
                Triple(count, trig, showInter)
            }
            ActionType.SWIPE -> {
                val count = InterAdLoader.increaseForwardCount()
                val trig = config.interAdCounterTrigger.coerceAtLeast(1)
                val showInter = config.isSwipeAdInterstitial
                Triple(count, trig, showInter)
            }
            ActionType.BACK -> {
                val count = InterAdLoader.increaseBackwardCount()
                val trig = config.interAdBackCounterTrigger.coerceAtLeast(1)
                val showInter = config.isClickAdInterstitial
                Triple(count, trig, showInter)
            }
        }

        Log.d(TAG, "showAdForAction: action=$actionType, count=$counter, trigger=$trigger, showInterstitial=$showInterstitial")

        if (counter >= trigger) {
            // Reset ONLY this specific counter register back to zero
            when (actionType) {
                ActionType.CLICK -> InterAdLoader.resetClickCount()
                ActionType.SWIPE -> InterAdLoader.resetForwardCount()
                ActionType.BACK  -> InterAdLoader.resetBackwardCount()
            }

            showRoutedFullScreenAd(
                activity = activity,
                showInterstitial = showInterstitial,
                onDismiss = {
                    SystemUiHelper.applyStickyImmersiveMode(activity)
                    if (!activity.isFinishing && !activity.isDestroyed) {
                        onProceed()
                    }
                }
            )
        } else {
            onProceed()
        }
    }

    /**
     * Dynamic routing with graceful fallback and strict network guards:
     * - showInterstitial == true: Present Interstitial if ready. If not ready, fallback to cached App Open. Else loads & presents Interstitial.
     * - showInterstitial == false: Present App Open if ready. If not ready, fallback to cached Interstitial. Else loads & presents App Open.
     */
    fun showRoutedFullScreenAd(
        activity: Activity,
        showInterstitial: Boolean,
        onDismiss: () -> Unit
    ) {
        if (activity.isFinishing || activity.isDestroyed) {
            onDismiss()
            return
        }

        val config = AdsConfigManager.config
        val canShowInter = config.canShowInter
        val canShowAppOpen = config.canShowAppOpen

        if (!canShowInter && !canShowAppOpen) {
            Log.d(TAG, "showRoutedFullScreenAd: Both Interstitial and App Open are disabled -> proceeding")
            onDismiss()
            return
        }

        val interLoader = InterAdLoader.instance
        val openLoader = AppOpenAdLoader.instance

        val isInterReady = canShowInter && interLoader?.isInterstitialReady == true
        val isOpenReady = canShowAppOpen && openLoader?.isAvailableAppOpenAd == true

        Log.d(TAG, "showRoutedFullScreenAd: showInterstitial=$showInterstitial, isInterReady=$isInterReady, isOpenReady=$isOpenReady, canShowInter=$canShowInter, canShowAppOpen=$canShowAppOpen")

        fun safeDismiss() {
            SystemUiHelper.applyStickyImmersiveMode(activity)
            onDismiss()
        }

        if (showInterstitial) {
            when {
                isInterReady -> {
                    Log.d(TAG, "Presenting preferred Interstitial Ad (cached)")
                    interLoader?.showInterstitialDirect(activity) { safeDismiss() } ?: safeDismiss()
                }
                isOpenReady -> {
                    Log.d(TAG, "Preferred Interstitial not cached -> Fallback to cached App Open Ad")
                    openLoader?.showAppOpenAd(activity) { safeDismiss() } ?: safeDismiss()
                }
                canShowInter -> {
                    Log.d(TAG, "Neither cached -> Loading and presenting Interstitial")
                    interLoader?.showInterstitialDirect(activity) { safeDismiss() } ?: safeDismiss()
                }
                canShowAppOpen -> {
                    Log.d(TAG, "Interstitial disabled -> Loading and presenting App Open fallback")
                    openLoader?.showAppOpenAd(activity) { safeDismiss() } ?: safeDismiss()
                }
                else -> safeDismiss()
            }
        } else {
            when {
                isOpenReady -> {
                    Log.d(TAG, "Presenting preferred App Open Ad (cached)")
                    openLoader?.showAppOpenAd(activity) { safeDismiss() } ?: safeDismiss()
                }
                isInterReady -> {
                    Log.d(TAG, "Preferred App Open not cached -> Fallback to cached Interstitial Ad")
                    interLoader?.showInterstitialDirect(activity) { safeDismiss() } ?: safeDismiss()
                }
                canShowAppOpen -> {
                    Log.d(TAG, "Neither cached -> Loading and presenting App Open")
                    openLoader?.showAppOpenAd(activity) { safeDismiss() } ?: safeDismiss()
                }
                canShowInter -> {
                    Log.d(TAG, "App Open disabled -> Loading and presenting Interstitial fallback")
                    interLoader?.showInterstitialDirect(activity) { safeDismiss() } ?: safeDismiss()
                }
                else -> safeDismiss()
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Native & Banner Ad Helpers (Guarded)
    // ─────────────────────────────────────────────────────────────────────────

    fun showDashboardNative(
        activity: Activity,
        nativeFrame: FrameLayout,
        shimmerFrame: FrameLayout,
        nativeCard: CardView
    ) {
        if (!AdsConfigManager.config.canShowNativeDashboard) {
            nativeFrame.removeAllViews()
            nativeFrame.visibility = View.GONE
            shimmerFrame.visibility = View.GONE
            nativeCard.visibility = View.GONE
            return
        }
        NativeAdLoader.instance?.showNativeLarge(activity, nativeFrame, shimmerFrame)
    }

    fun loadGoogleSearchNative(
        activity: Activity,
        onLoaded: (NativeAd) -> Unit,
        onFailed: () -> Unit
    ) {
        if (!AdsConfigManager.config.canShowNativeGoogleSearch) {
            onFailed()
            return
        }
        val adUnitId = AdsConfigManager.config.nativeAdIdGoogleSearch
        NativeAdLoader.instance?.loadNativeAd(
            activity,
            adUnitId,
            "GoogleSearch",
            onLoaded,
            onFailed
        ) ?: onFailed()
    }

    fun bindGoogleSearchNative(
        activity: Activity,
        nativeFrame: FrameLayout,
        shimmerFrame: FrameLayout,
        nativeAd: NativeAd
    ) {
        NativeAdLoader.instance?.bindNativeLarge(
            activity,
            nativeFrame,
            shimmerFrame,
            nativeAd,
            "GoogleSearch"
        )
    }

    fun showDrawerBanner(
        activity: Activity,
        bannerFrame: FrameLayout,
        shimmerFrame: FrameLayout
    ) {
        if (!AdsConfigManager.config.canShowBannerAppDrawer) {
            bannerFrame.removeAllViews()
            bannerFrame.visibility = View.GONE
            shimmerFrame.visibility = View.GONE
            return
        }
        BannerAdLoader.instance?.showDrawerBanner(activity, bannerFrame, shimmerFrame)
    }

    fun showFindPhoneBanner(
        activity: Activity,
        bannerFrame: FrameLayout,
        shimmerFrame: FrameLayout
    ) {
        if (!AdsConfigManager.config.canShowBannerFindPhone) {
            bannerFrame.removeAllViews()
            bannerFrame.visibility = View.GONE
            shimmerFrame.visibility = View.GONE
            return
        }
        BannerAdLoader.instance?.showFindPhoneBanner(activity, bannerFrame, shimmerFrame)
    }

    fun showAlertBanner(
        activity: Activity,
        bannerFrame: FrameLayout,
        shimmerFrame: FrameLayout
    ) {
        if (!AdsConfigManager.config.canShowBannerAlertScreen) {
            bannerFrame.removeAllViews()
            bannerFrame.visibility = View.GONE
            shimmerFrame.visibility = View.GONE
            return
        }
        BannerAdLoader.instance?.showAlertBanner(activity, bannerFrame, shimmerFrame)
    }
}
