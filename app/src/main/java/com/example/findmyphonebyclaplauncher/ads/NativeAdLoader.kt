package com.example.findmyphonebyclaplauncher.ads

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.example.findmyphonebyclaplauncher.App
import com.example.findmyphonebyclaplauncher.R
import com.example.findmyphonebyclaplauncher.ads.config.AdsConfigManager
import com.example.findmyphonebyclaplauncher.analytics.AnalyticsHelper.logAds
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.VideoController
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView

class NativeAdLoader {
    private val ads get() = AdsConfigManager.config
    private var nativeAdPreload: NativeAd? = null
    private var nativeAdPreloadLanguage: NativeAd? = null
    private var nativeAdPreloadAfterCall: NativeAd? = null

    private val isLoadingMap = java.util.concurrent.ConcurrentHashMap<String, Boolean>()
    private val pendingCallbacks = java.util.concurrent.ConcurrentHashMap<String, MutableList<Pair<(NativeAd) -> Unit, () -> Unit>>>()

    fun showNativeLarge(
        activity: Activity, ltNativeAds: FrameLayout, ltNativeShimmerAds: FrameLayout
    ) {
        if (ads.canShowNative) {
            Log.d(TAG, "Requesting Large Native Ad")
            showNative(activity, ltNativeAds, ltNativeShimmerAds, "Large")
        } else {
            Log.d(TAG, "Native ad disabled by Remote Config")
            ltNativeAds.removeAllViews()
            ltNativeAds.visibility = View.GONE
            ltNativeShimmerAds.visibility = View.GONE
        }
    }

    fun showNativeLargeAfterCall(
        activity: Activity,
        ltNativeAds: FrameLayout,
        ltNativeShimmerAds: FrameLayout,
        nativeAdCardView: CardView
    ) {
        if (ads.canShowNative) {
            Log.d(TAG, "Requesting After Call Native Ad")
            if (ads.nativeAdPreload && nativeAdPreloadAfterCall != null) {
                Log.d(TAG, "Inflating preloaded After Call Native Ad")
                instance!!.inflateGoogleNativeAd(
                    activity,
                    ltNativeAds,
                    ltNativeShimmerAds,
                    nativeAdPreloadAfterCall!!,
                    "Large",
                    false,
                    "AfterCall"
                )
                nativeAdPreloadAfterCall = null
                loadNativeAdPreload(activity)
            } else {
                Log.d(TAG, "Loading on-demand After Call Native Ad (Unit ID: ${ads.nativeAdIdAfterCall})")
                loadAndShowNativeAd(
                    activity,
                    ltNativeAds,
                    ltNativeShimmerAds,
                    "Large",
                    ads.nativeAdIdAfterCall,
                    "AfterCall",
                    nativeAdCardView
                )
            }
        } else {
            Log.d(TAG, "After Call Native Ad disabled by Remote Config")
            ltNativeAds.removeAllViews()
            ltNativeAds.visibility = View.GONE
            ltNativeShimmerAds.visibility = View.GONE
            nativeAdCardView.visibility = View.GONE
        }
    }

    fun showNativeLargeLanguage(
        activity: Activity,
        ltNativeAds: FrameLayout,
        ltNativeShimmerAds: FrameLayout,
        nativeAdCardView: CardView
    ) {
        if (ads.canShowNative) {
            Log.d(TAG, "Requesting Language Native Ad")
            if (ads.nativeAdPreload && nativeAdPreloadLanguage != null) {
                Log.d(TAG, "Inflating preloaded Language Native Ad")
                instance!!.inflateGoogleNativeAd(
                    activity,
                    ltNativeAds,
                    ltNativeShimmerAds,
                    nativeAdPreloadLanguage!!,
                    "Large",
                    false,
                    "Language"
                )
                nativeAdPreloadLanguage = null
                loadNativeAdPreload(activity)
            } else {
                Log.d(TAG, "Loading on-demand Language Native Ad (Unit ID: ${ads.nativeAdIdLanguage})")
                loadAndShowNativeAd(
                    activity,
                    ltNativeAds,
                    ltNativeShimmerAds,
                    "Large",
                    ads.nativeAdIdLanguage,
                    "Language",
                    nativeAdCardView
                )
            }
        } else {
            Log.d(TAG, "Language Native Ad disabled by Remote Config")
            ltNativeAds.removeAllViews()
            ltNativeAds.visibility = View.GONE
            ltNativeShimmerAds.visibility = View.GONE
            nativeAdCardView.visibility = View.GONE
        }
    }

