package com.example.findmyphonebyclaplauncher.ads

import android.app.Activity
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import androidx.cardview.widget.CardView
import com.example.findmyphonebyclaplauncher.ads.config.AdsConfigManager
import com.example.findmyphonebyclaplauncher.ads.config.RemoteConfigRepository
import com.example.findmyphonebyclaplauncher.util.NetworkUtil
import com.example.findmyphonebyclaplauncher.util.SystemUiHelper
import com.google.android.gms.ads.nativead.NativeAd

object LauncherAdsHelper {

    private const val TAG = "LauncherAdsHelper"

    enum class AdPlacement {
        APP_CLICK,
        SWAP,
        BACK_PRESS
    }

    enum class ActionType {
        CLICK,
        SWIPE,
        BACK
    }

    fun preloadInterstitial(activity: Activity) {
        if (!NetworkUtil.isNetworkAvailable(activity)) return
        if (RemoteConfigRepository.isInterAdEnabled && RemoteConfigRepository.interAdId.isNotBlank()) {
            InterAdLoader.instance?.loadInterstitialAds(activity)
        }
    }

    fun preloadAppOpen(activity: Activity) {
        if (!NetworkUtil.isNetworkAvailable(activity)) return
        if (RemoteConfigRepository.isAppOpenAdEnabled && RemoteConfigRepository.appOpenAdId.isNotBlank()) {
            AppOpenAdLoader.instance?.preloadAppOpenAd(activity)
        }
    }

    fun showAdForPlacement(
        activity: Activity,
        placement: AdPlacement,
        onAdClosed: () -> Unit
    ) {
        if (activity.isFinishing || activity.isDestroyed) {
            onAdClosed()
            return
        }

        if (!NetworkUtil.isNetworkAvailable(activity)) {
            Log.d("AdCounter", "showAdForPlacement: Offline mode. Suppressing ad for $placement")
            onAdClosed()
            return
        }

        val config = AdsConfigManager.config
        val isAdEnabled = when (placement) {
            AdPlacement.APP_CLICK -> config.isClickAdEnabled && (if (config.isClickAdInterstitial) config.canShowInter else config.canShowAppOpen)
            AdPlacement.SWAP -> config.isSwipeAdEnabled && (if (config.isSwipeAdInterstitial) config.canShowInter else config.canShowAppOpen)
            AdPlacement.BACK_PRESS -> config.isBackAdEnabled && config.canShowInter
        }

        Log.d("AdCounter", "showAdForPlacement: Placement=$placement, Enabled=$isAdEnabled")

        if (!isAdEnabled) {
            // Flag is false for this specific placement: Skip ad completely
            onAdClosed()
            return
        }

        val (counter, trigger) = when (placement) {
            AdPlacement.APP_CLICK -> {
                val count = InterAdLoader.increaseLauncherClickCount()
                val trig = config.clickAdCounterTrigger.coerceAtLeast(1)
                Pair(count, trig)
            }
            AdPlacement.SWAP -> {
                val count = InterAdLoader.increaseForwardCount()
                val trig = config.interAdCounterTrigger.coerceAtLeast(1)
                Pair(count, trig)
            }
            AdPlacement.BACK_PRESS -> {
                val count = InterAdLoader.increaseInAppBackCount()
                val trig = config.interAdBackCounterTrigger.coerceAtLeast(1)
                Pair(count, trig)
            }
        }

        Log.d("AdCounter", "Placement=$placement -> Counter=$counter / Trigger=$trigger")

        if (counter >= trigger) {
            Log.d("AdCounter", "Threshold reached for $placement! Resetting counter.")
            when (placement) {
                AdPlacement.APP_CLICK -> InterAdLoader.resetLauncherClickCount()
                AdPlacement.SWAP -> InterAdLoader.resetForwardCount()
                AdPlacement.BACK_PRESS -> InterAdLoader.resetInAppBackCount()
            }

            when (placement) {
                AdPlacement.APP_CLICK -> {
                    if (config.isClickAdInterstitial) {
                        Log.d("InterstitialAd", "Click trigger hit. is_click_ad_interstitial=true. Showing Interstitial.")
                        showClickInterstitialAd(activity, onAdClosed)
                    } else {
                        Log.d("AppOpenAd", "Click trigger hit. is_click_ad_interstitial=false. Showing App Open Ad.")
                        showAppOpenAd(activity, onAdClosed)
                    }
                }
                AdPlacement.SWAP -> {
                    if (config.isSwipeAdInterstitial) {
                        Log.d("InterstitialAd", "Swipe trigger hit. is_swipe_ad_interstitial=true. Showing Interstitial.")
                        showClickInterstitialAd(activity, onAdClosed)
                    } else {
                        Log.d("AppOpenAd", "Swipe trigger hit. is_swipe_ad_interstitial=false. Showing App Open Ad.")
                        showAppOpenAd(activity, onAdClosed)
                    }
                }
                AdPlacement.BACK_PRESS -> {
                    Log.d("InterstitialAd", "In-app back press trigger hit. Showing Back Interstitial Ad.")
                    showBackInterstitialAd(activity, onAdClosed)
                }
            }
        } else {
            onAdClosed()
        }
    }

