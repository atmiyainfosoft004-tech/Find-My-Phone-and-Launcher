package com.example.findmyphonebyclaplauncher.ads

import android.app.Activity
import android.view.View
import android.widget.FrameLayout
import androidx.cardview.widget.CardView
import com.example.findmyphonebyclaplauncher.ads.config.AdsConfigManager
import com.google.android.gms.ads.nativead.NativeAd

object LauncherAdsHelper {

    fun preloadInterstitial(activity: Activity) {
        InterAdLoader.instance?.loadInterstitialAds(activity)
    }

    fun showSwipeInter(activity: Activity, onDone: () -> Unit = {}) {
        InterAdLoader.instance?.showSwipeInterstitial(activity) { onDone() } ?: onDone()
    }

    fun showAppClickInterThen(activity: Activity, onContinue: () -> Unit) {
        InterAdLoader.instance?.showAppClickInterstitial(activity) {
            if (!activity.isFinishing) onContinue()
        } ?: onContinue()
    }

    fun showInterThen(activity: Activity, onContinue: () -> Unit) {
        InterAdLoader.instance?.showInterstitialAd(activity, false) {
            if (!activity.isFinishing) onContinue()
        } ?: onContinue()
    }

    fun showBlogReturnInter(activity: Activity) {
        if (activity.isFinishing || activity.isDestroyed) return
        InterAdLoader.instance?.showInterstitialImmediate(activity) {}
    }

    fun showDashboardNative(
        activity: Activity,
        nativeFrame: FrameLayout,
        shimmerFrame: FrameLayout,
        nativeCard: CardView
    ) {
        if (!AdsConfigManager.config.canShowNative) {
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
        if (!AdsConfigManager.config.canShowNative) {
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
        BannerAdLoader.instance?.showDrawerBanner(activity, bannerFrame, shimmerFrame)
    }
}
