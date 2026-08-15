package com.example.findmyphonebyclaplauncher.ads.config

import com.google.gson.annotations.SerializedName

data class AdsConfig(
    @SerializedName("banner_ad_id_splash")
    val bannerAdIdSplash: String = DEFAULT_BANNER_ID,

    @SerializedName("banner_ad_id_contact_home")
    val bannerAdIdContactHome: String = DEFAULT_BANNER_ID,

    @SerializedName("banner_ad_id_app_drawer")
    val bannerAdIdAppDrawer: String = DEFAULT_BANNER_ID,

    @SerializedName("native_ad_id_dashboard")
    val nativeAdIdDashboard: String = DEFAULT_NATIVE_ID,

    @SerializedName("native_ad_id_google_search")
    val nativeAdIdGoogleSearch: String = DEFAULT_NATIVE_ID,

    @SerializedName("native_ad_id_language")
    val nativeAdIdLanguage: String = DEFAULT_NATIVE_ID,

    @SerializedName("native_ad_id_after_call")
    val nativeAdIdAfterCall: String = DEFAULT_NATIVE_ID,

    @SerializedName("inter_ad_id")
    val interAdId: String = DEFAULT_INTER_ID,

    @SerializedName("app_open_ad_id")
    val appOpenAdId: String = DEFAULT_APP_OPEN_ID,

    @SerializedName(value = "inter_count", alternate = ["interstitialForwardAdCount", "interCount"])
    val interCount: Int = 1,

    @SerializedName(value = "inter_back_count", alternate = ["interstitialBackwardAdCount"])
    val interBackCount: Int = 3,

    @SerializedName("isAppClickInterOn")
    val isAppClickInterOn: Boolean = true,

    @SerializedName("isRightLeftSwipeInterOn")
    val isRightLeftSwipeInterOn: Boolean = true,

    @SerializedName("isBannerOn")
    val isBannerOn: Boolean = true,

    @SerializedName("isNativeOn")
    val isNativeOn: Boolean = true,

    @SerializedName("isInterOn")
    val isInterOn: Boolean = true,

    @SerializedName("bannerAdPreload")
    val bannerAdPreload: Boolean = false,

    @SerializedName("nativeAdPreload")
    val nativeAdPreload: Boolean = false,

    @SerializedName("interAdPreload")
    val interAdPreload: Boolean = true,

    @SerializedName("appOpenAdPreload")
    val appOpenAdPreload: Boolean = false,
) {
    val canShowBanner: Boolean get() = isBannerOn
    val canShowBannerSplash: Boolean get() = isBannerOn && false
    val canShowNative: Boolean get() = isNativeOn
    val canShowInter: Boolean get() = isInterOn
    val canShowAppOpen: Boolean get() = false
    val canShowAppClickInter: Boolean get() = isAppClickInterOn
    val canShowSwipeInter: Boolean get() = isRightLeftSwipeInterOn

    companion object {
        const val DEFAULT_BANNER_ID = "ca-app-pub-3940256099942544/9214589741"
        const val DEFAULT_NATIVE_ID = "ca-app-pub-3940256099942544/2247696110"
        const val DEFAULT_INTER_ID = "ca-app-pub-3940256099942544/1033173712"
        const val DEFAULT_APP_OPEN_ID = "ca-app-pub-3940256099942544/9257395921"

        val DEFAULT = AdsConfig()
    }
}
