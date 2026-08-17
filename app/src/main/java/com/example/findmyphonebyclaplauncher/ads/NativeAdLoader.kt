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

    fun showNativeLarge(
        activity: Activity, ltNativeAds: FrameLayout, ltNativeShimmerAds: FrameLayout
    ) {
        if (ads.canShowNative) {
            ltNativeAds.visibility = View.GONE
            ltNativeShimmerAds.visibility = View.VISIBLE
            showNative(activity, ltNativeAds, ltNativeShimmerAds, "Large")
        } else {
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
            ltNativeAds.visibility = View.GONE
            ltNativeShimmerAds.visibility = View.VISIBLE

            if (nativeAdPreloadAfterCall != null) {
                instance!!.inflateGoogleNativeAd(
                    activity,
                    ltNativeAds,
                    ltNativeShimmerAds,
                    nativeAdPreloadAfterCall!!,
                    "Large",
                    false,
                    "AfterCall"
                )
            } else {
                loadAndShowNativeAd(
                    activity,
                    ltNativeAds,
                    ltNativeShimmerAds,
                    "Large",
                    ads.nativeAdIdAfterCall,
                    "AfterCall"
                )
            }
        } else {
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
            ltNativeAds.visibility = View.GONE
            ltNativeShimmerAds.visibility = View.VISIBLE

            Log.e("Language native load with id", "${ads.nativeAdIdLanguage}...")
            if (nativeAdPreloadLanguage != null) {
                instance!!.inflateGoogleNativeAd(
                    activity,
                    ltNativeAds,
                    ltNativeShimmerAds,
                    nativeAdPreloadLanguage!!,
                    "Large",
                    false,
                    "Language"
                )
            } else {
                loadAndShowNativeAd(
                    activity,
                    ltNativeAds,
                    ltNativeShimmerAds,
                    "Large",
                    ads.nativeAdIdLanguage,
                    "Language"
                )
            }
        } else {
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
        if (!ads.canShowNative) {
            onFailed()
            return
        }
        val videoOptions = VideoOptions.Builder().setStartMuted(true).build()
        val adOptions = NativeAdOptions.Builder().setVideoOptions(videoOptions).build()
        val adLoaderNative = AdLoader.Builder(activity.applicationContext, adUnitId)
            .forNativeAd { nativeAd ->
                logAds(activity, "native_loaded_$type")
                resetFailedCountNative()
                if (activity.isDestroyed || activity.isFinishing) {
                    nativeAd.destroy()
                    return@forNativeAd
                }
                onLoaded(nativeAd)
            }.withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    increaseFailedCountNative()
                    logAds(activity, "native_failed_${type}_" + adError.code)
                    onFailed()
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    logAds(activity, "native_clicked_$type")
                }
            }).withNativeAdOptions(adOptions).build()
        val adRequest = AdRequest.Builder().build()
        logAds(activity, "native_req_$type")
        App.runWhenMobileAdsReady {
            if (activity.isFinishing || activity.isDestroyed) return@runWhenMobileAdsReady
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
            ltNativeAds.visibility = View.GONE
            ltNativeShimmerAds.visibility = View.VISIBLE
            showNative(activity, ltNativeAds, ltNativeShimmerAds, "Small")
        } else {
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
            if (nativeAdPreload != null) {
                instance!!.inflateGoogleNativeAd(
                    activity,
                    ltNativeAds,
                    ltNativeShimmerAds,
                    nativeAdPreload!!,
                    adType,
                    false,
                    "Default"
                )
            } else {
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

        if (nativeAdPreload == null) {
            AdLoader.Builder(activity.applicationContext, ads.nativeAdIdDashboard)
                .forNativeAd { nativeAd ->
                    logAds(activity, "native_loaded_default")
                    nativeAdPreload = nativeAd
                }.withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        logAds(activity, "native_failed_default_" + adError.code)
                        increaseFailedCountNative()
                    }
                }).withNativeAdOptions(adOptions).build().loadAd(adRequest)
        }

        if (nativeAdPreloadLanguage == null) {
            AdLoader.Builder(activity.applicationContext, ads.nativeAdIdLanguage)
                .forNativeAd { nativeAd ->
                    logAds(activity, "native_loaded_language")
                    nativeAdPreloadLanguage = nativeAd
                }.withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        logAds(activity, "native_failed_language_" + adError.code)
                    }
                }).withNativeAdOptions(adOptions).build().loadAd(adRequest)
        }

        if (nativeAdPreloadAfterCall == null) {
            AdLoader.Builder(activity.applicationContext, ads.nativeAdIdAfterCall)
                .forNativeAd { nativeAd ->
                    logAds(activity, "native_loaded_aftercall")
                    nativeAdPreloadAfterCall = nativeAd
                }.withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
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
        type: String = "Default"
    ) {
        if (!ads.isNativeAdEnabled || adUnitID.isBlank()) {
            ltUniversal.removeAllViews()
            ltUniversal.visibility = View.GONE
            adsNativeBigLoadingBinding.visibility = View.GONE
            return
        }
        Log.e("NativeAdLoad", "adUnitID: $adUnitID, type: $type")
        val videoOptions = VideoOptions.Builder().setStartMuted(true).build()
        val adOptions = NativeAdOptions.Builder().setVideoOptions(videoOptions).build()
        val adLoaderNative = AdLoader.Builder(activity.applicationContext, adUnitID)
            .forNativeAd { nativeAd ->
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
                    when (type) {
                        "Language" -> nativeAdPreloadLanguage = nativeAd
                        "AfterCall" -> nativeAdPreloadAfterCall = nativeAd
                        else -> nativeAdPreload = nativeAd
                    }
                }
            }.withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    ltUniversal.visibility = View.GONE
                    adsNativeBigLoadingBinding.visibility = View.GONE
                    increaseFailedCountNative()
                    logAds(activity, "native_failed_${type}_" + adError.code)
                    Log.e("NativeAdLoad", "failed $adError")
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    logAds(activity, "native_clicked_$type")
                }
            }).withNativeAdOptions(adOptions).build()
        val adRequest = AdRequest.Builder().build()
        logAds(activity, "native_req_$type")
        App.runWhenMobileAdsReady {
            if (activity.isFinishing || activity.isDestroyed) return@runWhenMobileAdsReady
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
            Log.d("ADCBLOGICLOG", log, e)
        }
    }
}
