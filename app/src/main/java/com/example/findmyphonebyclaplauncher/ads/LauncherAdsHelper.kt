package com.example.findmyphonebyclaplauncher.ads

import android.app.Activity
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import androidx.cardview.widget.CardView
import com.example.findmyphonebyclaplauncher.ads.config.AdsConfigManager
import com.example.findmyphonebyclaplauncher.ads.config.RemoteConfigRepository
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
        if (RemoteConfigRepository.isInterAdEnabled && RemoteConfigRepository.interAdId.isNotBlank()) {
            InterAdLoader.instance?.loadInterstitialAds(activity)
        }
    }

    fun preloadAppOpen(activity: Activity) {
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

        val config = AdsConfigManager.config
        val isAdEnabled = when (placement) {
            AdPlacement.APP_CLICK -> config.isClickAdEnabled && (if (config.isClickAdInterstitial) config.canShowInter else config.canShowAppOpen)
            AdPlacement.SWAP -> config.isSwipeAdEnabled && (if (config.isSwipeAdInterstitial) config.canShowInter else config.canShowAppOpen)
            AdPlacement.BACK_PRESS -> config.isBackAdEnabled && config.canShowInter
        }

        Log.d("AdPlacementDebug", "Placement: $placement | Enabled: $isAdEnabled")

        if (!isAdEnabled) {
            // Flag is false for this specific placement: Skip ad completely
            onAdClosed()
            return
        }

        val (counter, trigger) = when (placement) {
            AdPlacement.APP_CLICK -> {
                val count = InterAdLoader.increaseClickCount()
                val trig = config.clickAdCounterTrigger.coerceAtLeast(1)
                Pair(count, trig)
            }
            AdPlacement.SWAP -> {
                val count = InterAdLoader.increaseForwardCount()
                val trig = config.interAdCounterTrigger.coerceAtLeast(1)
                Pair(count, trig)
            }
            AdPlacement.BACK_PRESS -> {
                val count = InterAdLoader.increaseBackwardCount()
                val trig = config.interAdBackCounterTrigger.coerceAtLeast(1)
                Pair(count, trig)
            }
        }

        Log.d("AdPlacementDebug", "Placement: $placement | Counter: $counter / $trigger")

        if (counter >= trigger) {
            when (placement) {
                AdPlacement.APP_CLICK -> InterAdLoader.resetClickCount()
                AdPlacement.SWAP -> InterAdLoader.resetForwardCount()
                AdPlacement.BACK_PRESS -> InterAdLoader.resetBackwardCount()
            }

            when (placement) {
                AdPlacement.APP_CLICK -> {
                    if (config.isClickAdInterstitial) {
                        showClickInterstitialAd(activity, onAdClosed)
                    } else {
                        showAppOpenAd(activity, onAdClosed)
                    }
                }
                AdPlacement.SWAP -> {
                    if (config.isSwipeAdInterstitial) {
                        showClickInterstitialAd(activity, onAdClosed)
                    } else {
                        showAppOpenAd(activity, onAdClosed)
                    }
                }
                AdPlacement.BACK_PRESS -> {
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
        val config = AdsConfigManager.config
        if (!config.canShowInter) {
            Log.d(TAG, "showClickInterstitialAd: canShowInter is false -> proceeding")
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
        val config = AdsConfigManager.config
        if (!config.canShowAppOpen) {
            Log.d(TAG, "showAppOpenAd: canShowAppOpen is false -> proceeding")
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

        val config = AdsConfigManager.config
        val isClickAdEnabled = config.isClickAdEnabled
        val isClickAdInterstitial = config.isClickAdInterstitial
        val triggerThreshold = config.clickAdCounterTrigger.coerceAtLeast(1)

        Log.d("AdRouting", "is_click_ad_enabled: $isClickAdEnabled | is_click_ad_interstitial: $isClickAdInterstitial")

        if (!isClickAdEnabled) {
            onContinue()
            return
        }

        val clickCount = InterAdLoader.increaseClickCount()
        Log.d("AdRouting", "App clicked: $packageName | Counter: $clickCount / $triggerThreshold")

        if (clickCount >= triggerThreshold) {
            InterAdLoader.resetClickCount()

            if (isClickAdInterstitial) {
                // Flag is TRUE -> Display Interstitial Ad using inter_ad_id
                Log.d("AdRouting", "Threshold reached. Showing Interstitial Ad (inter_ad_id)...")
                showClickInterstitialAd(activity) {
                    onContinue()
                }
            } else {
                // Flag is FALSE -> Display App Open Ad using app_open_ad_id
                Log.d("AdRouting", "Threshold reached. Showing App Open Ad (app_open_ad_id)...")
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

        val config = AdsConfigManager.config
        val isSwipeEnabled = config.isSwipeAdEnabled
        val isSwipeInter = config.isSwipeAdInterstitial
        val trigger = config.interAdCounterTrigger.coerceAtLeast(1)
        Log.d("AdRouting", "Swipe ad requested: is_swipe_ad_enabled=$isSwipeEnabled | is_swipe_ad_interstitial=$isSwipeInter")

        if (!isSwipeEnabled) {
            onDone()
            return
        }

        val count = InterAdLoader.increaseForwardCount()
        Log.d("AdRouting", "Swipe counter: $count / $trigger")

        if (count >= trigger) {
            InterAdLoader.resetForwardCount()
            if (isSwipeInter) {
                // Flag is TRUE -> Display Interstitial Ad using inter_ad_id
                Log.d("AdRouting", "Threshold reached. Showing Interstitial Ad (inter_ad_id)...")
                showClickInterstitialAd(activity) {
                    onDone()
                }
            } else {
                // Flag is FALSE -> Display App Open Ad using app_open_ad_id
                Log.d("AdRouting", "Threshold reached. Showing App Open Ad (app_open_ad_id)...")
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
        Log.d("RemoteConfigDebug", "inter_ad_back_counter_trigger value: $value")
        return value
    }

    fun onFeatureBackRequested(activity: Activity, onComplete: () -> Unit) {
        if (activity.isFinishing || activity.isDestroyed) {
            onComplete()
            return
        }

        val config = AdsConfigManager.config
        val isBackAdEnabled = config.isBackAdEnabled && config.isInterAdEnabled
        val triggerCount = getBackAdTriggerCount().coerceAtLeast(1)

        Log.d("BackAdDebug", "Back pressed inside Screen. Counter: ${InterAdLoader.interstitialBackwardCount} / $triggerCount, Enabled: $isBackAdEnabled")

        if (!isBackAdEnabled) {
            Log.d("BackAdDebug", "Back ad disabled by Remote Config -> proceeding immediately")
            onComplete()
            return
        }

        val backPressCount = InterAdLoader.increaseBackwardCount()
        Log.d("BackAdDebug", "Feature screen back pressed: count = $backPressCount / $triggerCount")

        if (backPressCount >= triggerCount) {
            InterAdLoader.resetBackwardCount()
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
        if (activity.isFinishing || activity.isDestroyed || !AdsConfigManager.config.canShowInter) return
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

    /**
     * Dedicated isolated routing:
     * - showInterstitial == true: Displays Interstitial Ad (inter_ad_id)
     * - showInterstitial == false: Displays App Open Ad (app_open_ad_id)
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

        if (showInterstitial) {
            showClickInterstitialAd(activity, onDismiss)
        } else {
            showAppOpenAd(activity, onDismiss)
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