    fun showClickInterstitialAd(activity: Activity, onDone: () -> Unit) {
        if (activity.isFinishing || activity.isDestroyed) {
            onDone()
            return
        }
        if (!NetworkUtil.isNetworkAvailable(activity)) {
            Log.d("InterstitialAd", "showClickInterstitialAd: Offline mode -> proceeding")
            onDone()
            return
        }
        val config = AdsConfigManager.config
        if (!config.canShowInter) {
            Log.d("InterstitialAd", "showClickInterstitialAd: canShowInter is false -> proceeding")
            onDone()
            return
        }
        InterAdLoader.instance?.showOrLoadInterstitial(activity, isFromBack = false) {
            SystemUiHelper.applyStickyImmersiveMode(activity)
            if (!activity.isFinishing && !activity.isDestroyed) {
                onDone()
            }
        } ?: onDone()
    }

    fun showInterstitialAd(activity: Activity, onDone: () -> Unit) {
        showClickInterstitialAd(activity, onDone)
    }

    fun showAppOpenAd(activity: Activity, onDone: () -> Unit) {
        if (activity.isFinishing || activity.isDestroyed) {
            onDone()
            return
        }
        if (!NetworkUtil.isNetworkAvailable(activity)) {
            Log.d("AppOpenAd", "showAppOpenAd: Offline mode -> proceeding")
            onDone()
            return
        }
        val config = AdsConfigManager.config
        if (!config.canShowAppOpen) {
            Log.d("AppOpenAd", "showAppOpenAd: canShowAppOpen is false -> proceeding")
            onDone()
            return
        }
        AppOpenAdLoader.instance?.showAppOpenAd(activity) {
            SystemUiHelper.applyStickyImmersiveMode(activity)
            if (!activity.isFinishing && !activity.isDestroyed) {
                onDone()
            }
        } ?: onDone()
    }

    fun showAppClickAd(activity: Activity, packageName: String, onContinue: () -> Unit) {
        if (activity.isFinishing || activity.isDestroyed) {
            onContinue()
            return
        }

        if (!NetworkUtil.isNetworkAvailable(activity)) {
            Log.d("AdCounter", "showAppClickAd: Offline mode -> opening app directly")
            onContinue()
            return
        }

        val config = AdsConfigManager.config
        val isClickAdEnabled = config.isClickAdEnabled
        val isClickAdInterstitial = config.isClickAdInterstitial
        val triggerThreshold = config.clickAdCounterTrigger.coerceAtLeast(1)

        if (!isClickAdEnabled) {
            Log.d("AdCounter", "is_click_ad_enabled is false -> skipping launcher click ad")
            onContinue()
            return
        }

        val clickCount = InterAdLoader.increaseLauncherClickCount()
        Log.d("AdCounter", "Launcher item clicked: $packageName | Current count: $clickCount / Trigger: $triggerThreshold")

        if (clickCount >= triggerThreshold) {
            Log.d("AdCounter", "Launcher click threshold reached ($clickCount >= $triggerThreshold). Resetting count to 0.")
            InterAdLoader.resetLauncherClickCount()

            if (isClickAdInterstitial) {
                Log.d("InterstitialAd", "Click trigger hit. is_click_ad_interstitial=true. Showing Interstitial.")
                showClickInterstitialAd(activity) {
                    onContinue()
                }
            } else {
                Log.d("AppOpenAd", "Click trigger hit. is_click_ad_interstitial=false. Showing App Open Ad.")
                showAppOpenAd(activity) {
                    onContinue()
                }
            }
        } else {
            // Counter threshold not reached yet -> open app directly
            onContinue()
        }
    }

