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
import com.example.findmyphonebyclaplauncher.ads.config.AdsConfig
import com.example.findmyphonebyclaplauncher.ads.config.AdsConfigManager
import com.example.findmyphonebyclaplauncher.analytics.AnalyticsHelper.logAds
import com.example.findmyphonebyclaplauncher.util.NetworkUtil
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnPaidEventListener


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

    private fun startShimmerAnimation(view: View?) {
        when (view) {
            is ShimmerFrameLayout -> {
                view.visibility = View.VISIBLE
                view.startShimmer()
            }
            is ViewGroup -> {
                view.visibility = View.VISIBLE
                val shimmer = view.findViewById<ShimmerFrameLayout>(R.id.bannerAdShimmerFrameLayout)
                    ?: (0 until view.childCount).mapNotNull { view.getChildAt(it) as? ShimmerFrameLayout }.firstOrNull()
                shimmer?.startShimmer()
            }
            else -> {
                view?.visibility = View.VISIBLE
            }
        }
    }

    private fun stopShimmerAnimation(view: View?) {
        when (view) {
            is ShimmerFrameLayout -> {
                view.stopShimmer()
                view.visibility = View.GONE
            }
            is ViewGroup -> {
                val shimmer = view.findViewById<ShimmerFrameLayout>(R.id.bannerAdShimmerFrameLayout)
                    ?: (0 until view.childCount).mapNotNull { view.getChildAt(it) as? ShimmerFrameLayout }.firstOrNull()
                shimmer?.stopShimmer()
                view.visibility = View.GONE
            }
            else -> {
                view?.visibility = View.GONE
            }
        }
    }

    fun hideBannerContainer(
        frameLayout: FrameLayout,
        shimmerFrameLayout: View,
        rootContainer: View? = null
    ) {
        stopShimmerAnimation(shimmerFrameLayout)
        shimmerFrameLayout.visibility = View.GONE
        frameLayout.removeAllViews()
        frameLayout.visibility = View.GONE
        rootContainer?.visibility = View.GONE

        // Also check if immediate parent or grand-parent is llBannerRoot
        val parent = frameLayout.parent as? View
        if (parent?.id == R.id.llBannerRoot) {
            parent.visibility = View.GONE
        }
    }

    fun loadBannerAdPreload(activity: Activity) {
        if (!NetworkUtil.isNetworkAvailable(activity)) {
            Log.d(TAG, "loadBannerAdPreload: Offline mode detected. Suppressing banner preload.")
            return
        }

        if (ads.bannerAdPreload && ads.canShowBannerHome) {
            if (isLoadingMap["preload"] == true) {
                Log.d(TAG, "Banner preload already in progress. Ignoring duplicate request.")
                return
            }
            isLoadingMap["preload"] = true
            App.runWhenMobileAdsReady {
                if (activity.isFinishing || activity.isDestroyed || !NetworkUtil.isNetworkAvailable(activity)) {
                    isLoadingMap["preload"] = false
                    return@runWhenMobileAdsReady
                }
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

                        adView.setOnPaidEventListener(object : OnPaidEventListener {
                            override fun onPaidEvent(adValue: AdValue) {
                                val valueMicros = adValue.getValueMicros()

                                val revenue = valueMicros / 1000000.0

                                val currency = adValue.getCurrencyCode()

                                val precision = adValue.getPrecisionType()

                                Log.e(TAG, "onPaidEvent: revenue = $revenue, currency = $currency, precision = $precision", )

                                App.getInstance().sendRevenueToAnalytics(revenue, currency, precision)
                            }
                        })
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
        shimmerFrameLayout: FrameLayout,
        rootContainer: View? = null
    ) {
        if (!NetworkUtil.isNetworkAvailable(activity)) {
            Log.d(TAG, "Splash banner: Offline mode. Hiding view completely.")
            hideBannerContainer(frameLayout, shimmerFrameLayout, rootContainer)
            return
        }

        if (ads.canShowBannerSplash) {
            Log.d(TAG, "Loading banner for Splash screen")
            loadAndShowBanner(
                activity,
                frameLayout,
                shimmerFrameLayout,
                ads.bannerAdIdSplash,
                rootContainer
            )
        } else {
            Log.d(TAG, "Splash banner disabled by Remote Config")
            hideBannerContainer(frameLayout, shimmerFrameLayout, rootContainer)
        }
    }

    fun showHomeBanner(
        activity: Activity,
        frameLayout: FrameLayout,
        shimmerFrameLayout: FrameLayout,
        rootContainer: View? = null
    ) {
        if (!NetworkUtil.isNetworkAvailable(activity)) {
            Log.d(TAG, "Home banner: Offline mode. Hiding view completely.")
            hideBannerContainer(frameLayout, shimmerFrameLayout, rootContainer)
            return
        }

        if (ads.canShowBannerHome) {
            Log.d(TAG, "Loading banner for Home screen")
            loadAndShowBanner(
                activity,
                frameLayout,
                shimmerFrameLayout,
                ads.bannerAdIdHome,
                rootContainer
            )
        } else {
            Log.d(TAG, "Home banner disabled by Remote Config")
            hideBannerContainer(frameLayout, shimmerFrameLayout, rootContainer)
        }
    }

    fun showBanner(
        activity: Activity,
        frameLayout: FrameLayout,
        shimmerFrameLayout: FrameLayout,
        rootContainer: View? = null
    ) {
        showHomeBanner(activity, frameLayout, shimmerFrameLayout, rootContainer)
    }

    fun showDrawerBanner(
        activity: Activity,
        frameLayout: FrameLayout,
        shimmerFrameLayout: FrameLayout,
        rootContainer: View? = null
    ) {
        if (!NetworkUtil.isNetworkAvailable(activity)) {
            Log.d(TAG, "App Drawer banner: Offline mode. Hiding view completely.")
            hideBannerContainer(frameLayout, shimmerFrameLayout, rootContainer)
            return
        }

        if (ads.canShowBannerAppDrawer) {
            Log.d(TAG, "Loading banner for App Drawer")
            loadAndShowBanner(
                activity,
                frameLayout,
                shimmerFrameLayout,
                ads.bannerAdIdAppDrawer.ifBlank { ads.bannerAdIdHome },
                rootContainer
            )
        } else {
            Log.d(TAG, "App Drawer banner disabled by Remote Config")
            hideBannerContainer(frameLayout, shimmerFrameLayout, rootContainer)
        }
    }

    fun showFindPhoneBanner(
        activity: Activity,
        frameLayout: FrameLayout,
        shimmerFrameLayout: FrameLayout,
        rootContainer: View? = null
    ) {
        if (!NetworkUtil.isNetworkAvailable(activity)) {
            Log.d(TAG, "Find Phone banner: Offline mode. Hiding view completely.")
            hideBannerContainer(frameLayout, shimmerFrameLayout, rootContainer)
            return
        }

        if (ads.canShowBannerFindPhone) {
            Log.d(TAG, "Loading banner for Find Phone screen")
            loadAndShowBanner(
                activity,
                frameLayout,
                shimmerFrameLayout,
                ads.bannerAdIdFindPhone.ifBlank { ads.bannerAdIdHome },
                rootContainer
            )
        } else {
            Log.d(TAG, "Find Phone banner disabled by Remote Config")
            hideBannerContainer(frameLayout, shimmerFrameLayout, rootContainer)
        }
    }

    fun showAlertBanner(
        activity: Activity,
        frameLayout: FrameLayout,
        shimmerFrameLayout: FrameLayout,
        rootContainer: View? = null
    ) {
        if (!NetworkUtil.isNetworkAvailable(activity)) {
            Log.d(TAG, "Alert screen banner: Offline mode. Hiding view completely.")
            hideBannerContainer(frameLayout, shimmerFrameLayout, rootContainer)
            return
        }

        if (ads.canShowBannerAlertScreen) {
            Log.d(TAG, "Loading banner for Alert screen")
            loadAndShowBanner(
                activity,
                frameLayout,
                shimmerFrameLayout,
                ads.bannerAdIdAlertScreen.ifBlank { ads.bannerAdIdHome },
                rootContainer
            )
        } else {
            Log.d(TAG, "Alert screen banner disabled by Remote Config")
            hideBannerContainer(frameLayout, shimmerFrameLayout, rootContainer)
        }
    }

    fun showBannerAfterCall(
        activity: Activity,
        frameLayout: FrameLayout,
        shimmerFrameLayout: View,
        rootContainer: View? = null
    ) {
        if (!NetworkUtil.isNetworkAvailable(activity)) {
            Log.d(TAG, "After Call banner: Offline mode. Hiding view completely.")
            hideBannerContainer(frameLayout, shimmerFrameLayout, rootContainer)
            return
        }

        if (ads.canShowBannerAfterCall) {
            Log.d(TAG, "Loading Medium Rectangle banner for After Call screen")
            val adUnitId = ads.bannerAdIdAfterCall.ifBlank { ads.bannerAdIdHome.ifBlank { AdsConfig.DEFAULT_BANNER_ID } }
            loadAndShowBannerAfterCall(
                activity,
                frameLayout,
                shimmerFrameLayout,
                adUnitId,
                rootContainer
            )
        } else {
            Log.d(TAG, "After Call banner disabled by Remote Config")
            hideBannerContainer(frameLayout, shimmerFrameLayout, rootContainer)
        }
    }

    fun showLanguageRectBanner(
        activity: Activity,
        frameLayout: FrameLayout,
        shimmerFrameLayout: View,
        rootContainer: View? = null
    ) {
        if (!NetworkUtil.isNetworkAvailable(activity)) {
            Log.d(TAG, "Language rect banner: Offline mode. Hiding view completely.")
            hideBannerContainer(frameLayout, shimmerFrameLayout, rootContainer)
            return
        }

        if (ads.canShowBannerLanguageRect) {
            Log.d(TAG, "Loading Medium Rectangle banner for Language screen")
            val adUnitId = ads.bannerAdIdLanguageRect.ifBlank { ads.bannerAdIdHome.ifBlank { AdsConfig.DEFAULT_BANNER_ID } }
            loadAndShowLanguageRectBanner(
                activity,
                frameLayout,
                shimmerFrameLayout,
                adUnitId,
                rootContainer
            )
        } else {
            Log.d(TAG, "Language rect banner disabled by Remote Config")
            hideBannerContainer(frameLayout, shimmerFrameLayout, rootContainer)
        }
    }

    fun showOnboardingBanner(
        activity: Activity,
        frameLayout: FrameLayout,
        shimmerFrameLayout: FrameLayout,
        rootContainer: View? = null
    ) {
        if (!NetworkUtil.isNetworkAvailable(activity)) {
            Log.d(TAG, "Onboarding banner: Offline mode. Hiding view completely.")
            hideBannerContainer(frameLayout, shimmerFrameLayout, rootContainer)
            return
        }

        if (ads.canShowBannerOnboarding) {
            Log.d(TAG, "Loading banner for Onboarding screen")
            loadAndShowBanner(
                activity,
                frameLayout,
                shimmerFrameLayout,
                ads.bannerAdIdOnboarding.ifBlank { ads.bannerAdIdHome },
                rootContainer
            )
        } else {
            Log.d(TAG, "Onboarding banner disabled by Remote Config")
            hideBannerContainer(frameLayout, shimmerFrameLayout, rootContainer)
        }
    }

    fun loadAndShowBanner(
        activity: Activity,
        frameLayout: FrameLayout,
        shimmerFrameLayout: FrameLayout,
        adUnitID: String,
        rootContainer: View? = null
    ) {
        if (!NetworkUtil.isNetworkAvailable(activity)) {
            Log.d(TAG, "Offline mode detected: Suppressing banner request for unit '$adUnitID'")
            hideBannerContainer(frameLayout, shimmerFrameLayout, rootContainer)
            return
        }

        if (!ads.isBannerAdEnabled || adUnitID.isBlank()) {
            Log.d(TAG, "Banner ad disabled or empty unit ID ($adUnitID)")
            hideBannerContainer(frameLayout, shimmerFrameLayout, rootContainer)
            return
        }

        // If preloading is enabled and cached banner is ready, render immediately without shimmer
        if (ads.bannerAdPreload && bannerAdPreload != null) {
            Log.d(TAG, "Rendering preloaded banner ad instantly, hiding shimmer")
            val preloaded = bannerAdPreload!!
            if (preloaded.parent != null) {
                (preloaded.parent as ViewGroup).removeView(preloaded)
            }
            stopShimmerAnimation(shimmerFrameLayout)
            shimmerFrameLayout.visibility = View.GONE
            frameLayout.removeAllViews()
            frameLayout.addView(preloaded)
            frameLayout.visibility = View.VISIBLE
            rootContainer?.visibility = View.VISIBLE
            (frameLayout.parent as? View)?.let { if (it.id == R.id.llBannerRoot) it.visibility = View.VISIBLE }
            logAds(activity, "banner_showed")
            bannerAdPreload = null
            loadBannerAdPreload(activity)
            return
        }

        // On-demand: Display shimmer placeholder only when actively requesting
        Log.d(TAG, "On-demand banner load: Showing shimmer placeholder with Unit ID: $adUnitID")
        rootContainer?.visibility = View.VISIBLE
        (frameLayout.parent as? View)?.let { if (it.id == R.id.llBannerRoot) it.visibility = View.VISIBLE }
        startShimmerAnimation(shimmerFrameLayout)
        frameLayout.visibility = View.GONE

        App.runWhenMobileAdsReady {
            if (activity.isFinishing || activity.isDestroyed || !NetworkUtil.isNetworkAvailable(activity)) {
                hideBannerContainer(frameLayout, shimmerFrameLayout, rootContainer)
                return@runWhenMobileAdsReady
            }
            frameLayout.post {
                if (activity.isFinishing || activity.isDestroyed || !NetworkUtil.isNetworkAvailable(activity)) {
                    hideBannerContainer(frameLayout, shimmerFrameLayout, rootContainer)
                    return@post
                }
                attachAndLoadBanner(activity, frameLayout, shimmerFrameLayout, adUnitID, rootContainer)
            }
        }
    }

    private fun attachAndLoadBanner(
        activity: Activity,
        frameLayout: FrameLayout,
        shimmerFrameLayout: FrameLayout,
        adUnitID: String,
        rootContainer: View? = null
    ) {
        if (!NetworkUtil.isNetworkAvailable(activity) || !ads.isBannerAdEnabled || adUnitID.isBlank()) {
            hideBannerContainer(frameLayout, shimmerFrameLayout, rootContainer)
            return
        }

        val requestKey = "banner:$adUnitID"
        if (isLoadingMap[requestKey] == true) {
            Log.d(TAG, "Banner request for unit '$adUnitID' is already in progress. Ignoring duplicate request.")
            return
        }
        isLoadingMap[requestKey] = true

        rootContainer?.visibility = View.VISIBLE
        (frameLayout.parent as? View)?.let { if (it.id == R.id.llBannerRoot) it.visibility = View.VISIBLE }
        startShimmerAnimation(shimmerFrameLayout)
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
                stopShimmerAnimation(shimmerFrameLayout)
                shimmerFrameLayout.visibility = View.GONE
                frameLayout.visibility = View.VISIBLE
                rootContainer?.visibility = View.VISIBLE
                (frameLayout.parent as? View)?.let { if (it.id == R.id.llBannerRoot) it.visibility = View.VISIBLE }
                logAds(activity, "banner_showed")

                adView.setOnPaidEventListener(object : OnPaidEventListener {
                    override fun onPaidEvent(adValue: AdValue) {
                        val valueMicros = adValue.getValueMicros()

                        val revenue = valueMicros / 1000000.0

                        val currency = adValue.getCurrencyCode()

                        val precision = adValue.getPrecisionType()


                        Log.e(TAG, "onPaidEvent: revenue = $revenue, currency = $currency, precision = $precision", )
                        App.getInstance().sendRevenueToAnalytics(revenue, currency, precision)
                    }
                })
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
                hideBannerContainer(frameLayout, shimmerFrameLayout, rootContainer)
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
        shimmerFrameLayout: View,
        adUnitID: String,
        rootContainer: View? = null
    ) {
        if (!NetworkUtil.isNetworkAvailable(activity)) {
            Log.d(TAG, "loadAndShowBannerAfterCall: Offline mode. Suppressing request.")
            hideBannerContainer(frameLayout, shimmerFrameLayout, rootContainer)
            return
        }

        val safeAdUnitId = if (adUnitID.contains("2247696110")) {
            AdsConfig.DEFAULT_BANNER_ID
        } else {
            adUnitID.ifBlank { AdsConfig.DEFAULT_BANNER_ID }
        }

        val requestKey = "banner_after_call:$safeAdUnitId"
        if (isLoadingMap[requestKey] == true) {
            Log.d(TAG, "After Call banner request is already in progress. Ignoring duplicate.")
            return
        }
        isLoadingMap[requestKey] = true

        Log.d(TAG, "loadAndShowBannerAfterCall with Unit ID: $safeAdUnitId")
        rootContainer?.visibility = View.VISIBLE
        (frameLayout.parent as? View)?.let { if (it.id == R.id.llBannerRoot) it.visibility = View.VISIBLE }
        startShimmerAnimation(shimmerFrameLayout)
        frameLayout.visibility = View.GONE

        val adView = AdView(activity)
        adView.adUnitId = safeAdUnitId
        adView.setAdSize(AdSize.MEDIUM_RECTANGLE)
        val adRequest = AdRequest.Builder().build()
        logAds(activity, "banner_req")
        adView.loadAd(adRequest)
        adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                super.onAdLoaded()
                isLoadingMap[requestKey] = false
                Log.d(TAG, "After Call banner loaded successfully")
                logAds(activity, "banner_loaded")
                stopShimmerAnimation(shimmerFrameLayout)
                shimmerFrameLayout.visibility = View.GONE
                frameLayout.visibility = View.VISIBLE
                rootContainer?.visibility = View.VISIBLE
                (frameLayout.parent as? View)?.let { if (it.id == R.id.llBannerRoot) it.visibility = View.VISIBLE }

                if (adView.parent != null) {
                    (adView.parent as ViewGroup).removeView(adView)
                }
                frameLayout.removeAllViews()
                logAds(activity, "banner_showed")
                frameLayout.addView(adView)

                adView.setOnPaidEventListener(object : OnPaidEventListener {
                    override fun onPaidEvent(adValue: AdValue) {
                        val valueMicros = adValue.getValueMicros()

                        val revenue = valueMicros / 1000000.0

                        val currency = adValue.getCurrencyCode()

                        val precision = adValue.getPrecisionType()

                        Log.e(TAG, "onPaidEvent: revenue = $revenue, currency = $currency, precision = $precision", )

                        App.getInstance().sendRevenueToAnalytics(revenue, currency, precision)
                    }
                })
            }

            override fun onAdClicked() {
                super.onAdClicked()
                Log.d(TAG, "After Call banner clicked")
                logAds(activity, "banner_clicked")
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                super.onAdFailedToLoad(loadAdError)
                isLoadingMap[requestKey] = false
                Log.e(TAG, "After Call banner failed to load: ${loadAdError.message} (code ${loadAdError.code})")
                logAds(activity, "banner_failed_to_load_" + loadAdError.code)
                hideBannerContainer(frameLayout, shimmerFrameLayout, rootContainer)
            }
        }
    }

    fun loadAndShowLanguageRectBanner(
        activity: Activity,
        frameLayout: FrameLayout,
        shimmerFrameLayout: View,
        adUnitID: String,
        rootContainer: View? = null
    ) {
        if (!NetworkUtil.isNetworkAvailable(activity)) {
            Log.d(TAG, "loadAndShowLanguageRectBanner: Offline mode. Suppressing request.")
            hideBannerContainer(frameLayout, shimmerFrameLayout, rootContainer)
            return
        }

        val safeAdUnitId = if (adUnitID.contains("2247696110")) {
            AdsConfig.DEFAULT_BANNER_ID
        } else {
            adUnitID.ifBlank { AdsConfig.DEFAULT_BANNER_ID }
        }

        val requestKey = "banner_lang_rect:$safeAdUnitId"
        if (isLoadingMap[requestKey] == true) {
            Log.d(TAG, "Language rect banner request is already in progress. Ignoring duplicate.")
            return
        }
        isLoadingMap[requestKey] = true

        Log.d(TAG, "loadAndShowLanguageRectBanner requested with Unit ID: $safeAdUnitId (Format: AdSize.MEDIUM_RECTANGLE 300x250)")
        rootContainer?.visibility = View.VISIBLE
        (frameLayout.parent as? View)?.let { if (it.id == R.id.llBannerRoot) it.visibility = View.VISIBLE }
        startShimmerAnimation(shimmerFrameLayout)
        frameLayout.visibility = View.GONE

        App.runWhenMobileAdsReady {
            if (activity.isFinishing || activity.isDestroyed || !NetworkUtil.isNetworkAvailable(activity)) {
                isLoadingMap[requestKey] = false
                hideBannerContainer(frameLayout, shimmerFrameLayout, rootContainer)
                return@runWhenMobileAdsReady
            }
            frameLayout.post {
                if (activity.isFinishing || activity.isDestroyed || !NetworkUtil.isNetworkAvailable(activity)) {
                    isLoadingMap[requestKey] = false
                    hideBannerContainer(frameLayout, shimmerFrameLayout, rootContainer)
                    return@post
                }

                val adView = AdView(activity)
                adView.adUnitId = safeAdUnitId
                adView.setAdSize(AdSize.MEDIUM_RECTANGLE)
                adView.adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        super.onAdLoaded()
                        isLoadingMap[requestKey] = false
                        Log.d(TAG, "Language rect banner loaded successfully for Unit ID: $safeAdUnitId")
                        logAds(activity, "banner_loaded")
                        stopShimmerAnimation(shimmerFrameLayout)
                        shimmerFrameLayout.visibility = View.GONE
                        frameLayout.visibility = View.VISIBLE
                        rootContainer?.visibility = View.VISIBLE
                        (frameLayout.parent as? View)?.let { if (it.id == R.id.llBannerRoot) it.visibility = View.VISIBLE }

                        if (adView.parent != null) {
                            (adView.parent as ViewGroup).removeView(adView)
                        }
                        frameLayout.removeAllViews()
                        logAds(activity, "banner_showed")
                        frameLayout.addView(adView)

                        adView.setOnPaidEventListener(object : OnPaidEventListener {
                            override fun onPaidEvent(adValue: AdValue) {
                                val valueMicros = adValue.getValueMicros()

                                val revenue = valueMicros / 1000000.0

                                val currency = adValue.getCurrencyCode()

                                val precision = adValue.getPrecisionType()

                                Log.e(TAG, "onPaidEvent: revenue = $revenue, currency = $currency, precision = $precision", )


                                App.getInstance().sendRevenueToAnalytics(revenue, currency, precision)
                            }
                        })
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        super.onAdFailedToLoad(loadAdError)
                        isLoadingMap[requestKey] = false
                        Log.e(TAG, "Language rect banner failed to load for Unit ID '$safeAdUnitId': code=${loadAdError.code}, message=${loadAdError.message}, domain=${loadAdError.domain}")
                        logAds(activity, "banner_failed_to_load_" + loadAdError.code)
                        hideBannerContainer(frameLayout, shimmerFrameLayout, rootContainer)
                    }

                    override fun onAdImpression() {
                        super.onAdImpression()
                        Log.d(TAG, "Language rect banner impression recorded")
                        logAds(activity, "banner_impression")
                    }

                    override fun onAdClicked() {
                        super.onAdClicked()
                        Log.d(TAG, "Language rect banner clicked")
                        logAds(activity, "banner_clicked")
                    }

                    override fun onAdOpened() {
                        super.onAdOpened()
                        Log.d(TAG, "Language rect banner opened")
                    }

                    override fun onAdClosed() {
                        super.onAdClosed()
                        Log.d(TAG, "Language rect banner closed")
                    }
                }
                val adRequest = AdRequest.Builder().build()
                logAds(activity, "banner_req")
                adView.loadAd(adRequest)
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