    fun loadNativeAd(
        activity: Activity,
        adUnitId: String,
        type: String,
        onLoaded: (NativeAd) -> Unit,
        onFailed: () -> Unit = {}
    ) {
        if (!ads.canShowNative || adUnitId.isBlank()) {
            Log.d(TAG, "loadNativeAd: skipped, canShowNative is false or adUnitId is blank")
            onFailed()
            return
        }

        val requestKey = "$type:$adUnitId"
        if (isLoadingMap[requestKey] == true) {
            Log.d(TAG, "Ad request for placement '$type' is already in progress. Attaching callback to active request.")
            pendingCallbacks.computeIfAbsent(requestKey) { mutableListOf() }.add(Pair(onLoaded, onFailed))
            return
        }

        isLoadingMap[requestKey] = true
        pendingCallbacks.computeIfAbsent(requestKey) { mutableListOf() }.add(Pair(onLoaded, onFailed))

        Log.d(TAG, "loadNativeAd requested for type: $type with Unit ID: $adUnitId")
        val videoOptions = VideoOptions.Builder().setStartMuted(true).build()
        val adOptions = NativeAdOptions.Builder().setVideoOptions(videoOptions).build()
        val adLoaderNative = AdLoader.Builder(activity.applicationContext, adUnitId)
            .forNativeAd { nativeAd ->
                Log.d(TAG, "Native Ad ($type) loaded successfully")
                logAds(activity, "native_loaded_$type")
                resetFailedCountNative()
                isLoadingMap[requestKey] = false
                val callbacks = pendingCallbacks.remove(requestKey) ?: emptyList()
                if (activity.isDestroyed || activity.isFinishing) {
                    nativeAd.destroy()
                    return@forNativeAd
                }
                callbacks.forEach { it.first.invoke(nativeAd) }
            }.withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG, "Native Ad ($type) failed to load: ${adError.message} (code ${adError.code})")
                    increaseFailedCountNative()
                    logAds(activity, "native_failed_${type}_" + adError.code)
                    isLoadingMap[requestKey] = false
                    val callbacks = pendingCallbacks.remove(requestKey) ?: emptyList()
                    callbacks.forEach { it.second.invoke() }
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    Log.d(TAG, "Native Ad ($type) clicked")
                    logAds(activity, "native_clicked_$type")
                }
            }).withNativeAdOptions(adOptions).build()
        val adRequest = AdRequest.Builder().build()
        logAds(activity, "native_req_$type")
        App.runWhenMobileAdsReady {
            if (activity.isFinishing || activity.isDestroyed) {
                isLoadingMap[requestKey] = false
                pendingCallbacks.remove(requestKey)
                return@runWhenMobileAdsReady
            }
            adLoaderNative.loadAd(adRequest)
        }
    }

    fun bindNativeLarge(
        activity: Activity,
        ltNativeAds: FrameLayout,
        ltNativeShimmerAds: FrameLayout,
        nativeAd: NativeAd,
        type: String = "GoogleSearch"
    ) {
        Log.d(TAG, "Binding Native Ad for type: $type")
        inflateGoogleNativeAd(
            activity,
            ltNativeAds,
            ltNativeShimmerAds,
            nativeAd,
            "Large",
            true,
            type
        )
    }

    fun showNativeSmall(
        activity: Activity,
        ltNativeAds: FrameLayout,
        ltNativeShimmerAds: FrameLayout,
        nativeAdCardView: CardView? = null
    ) {
        if (ads.canShowNative) {
            Log.d(TAG, "Requesting Small Native Ad")
            showNative(activity, ltNativeAds, ltNativeShimmerAds, "Small")
        } else {
            Log.d(TAG, "Small Native Ad disabled by Remote Config")
            ltNativeAds.removeAllViews()
            ltNativeAds.visibility = View.GONE
            ltNativeShimmerAds.visibility = View.GONE
            nativeAdCardView?.visibility = View.GONE
        }
    }

    private fun showNative(
        activity: Activity,
        ltNativeAds: FrameLayout,
        ltNativeShimmerAds: FrameLayout,
        adType: String
    ) {
        if (ads.canShowNative) {
            if (ads.nativeAdPreload && nativeAdPreload != null) {
                Log.d(TAG, "Inflating preloaded $adType Native Ad (Default)")
                instance!!.inflateGoogleNativeAd(
                    activity,
                    ltNativeAds,
                    ltNativeShimmerAds,
                    nativeAdPreload!!,
                    adType,
                    false,
                    "Default"
                )
                nativeAdPreload = null
                loadNativeAdPreload(activity)
            } else {
                Log.d(TAG, "Loading on-demand $adType Native Ad (Default) with Unit ID: ${ads.nativeAdIdDashboard}")
                loadAndShowNativeAd(
                    activity,
                    ltNativeAds,
                    ltNativeShimmerAds,
                    adType,
                    ads.nativeAdIdDashboard,
                    "Default"
                )
            }
        } else {
            ltNativeAds.removeAllViews()
            ltNativeAds.visibility = View.GONE
            ltNativeShimmerAds.visibility = View.GONE
        }
    }

    fun loadNativeAdPreload(activity: Activity) {
        if (ads.nativeAdPreload &&
            ads.canShowNative
        ) {
            App.runWhenMobileAdsReady {
                if (activity.isFinishing || activity.isDestroyed) return@runWhenMobileAdsReady
                preloadNativeAds(activity)
            }
        }
    }

    private fun preloadNativeAds(activity: Activity) {
        val videoOptions = VideoOptions.Builder().setStartMuted(true).build()
        val adOptions = NativeAdOptions.Builder().setVideoOptions(videoOptions).build()
        val adRequest = AdRequest.Builder().build()

        if (nativeAdPreload == null && isLoadingMap["preload_default"] != true) {
            isLoadingMap["preload_default"] = true
            Log.d(TAG, "Preloading Dashboard Native Ad with Unit ID: ${ads.nativeAdIdDashboard}")
            AdLoader.Builder(activity.applicationContext, ads.nativeAdIdDashboard)
                .forNativeAd { nativeAd ->
                    isLoadingMap["preload_default"] = false
                    Log.d(TAG, "Preloaded Dashboard Native Ad loaded successfully")
                    logAds(activity, "native_loaded_default")
                    nativeAdPreload = nativeAd
                }.withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        isLoadingMap["preload_default"] = false
                        Log.e(TAG, "Preload Dashboard Native Ad failed: ${adError.message} (code ${adError.code})")
                        logAds(activity, "native_failed_default_" + adError.code)
                        increaseFailedCountNative()
                    }
                }).withNativeAdOptions(adOptions).build().loadAd(adRequest)
        }

        if (nativeAdPreloadLanguage == null && isLoadingMap["preload_language"] != true) {
            isLoadingMap["preload_language"] = true
            Log.d(TAG, "Preloading Language Native Ad with Unit ID: ${ads.nativeAdIdLanguage}")
            AdLoader.Builder(activity.applicationContext, ads.nativeAdIdLanguage)
                .forNativeAd { nativeAd ->
                    isLoadingMap["preload_language"] = false
                    Log.d(TAG, "Preloaded Language Native Ad loaded successfully")
                    logAds(activity, "native_loaded_language")
                    nativeAdPreloadLanguage = nativeAd
                }.withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        isLoadingMap["preload_language"] = false
                        Log.e(TAG, "Preload Language Native Ad failed: ${adError.message} (code ${adError.code})")
                        logAds(activity, "native_failed_language_" + adError.code)
                    }
                }).withNativeAdOptions(adOptions).build().loadAd(adRequest)
        }

        if (nativeAdPreloadAfterCall == null && isLoadingMap["preload_aftercall"] != true) {
            isLoadingMap["preload_aftercall"] = true
            Log.d(TAG, "Preloading After Call Native Ad with Unit ID: ${ads.nativeAdIdAfterCall}")
            AdLoader.Builder(activity.applicationContext, ads.nativeAdIdAfterCall)
                .forNativeAd { nativeAd ->
                    isLoadingMap["preload_aftercall"] = false
                    Log.d(TAG, "Preloaded After Call Native Ad loaded successfully")
                    logAds(activity, "native_loaded_aftercall")
                    nativeAdPreloadAfterCall = nativeAd
                }.withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        isLoadingMap["preload_aftercall"] = false
                        Log.e(TAG, "Preload After Call Native Ad failed: ${adError.message} (code ${adError.code})")
                        logAds(activity, "native_failed_aftercall_" + adError.code)
                    }
                }).withNativeAdOptions(adOptions).build().loadAd(adRequest)
        }
    }

    private fun loadAndShowNativeAd(
        activity: Activity,
        ltUniversal: FrameLayout,
        adsNativeBigLoadingBinding: FrameLayout,
        adType: String,
        adUnitID: String,
        type: String = "Default",
        cardView: CardView? = null
    ) {
        if (!ads.isNativeAdEnabled || adUnitID.isBlank()) {
            Log.d(TAG, "Native ad disabled or empty unit ID ($adUnitID)")
            ltUniversal.removeAllViews()
            ltUniversal.visibility = View.GONE
            adsNativeBigLoadingBinding.visibility = View.GONE
            cardView?.visibility = View.GONE
            return
        }

        val requestKey = "show:$type:$adUnitID"
        if (isLoadingMap[requestKey] == true) {
            Log.d(TAG, "Ad request for placement '$type' is already in progress. Ignoring duplicate request.")
            return
        }
        isLoadingMap[requestKey] = true

        Log.d(TAG, "On-demand Native Ad load: Showing shimmer layout for $type (Unit ID: $adUnitID)")
        ltUniversal.visibility = View.GONE
        adsNativeBigLoadingBinding.visibility = View.VISIBLE
        cardView?.visibility = View.VISIBLE

        val videoOptions = VideoOptions.Builder().setStartMuted(true).build()
        val adOptions = NativeAdOptions.Builder().setVideoOptions(videoOptions).build()
        val adLoaderNative = AdLoader.Builder(activity.applicationContext, adUnitID)
            .forNativeAd { nativeAd ->
                isLoadingMap[requestKey] = false
                Log.d(TAG, "Native Ad ($type) loaded successfully. Inflating view and hiding shimmer.")
                logAds(activity, "native_loaded_$type")
                resetFailedCountNative()
                if (!activity.isDestroyed && !activity.isFinishing) {
                    instance!!.inflateGoogleNativeAd(
                        activity,
                        ltUniversal,
                        adsNativeBigLoadingBinding,
                        nativeAd,
                        adType,
                        true,
                        type
                    )
                } else {
                    if (ads.nativeAdPreload) {
                        when (type) {
                            "Language" -> nativeAdPreloadLanguage = nativeAd
                            "AfterCall" -> nativeAdPreloadAfterCall = nativeAd
                            else -> nativeAdPreload = nativeAd
                        }
                    } else {
                        nativeAd.destroy()
                    }
                }
            }.withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    isLoadingMap[requestKey] = false
                    Log.e(TAG, "Native Ad ($type) failed to load: ${adError.message} (code ${adError.code}). Hiding shimmer layout.")
                    ltUniversal.visibility = View.GONE
                    adsNativeBigLoadingBinding.visibility = View.GONE
                    cardView?.visibility = View.GONE
                    increaseFailedCountNative()
                    logAds(activity, "native_failed_${type}_" + adError.code)
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    Log.d(TAG, "Native Ad ($type) clicked")
                    logAds(activity, "native_clicked_$type")
                }
            }).withNativeAdOptions(adOptions).build()
        val adRequest = AdRequest.Builder().build()
        logAds(activity, "native_req_$type")
        App.runWhenMobileAdsReady {
            if (activity.isFinishing || activity.isDestroyed) {
                isLoadingMap[requestKey] = false
                return@runWhenMobileAdsReady
            }
            adLoaderNative.loadAd(adRequest)
        }
    }

    private fun inflateGoogleNativeAd(
        activity: Activity,
        ltUniversal: FrameLayout,
        adsNativeBigLoadingBinding: FrameLayout,
        nativeAd: NativeAd,
        adType: String,
        isFromStatic: Boolean?,
        type: String = "Default"
    ) {
        adsNativeBigLoadingBinding.visibility = View.GONE
        ltUniversal.visibility = View.VISIBLE

        var adView =
            activity.layoutInflater.inflate(
                R.layout.ads_contact_native_big,
                null,
                false
            ) as NativeAdView
        if (adType.equals("Small", ignoreCase = true)) {
            adView = activity.layoutInflater.inflate(
                R.layout.ads_contact_native, null, false
            ) as NativeAdView
        } else if (adType.equals("Large", ignoreCase = true)) {
            adView = activity.layoutInflater.inflate(
                R.layout.ads_contact_native_big, null, false
            ) as NativeAdView
        }

        val install = adView.findViewById<TextView>(R.id.ad_call_to_action)
        adView.mediaView = adView.findViewById<MediaView?>(R.id.ad_media)
        adView.headlineView = adView.findViewById<View?>(R.id.ad_headline)
        adView.bodyView = adView.findViewById<View?>(R.id.ad_body)
        adView.callToActionView = install
        adView.iconView = adView.findViewById<View?>(R.id.ad_app_icon)
        adView.priceView = adView.findViewById<View?>(R.id.ad_price)
        adView.starRatingView = adView.findViewById<View?>(R.id.ad_stars)
        adView.storeView = adView.findViewById<View?>(R.id.ad_store)
        adView.advertiserView = adView.findViewById<View?>(R.id.ad_advertiser)

        install.text = nativeAd.callToAction

        adView.callToActionView = install

        (adView.headlineView as TextView).text = nativeAd.headline

        if (adView.mediaView != null) {
            adView.mediaView!!.mediaContent = nativeAd.mediaContent
        }

        if (nativeAd.body == null) {
            adView.bodyView!!.visibility = View.GONE
        } else {
            adView.bodyView!!.visibility = View.VISIBLE
            if (adView.bodyView != null) {
                (adView.bodyView as TextView).text = nativeAd.body
            }
        }

        if (nativeAd.callToAction == null) {
            adView.callToActionView!!.visibility = View.GONE
        } else {
            adView.callToActionView!!.visibility = View.VISIBLE
            if (adView.callToActionView != null) {
                (adView.callToActionView as TextView).text = nativeAd.callToAction
            }
        }

        if (nativeAd.icon == null) {
            adView.iconView!!.visibility = View.GONE
        } else {
            (adView.iconView as ImageView).setImageDrawable(nativeAd.icon!!.drawable)
            adView.iconView!!.visibility = View.VISIBLE
        }

        if (nativeAd.price == null) {
            if (adView.priceView != null) {
                adView.priceView!!.visibility = View.GONE
            }
        } else {
            if (adView.priceView != null) {
                adView.priceView!!.visibility = View.VISIBLE
                if (adView.priceView != null) {
                    (adView.priceView as TextView).text = nativeAd.price
                }
            }
        }

        if (nativeAd.store == null) {
            if (adView.storeView != null) {
                adView.storeView!!.visibility = View.GONE
            }
        } else {
            if (adView.storeView != null) {
                adView.storeView!!.visibility = View.VISIBLE
                if (adView.storeView != null) {
                    (adView.storeView as TextView).text = nativeAd.store
                }
            }
        }

        if (nativeAd.starRating == null) {
            if (adView.starRatingView != null) {
                adView.starRatingView!!.visibility = View.GONE
            }
        } else {
            if (adView.starRatingView != null) {
                (adView.starRatingView as RatingBar).rating = nativeAd.starRating!!.toFloat()
                adView.starRatingView!!.visibility = View.VISIBLE
            }
        }

        if (nativeAd.advertiser == null) {
            if (adView.advertiserView != null) {
                adView.advertiserView!!.visibility = View.GONE
            }
        } else {
            if (adView.advertiserView != null) {
                if (adView.advertiserView != null) {
                    (adView.advertiserView as TextView).text = nativeAd.advertiser
                }
                adView.advertiserView!!.visibility = View.VISIBLE
            }
        }

        adView.setNativeAd(nativeAd)

        if (adView.mediaView != null) {
            val vc = nativeAd.mediaContent!!.videoController

            if (nativeAd.mediaContent != null && nativeAd.mediaContent!!.hasVideoContent()) {
                vc.videoLifecycleCallbacks = object : VideoController.VideoLifecycleCallbacks() {
                    override fun onVideoEnd() {
                        super.onVideoEnd()
                    }
                }
            }
        }
        ltUniversal.removeAllViews()
        logAds(activity, "native_Showed_$type")
        ltUniversal.addView(adView)

        isFromStatic?.let { isStatic ->
            if (!isStatic) {
                when (type) {
                    "Language" -> nativeAdPreloadLanguage = null
                    "AfterCall" -> nativeAdPreloadAfterCall = null
                    else -> nativeAdPreload = null
                }
                loadNativeAdPreload(activity)
            }
        }
    }

    companion object {
        private const val TAG = "NativeAd"
        private const val KEY_FAILED_COUNT_NATIVE = "KeyFailedCountNative"

        var instance: NativeAdLoader? = null
            get() {
                if (field == null) {
                    field = NativeAdLoader()
                }
                return field
            }
            private set

        fun resetCounter() {
            resetFailedCountNative()
        }

        private val preference: SharedPreferences
            get() = App.getInstance().getSharedPreferences(
                App.getInstance().packageName, Context.MODE_PRIVATE
            )

        fun resetFailedCountNative() {
            val preferences: SharedPreferences = preference
            preferences.edit().putInt(KEY_FAILED_COUNT_NATIVE, 0).apply()
        }

        fun increaseFailedCountNative() {
            val preferences: SharedPreferences = preference
            preferences.edit().putInt(KEY_FAILED_COUNT_NATIVE, failedCountNative + 1).apply()
        }

        private val failedCountNative: Int
            get() {
                val preferences: SharedPreferences = preference
                return preferences.getInt(KEY_FAILED_COUNT_NATIVE, 0)
            }

        fun log(log: String?, e: Exception?) {
            Log.d(TAG, log, e)
        }
    }
}