    fun showSwipeAd(activity: Activity, onDone: () -> Unit = {}) {
        if (activity.isFinishing || activity.isDestroyed) {
            onDone()
            return
        }

        if (!NetworkUtil.isNetworkAvailable(activity)) {
            Log.d("AdCounter", "showSwipeAd: Offline mode -> skipping swipe ad")
            onDone()
            return
        }

        val config = AdsConfigManager.config
        val isSwipeEnabled = config.isSwipeAdEnabled
        val isSwipeInter = config.isSwipeAdInterstitial
        val trigger = config.interAdCounterTrigger.coerceAtLeast(1)

        if (!isSwipeEnabled) {
            Log.d("AdCounter", "is_swipe_ad_enabled is false -> skipping swipe ad")
            onDone()
            return
        }

        val count = InterAdLoader.increaseForwardCount()
        Log.d("AdCounter", "Swipe action performed. Current count: $count / Trigger: $trigger")

        if (count >= trigger) {
            Log.d("AdCounter", "Swipe threshold reached ($count >= $trigger). Resetting count to 0.")
            InterAdLoader.resetForwardCount()
            if (isSwipeInter) {
                Log.d("InterstitialAd", "Swipe trigger hit. is_swipe_ad_interstitial=true. Showing Interstitial.")
                showClickInterstitialAd(activity) {
                    onDone()
                }
            } else {
                Log.d("AppOpenAd", "Swipe trigger hit. is_swipe_ad_interstitial=false. Showing App Open Ad.")
                showAppOpenAd(activity) {
                    onDone()
                }
            }
        } else {
            onDone()
        }
    }

    fun showSwapInterstitialAd(activity: Activity, onDone: () -> Unit = {}) {
        showSwipeAd(activity, onDone)
    }

    fun getBackAdTriggerCount(): Int {
        val value = AdsConfigManager.config.interAdBackCounterTrigger
        Log.d("RemoteConfig", "inter_ad_back_counter_trigger value: $value")
        return value
    }

    fun onFeatureBackRequested(activity: Activity, onComplete: () -> Unit) {
        if (activity.isFinishing || activity.isDestroyed) {
            onComplete()
            return
        }

        if (!NetworkUtil.isNetworkAvailable(activity)) {
            Log.d("InterstitialAd", "onFeatureBackRequested: Offline mode -> proceeding immediately")
            onComplete()
            return
        }

        val config = AdsConfigManager.config
        val isBackAdEnabled = config.isBackAdEnabled && config.isInterAdEnabled
        val triggerCount = getBackAdTriggerCount().coerceAtLeast(1)

        if (!isBackAdEnabled) {
            Log.d("InterstitialAd", "Back ad disabled by Remote Config -> proceeding immediately")
            onComplete()
            return
        }

        val backPressCount = InterAdLoader.increaseInAppBackCount()
        Log.d("AdCounter", "In-app back pressed. Current count: $backPressCount / Trigger: $triggerCount")

        if (backPressCount >= triggerCount) {
            Log.d("AdCounter", "In-app back threshold reached ($backPressCount >= $triggerCount). Resetting count to 0.")
            InterAdLoader.resetInAppBackCount()
            Log.d("InterstitialAd", "Presenting Back Interstitial Ad...")
            InterAdLoader.instance?.showOrLoadInterstitial(activity, isFromBack = true) {
                SystemUiHelper.applyStickyImmersiveMode(activity)
                if (!activity.isFinishing && !activity.isDestroyed) {
                    onComplete()
                }
            } ?: onComplete()
        } else {
            onComplete()
        }
    }

    fun showBackInterstitialAd(activity: Activity, onDone: () -> Unit) {
        onFeatureBackRequested(activity, onDone)
    }

    fun showSwipeInter(activity: Activity, onDone: () -> Unit = {}) {
        showSwipeAd(activity, onDone)
    }

    fun showBackAd(activity: Activity, onDone: () -> Unit) {
        showBackInterstitialAd(activity, onDone)
    }

    fun showAppClickInterThen(activity: Activity, onContinue: () -> Unit) {
        showAppClickAd(activity, "", onContinue)
    }

    fun showAppClickAdThen(activity: Activity, onContinue: () -> Unit) {
        showAppClickAd(activity, "", onContinue)
    }

    fun showInterThen(activity: Activity, onContinue: () -> Unit) {
        showAppClickAd(activity, "", onContinue)
    }

    fun showBlogReturnInter(activity: Activity) {
        if (activity.isFinishing || activity.isDestroyed || !NetworkUtil.isNetworkAvailable(activity) || !AdsConfigManager.config.canShowInter) return
        InterAdLoader.instance?.showInterstitialImmediate(activity) {}
    }

