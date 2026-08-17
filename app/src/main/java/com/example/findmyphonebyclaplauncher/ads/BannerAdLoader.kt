package com.example.findmyphonebyclaplauncher.ads

import android.app.Activity
import android.content.Context
import android.os.Build
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowMetrics
import android.widget.FrameLayout
import com.example.findmyphonebyclaplauncher.App
import com.example.findmyphonebyclaplauncher.R
import com.example.findmyphonebyclaplauncher.ads.config.AdsConfigManager
import com.example.findmyphonebyclaplauncher.analytics.AnalyticsHelper.logAds
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

class BannerAdLoader {
    private val ads get() = AdsConfigManager.config
    private var bannerAdPreload: AdView? = null

    fun loadBannerAdPreload(activity: Activity) {
        if (ads.bannerAdPreload) {
            App.runWhenMobileAdsReady {
                bannerAdPreload = null
                val adView = AdView(activity)
                adView.adUnitId = ads.bannerAdIdHome
                adView.setAdSize(getNormalAdSize(activity))
                adView.adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        super.onAdLoaded()
                        logAds(activity, "banner_loaded")
                        bannerAdPreload = adView
                    }

                    override fun onAdClicked() {
                        super.onAdClicked()
                        logAds(activity, "banner_clicked")
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        super.onAdFailedToLoad(loadAdError)
                        Log.e(
                            "LoadBannerAdCheck",
                            "loadBannerAdPreload -> failed code=${loadAdError.code} domain=${loadAdError.domain} msg=${loadAdError.message} cause=${loadAdError.cause}"
                        )
                        logAds(activity, "banner_failed_to_load_" + loadAdError.code)
                    }
                }
                val adRequest = AdRequest.Builder().build()
                logAds(activity, "banner_req")
                adView.loadAd(adRequest)
            }
        }
    }

    fun showSplashBanner(
        activity: Activity,
        frameLayout: FrameLayout,
        shimmerFrameLayout: FrameLayout
    ) {
        if (ads.canShowBannerSplash) {
            loadAndShowBanner(
                activity,
                frameLayout,
                shimmerFrameLayout,
                ads.bannerAdIdSplash
            )
        } else {
            frameLayout.removeAllViews()
            frameLayout.visibility = View.GONE
            shimmerFrameLayout.visibility = View.GONE
        }
    }

    fun showHomeBanner(
        activity: Activity,
        frameLayout: FrameLayout,
        shimmerFrameLayout: FrameLayout
    ) {
        if (ads.canShowBannerHome) {
            if (bannerAdPreload != null) {
                Log.e("LoadBannerAdCheck", "bannerAdPreload!=null")
                frameLayout.visibility = View.VISIBLE
                shimmerFrameLayout.visibility = View.GONE

                if (bannerAdPreload!!.parent != null) {
                    (bannerAdPreload!!.parent as ViewGroup).removeView(bannerAdPreload)
                }
                frameLayout.removeAllViews()
                logAds(activity, "banner_showed")
                frameLayout.addView(bannerAdPreload)
                bannerAdPreload = null
                Log.e("LoadBannerAdCheck", "loadBannerAdPreload: showBanner")
                loadBannerAdPreload(activity)
            } else {
                loadAndShowBanner(
                    activity,
                    frameLayout,
                    shimmerFrameLayout,
                    ads.bannerAdIdHome
                )
            }
        } else {
            frameLayout.removeAllViews()
            frameLayout.visibility = View.GONE
            shimmerFrameLayout.visibility = View.GONE
        }
    }

    fun showBanner(
        activity: Activity,
        frameLayout: FrameLayout,
        shimmerFrameLayout: FrameLayout
    ) {
        showHomeBanner(activity, frameLayout, shimmerFrameLayout)
    }

    fun showDrawerBanner(
        activity: Activity,
        frameLayout: FrameLayout,
        shimmerFrameLayout: FrameLayout
    ) {
        if (ads.canShowBannerAppDrawer) {
            loadAndShowBanner(
                activity,
                frameLayout,
                shimmerFrameLayout,
                ads.bannerAdIdAppDrawer.ifBlank { ads.bannerAdIdHome }
            )
        } else {
            frameLayout.removeAllViews()
            frameLayout.visibility = View.GONE
            shimmerFrameLayout.visibility = View.GONE
        }
    }

    fun showFindPhoneBanner(
        activity: Activity,
        frameLayout: FrameLayout,
        shimmerFrameLayout: FrameLayout
    ) {
        if (ads.canShowBannerFindPhone) {
            loadAndShowBanner(
                activity,
                frameLayout,
                shimmerFrameLayout,
                ads.bannerAdIdFindPhone.ifBlank { ads.bannerAdIdHome }
            )
        } else {
            frameLayout.removeAllViews()
            frameLayout.visibility = View.GONE
            shimmerFrameLayout.visibility = View.GONE
        }
    }

    fun showAlertBanner(
        activity: Activity,
        frameLayout: FrameLayout,
        shimmerFrameLayout: FrameLayout
    ) {
        if (ads.canShowBannerAlertScreen) {
            loadAndShowBanner(
                activity,
                frameLayout,
                shimmerFrameLayout,
                ads.bannerAdIdAlertScreen.ifBlank { ads.bannerAdIdHome }
            )
        } else {
            frameLayout.removeAllViews()
            frameLayout.visibility = View.GONE
            shimmerFrameLayout.visibility = View.GONE
        }
    }

    fun showBannerAfterCall(
        activity: Activity,
        frameLayout: FrameLayout,
        shimmerFrameLayout: FrameLayout
    ) {
        if (ads.canShowBanner) {
            loadAndShowBannerAfterCall(
                activity,
                frameLayout,
                shimmerFrameLayout,
                ads.bannerAdIdHome
            )
        } else {
            frameLayout.removeAllViews()
            frameLayout.visibility = View.GONE
            shimmerFrameLayout.visibility = View.GONE
        }
    }

    private fun loadAndShowBanner(
        activity: Activity,
        frameLayout: FrameLayout,
        shimmerFrameLayout: FrameLayout,
        adUnitID: String
    ) {
        App.runWhenMobileAdsReady {
            if (activity.isFinishing || activity.isDestroyed) return@runWhenMobileAdsReady
            frameLayout.post {
                if (activity.isFinishing || activity.isDestroyed) return@post
                attachAndLoadBanner(activity, frameLayout, shimmerFrameLayout, adUnitID)
            }
        }
    }

    private fun attachAndLoadBanner(
        activity: Activity,
        frameLayout: FrameLayout,
        shimmerFrameLayout: FrameLayout,
        adUnitID: String
    ) {
        if (!ads.isBannerAdEnabled || adUnitID.isBlank()) {
            frameLayout.removeAllViews()
            frameLayout.visibility = View.GONE
            shimmerFrameLayout.visibility = View.GONE
            return
        }
        Log.e("LoadBannerAdCheck", "load banner unit=$adUnitID")
        frameLayout.visibility = View.VISIBLE
        shimmerFrameLayout.visibility = View.VISIBLE

        val adView = AdView(activity)
        adView.adUnitId = adUnitID
        adView.setAdSize(AdSize.BANNER)
        adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                super.onAdLoaded()
                logAds(activity, "banner_loaded")
                frameLayout.visibility = View.VISIBLE
                shimmerFrameLayout.visibility = View.GONE
                logAds(activity, "banner_showed")
            }

            override fun onAdClicked() {
                super.onAdClicked()
                logAds(activity, "banner_clicked")
                showBanner(activity, frameLayout, shimmerFrameLayout)
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                super.onAdFailedToLoad(loadAdError)
                Log.e(
                    "LoadBannerAdCheck",
                    "showBanner -> failed $loadAdError"
                )
                logAds(activity, "banner_failed_to_load_" + loadAdError.code)
                frameLayout.removeAllViews()
                frameLayout.visibility = View.GONE
                shimmerFrameLayout.visibility = View.GONE
            }
        }
        frameLayout.removeAllViews()
        frameLayout.addView(adView)
        logAds(activity, "banner_req")
        adView.loadAd(AdRequest.Builder().build())
    }

    fun loadAndShowBannerAfterCall(
        activity: Activity,
        frameLayout: FrameLayout,
        shimmerFrameLayout: FrameLayout,
        adUnitID: String
    ) {
        Log.e("LoadBannerAdCheck", "bannerAdPreload==null")
        val adView = AdView(activity)
        adView.adUnitId = adUnitID
        adView.setAdSize(AdSize.MEDIUM_RECTANGLE)
        val adRequest = AdRequest.Builder().build()
        logAds(activity, "banner_req")
        adView.loadAd(adRequest)
        adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                super.onAdLoaded()
                logAds(activity, "banner_loaded")
                frameLayout.visibility = View.VISIBLE
                shimmerFrameLayout.visibility = View.GONE

                if (adView.parent != null) {
                    (adView.parent as ViewGroup).removeView(adView)
                }
                frameLayout.removeAllViews()
                logAds(activity, "banner_showed")
                frameLayout.addView(adView)
            }

            override fun onAdClicked() {
                super.onAdClicked()
                logAds(activity, "banner_clicked")
                showBanner(activity, frameLayout, shimmerFrameLayout)
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                super.onAdFailedToLoad(loadAdError)
                frameLayout.visibility = View.GONE
                shimmerFrameLayout.visibility = View.GONE
                Log.e("LoadBannerAdCheck", "showBanner -> failed -> ${loadAdError.message}")
                logAds(activity, "banner_failed_to_load_" + loadAdError.code)
            }
        }
    }

    private fun getNormalAdSize(activity: Activity, container: View? = null): AdSize {
        val displayMetrics = activity.resources.displayMetrics
        var adWidthPixels = container?.width ?: 0
        if (adWidthPixels <= 0) {
            adWidthPixels =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val windowMetrics: WindowMetrics = activity.windowManager.currentWindowMetrics
                    windowMetrics.bounds.width()
                } else {
                    displayMetrics.widthPixels
                }
        }
        if (adWidthPixels <= 0) {
            adWidthPixels = displayMetrics.widthPixels
        }
        val density = displayMetrics.density
        val adWidth = (adWidthPixels / density).toInt().coerceAtLeast(320)
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth)
    }

    fun getAdaptiveAdSize(context: Context): AdSize {
        val displayMetrics = context.resources.displayMetrics
        val density = displayMetrics.density
        val densityDpi = displayMetrics.densityDpi

        val adWidthDp = (displayMetrics.widthPixels / density) - (5 / (densityDpi / 160.0f))

        val adHeightDp =
            (context.resources.getDimension(R.dimen.adaptive_banner_caller_ad_height) / (densityDpi / 160.0f))

        return AdSize.getInlineAdaptiveBannerAdSize(adWidthDp.toInt(), adHeightDp.toInt())
    }

    companion object {
        var instance: BannerAdLoader? = null
            get() {
                if (field == null) {
                    field = BannerAdLoader()
                }
                return field
            }
            private set
    }
}
