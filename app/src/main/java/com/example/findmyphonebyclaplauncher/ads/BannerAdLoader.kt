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
    private val isLoadingMap = java.util.concurrent.ConcurrentHashMap<String, Boolean>()

    companion object {
        private const val TAG = "BannerAd"

        var instance: BannerAdLoader? = null
            get() {
                if (field == null) {
                    field = BannerAdLoader()
                }
                return field
            }
            private set
    }

    fun loadBannerAdPreload(activity: Activity) {
        if (ads.bannerAdPreload) {
            if (isLoadingMap["preload"] == true) {
                Log.d(TAG, "Banner preload already in progress. Ignoring duplicate request.")
                return
            }
            isLoadingMap["preload"] = true
            App.runWhenMobileAdsReady {
                bannerAdPreload = null
                val adView = AdView(activity)
                adView.adUnitId = ads.bannerAdIdHome
                adView.setAdSize(getNormalAdSize(activity))
                Log.d(TAG, "Preloading banner for home screen with Unit ID: ${ads.bannerAdIdHome}")
                adView.adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        super.onAdLoaded()
                        isLoadingMap["preload"] = false
                        Log.d(TAG, "Preloaded banner ad loaded successfully")
                        logAds(activity, "banner_loaded")
                        bannerAdPreload = adView
                    }

                    override fun onAdClicked() {
                        super.onAdClicked()
                        Log.d(TAG, "Preloaded banner ad clicked")
                        logAds(activity, "banner_clicked")
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        super.onAdFailedToLoad(loadAdError)
                        isLoadingMap["preload"] = false
                        Log.e(
                            TAG,
                            "Preloaded banner failed to load: code=${loadAdError.code}, message=${loadAdError.message}"
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
            Log.d(TAG, "Loading banner for Splash screen")
            loadAndShowBanner(
                activity,
                frameLayout,
                shimmerFrameLayout,
                ads.bannerAdIdSplash
            )
        } else {
            Log.d(TAG, "Splash banner disabled by Remote Config")
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
            Log.d(TAG, "Loading banner for Home screen")
            loadAndShowBanner(
                activity,
                frameLayout,
                shimmerFrameLayout,
                ads.bannerAdIdHome
            )
        } else {
            Log.d(TAG, "Home banner disabled by Remote Config")
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
            Log.d(TAG, "Loading banner for App Drawer")
            loadAndShowBanner(
                activity,
                frameLayout,
                shimmerFrameLayout,
                ads.bannerAdIdAppDrawer.ifBlank { ads.bannerAdIdHome }
            )
        } else {
            Log.d(TAG, "App Drawer banner disabled by Remote Config")
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
            Log.d(TAG, "Loading banner for Find Phone screen")
            loadAndShowBanner(
                activity,
                frameLayout,
                shimmerFrameLayout,
                ads.bannerAdIdFindPhone.ifBlank { ads.bannerAdIdHome }
            )
        } else {
            Log.d(TAG, "Find Phone banner disabled by Remote Config")
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
            Log.d(TAG, "Loading banner for Alert screen")
            loadAndShowBanner(
                activity,
                frameLayout,
                shimmerFrameLayout,
                ads.bannerAdIdAlertScreen.ifBlank { ads.bannerAdIdHome }
            )
        } else {
            Log.d(TAG, "Alert screen banner disabled by Remote Config")
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
            Log.d(TAG, "Loading banner for After Call screen")
            loadAndShowBannerAfterCall(
                activity,
                frameLayout,
                shimmerFrameLayout,
                ads.bannerAdIdHome
            )
        } else {
            Log.d(TAG, "After Call banner disabled by Remote Config")
            frameLayout.removeAllViews()
            frameLayout.visibility = View.GONE
            shimmerFrameLayout.visibility = View.GONE
        }
    }

    fun loadAndShowBanner(
        activity: Activity,
        frameLayout: FrameLayout,
        shimmerFrameLayout: FrameLayout,
        adUnitID: String
    ) {
        if (!ads.isBannerAdEnabled || adUnitID.isBlank()) {
            Log.d(TAG, "Banner ad disabled or empty unit ID ($adUnitID)")
            frameLayout.removeAllViews()
            frameLayout.visibility = View.GONE
            shimmerFrameLayout.visibility = View.GONE
            return
        }

        // If preloading is enabled and cached banner is ready, render immediately without shimmer
        if (ads.bannerAdPreload && bannerAdPreload != null) {
            Log.d(TAG, "Rendering preloaded banner ad instantly, hiding shimmer")
            val preloaded = bannerAdPreload!!
            if (preloaded.parent != null) {
                (preloaded.parent as ViewGroup).removeView(preloaded)
            }
            frameLayout.removeAllViews()
            frameLayout.addView(preloaded)
            frameLayout.visibility = View.VISIBLE
            shimmerFrameLayout.visibility = View.GONE
            logAds(activity, "banner_showed")
            bannerAdPreload = null
            loadBannerAdPreload(activity)
            return
        }

        // On-demand: Display shimmer placeholder immediately
        Log.d(TAG, "On-demand banner load: Showing shimmer placeholder with Unit ID: $adUnitID")
        shimmerFrameLayout.visibility = View.VISIBLE
        frameLayout.visibility = View.GONE

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

        val requestKey = "banner:$adUnitID"
        if (isLoadingMap[requestKey] == true) {
            Log.d(TAG, "Banner request for unit '$adUnitID' is already in progress. Ignoring duplicate request.")
            return
        }
        isLoadingMap[requestKey] = true

        shimmerFrameLayout.visibility = View.VISIBLE
        frameLayout.visibility = View.GONE

        val adView = AdView(activity)
        adView.adUnitId = adUnitID
        adView.setAdSize(AdSize.BANNER)
        adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                super.onAdLoaded()
                isLoadingMap[requestKey] = false
                Log.d(TAG, "Banner ad loaded successfully. Hiding shimmer and displaying ad view.")
                logAds(activity, "banner_loaded")
                shimmerFrameLayout.visibility = View.GONE
                frameLayout.visibility = View.VISIBLE
                logAds(activity, "banner_showed")
            }

            override fun onAdClicked() {
                super.onAdClicked()
                Log.d(TAG, "Banner ad clicked")
                logAds(activity, "banner_clicked")
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                super.onAdFailedToLoad(loadAdError)
                isLoadingMap[requestKey] = false
                Log.e(TAG, "Banner failed to load: ${loadAdError.message} (code ${loadAdError.code})")
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
        Log.d(TAG, "loadAndShowBannerAfterCall with Unit ID: $adUnitID")
        val adView = AdView(activity)
        adView.adUnitId = adUnitID
        adView.setAdSize(AdSize.MEDIUM_RECTANGLE)
        val adRequest = AdRequest.Builder().build()
        logAds(activity, "banner_req")
        adView.loadAd(adRequest)
        adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                super.onAdLoaded()
                Log.d(TAG, "After Call banner loaded successfully")
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
                Log.d(TAG, "After Call banner clicked")
                logAds(activity, "banner_clicked")
                showBanner(activity, frameLayout, shimmerFrameLayout)
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                super.onAdFailedToLoad(loadAdError)
                frameLayout.visibility = View.GONE
                shimmerFrameLayout.visibility = View.GONE
                Log.e(TAG, "After Call banner failed to load: ${loadAdError.message} (code ${loadAdError.code})")
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
}