    fun showAdForAction(
        activity: Activity,
        actionType: ActionType,
        onProceed: () -> Unit
    ) {
        when (actionType) {
            ActionType.CLICK -> showAdForPlacement(activity, AdPlacement.APP_CLICK, onProceed)
            ActionType.SWIPE -> showAdForPlacement(activity, AdPlacement.SWAP, onProceed)
            ActionType.BACK  -> showAdForPlacement(activity, AdPlacement.BACK_PRESS, onProceed)
        }
    }

    fun showRoutedFullScreenAd(
        activity: Activity,
        showInterstitial: Boolean,
        onDismiss: () -> Unit
    ) {
        if (activity.isFinishing || activity.isDestroyed || !NetworkUtil.isNetworkAvailable(activity)) {
            onDismiss()
            return
        }

        if (showInterstitial) {
            showClickInterstitialAd(activity, onDismiss)
        } else {
            showAppOpenAd(activity, onDismiss)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Native & Banner Ad Helpers (Guarded with Network & Remote Config)
    // ─────────────────────────────────────────────────────────────────────────

    fun showDashboardNative(
        activity: Activity,
        nativeFrame: FrameLayout,
        shimmerFrame: FrameLayout,
        nativeCard: CardView
    ) {
        if (!NetworkUtil.isNetworkAvailable(activity) || !AdsConfigManager.config.canShowNativeDashboard) {
            NativeAdLoader.instance?.hideNativeContainer(nativeFrame, shimmerFrame, nativeCard)
            return
        }
        NativeAdLoader.instance?.showDashboardNative(activity, nativeFrame, shimmerFrame, nativeCard)
    }

    fun loadGoogleSearchNative(
        activity: Activity,
        onLoaded: (NativeAd) -> Unit,
        onFailed: () -> Unit
    ) {
        if (!NetworkUtil.isNetworkAvailable(activity) || !AdsConfigManager.config.canShowNativeGoogleSearch) {
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
        nativeAd: NativeAd,
        cardView: CardView? = null
    ) {
        NativeAdLoader.instance?.bindNativeLarge(
            activity,
            nativeFrame,
            shimmerFrame,
            nativeAd,
            "GoogleSearch",
            cardView
        )
    }

    fun showDrawerBanner(
        activity: Activity,
        bannerFrame: FrameLayout,
        shimmerFrame: FrameLayout,
        rootContainer: View? = null
    ) {
        if (!NetworkUtil.isNetworkAvailable(activity) || !AdsConfigManager.config.canShowBannerAppDrawer) {
            BannerAdLoader.instance?.hideBannerContainer(bannerFrame, shimmerFrame, rootContainer)
            return
        }
        BannerAdLoader.instance?.showDrawerBanner(activity, bannerFrame, shimmerFrame, rootContainer)
    }

    fun showFindPhoneBanner(
        activity: Activity,
        bannerFrame: FrameLayout,
        shimmerFrame: FrameLayout,
        rootContainer: View? = null
    ) {
        if (!NetworkUtil.isNetworkAvailable(activity) || !AdsConfigManager.config.canShowBannerFindPhone) {
            BannerAdLoader.instance?.hideBannerContainer(bannerFrame, shimmerFrame, rootContainer)
            return
        }
        BannerAdLoader.instance?.showFindPhoneBanner(activity, bannerFrame, shimmerFrame, rootContainer)
    }

    fun showAlertBanner(
        activity: Activity,
        bannerFrame: FrameLayout,
        shimmerFrame: FrameLayout,
        rootContainer: View? = null
    ) {
        if (!NetworkUtil.isNetworkAvailable(activity) || !AdsConfigManager.config.canShowBannerAlertScreen) {
            BannerAdLoader.instance?.hideBannerContainer(bannerFrame, shimmerFrame, rootContainer)
            return
        }
        BannerAdLoader.instance?.showAlertBanner(activity, bannerFrame, shimmerFrame, rootContainer)
    }

    fun showLanguageRectBanner(
        activity: Activity,
        bannerFrame: FrameLayout,
        shimmerFrame: View,
        rootContainer: View? = null
    ) {
        if (!NetworkUtil.isNetworkAvailable(activity) || !AdsConfigManager.config.canShowBannerLanguageRect) {
            BannerAdLoader.instance?.hideBannerContainer(bannerFrame, shimmerFrame, rootContainer)
            return
        }
        BannerAdLoader.instance?.showLanguageRectBanner(activity, bannerFrame, shimmerFrame, rootContainer)
    }
}
