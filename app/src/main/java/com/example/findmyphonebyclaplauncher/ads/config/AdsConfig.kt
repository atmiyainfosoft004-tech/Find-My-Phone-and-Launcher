package com.example.findmyphonebyclaplauncher.ads.config

import com.google.gson.annotations.SerializedName

data class AdsConfig(
    // System & Feature Flags
    @SerializedName("system_hide_navigation_bar_auto")
    val systemHideNavigationBarAuto: Boolean = true,

    // Global Ad Type Switches (Master Toggles)
    @SerializedName("is_banner_ad_enabled")
    val isBannerAdEnabled: Boolean = true,

    @SerializedName("is_native_ad_enabled")
    val isNativeAdEnabled: Boolean = true,

    @SerializedName("is_inter_ad_enabled")
    val isInterAdEnabled: Boolean = true,

    @SerializedName("is_app_open_ad_enabled")
    val isAppOpenAdEnabled: Boolean = true,

    // Action-Level Ad Switches
    @SerializedName("is_click_ad_enabled")
    val isClickAdEnabled: Boolean = true,

    @SerializedName("is_swipe_ad_enabled")
    val isSwipeAdEnabled: Boolean = true,

    @SerializedName(value = "right_to_left_ad_enable", alternate = ["rightToLeftAdEnable", "is_right_to_left_ad_enabled", "isRightToLeftAdEnabled"])
    val rightToLeftAdEnable: Boolean = true,

    @SerializedName(value = "left_to_right_ad_enable", alternate = ["leftToRightAdEnable", "is_left_to_right_ad_enabled", "isLeftToRightAdEnabled"])
    val leftToRightAdEnable: Boolean = true,

    @SerializedName("is_back_ad_enabled")
    val isBackAdEnabled: Boolean = true,

    // Action Ad Format Selectors (true = Interstitial | false = App Open)
    @SerializedName(value = "is_click_ad_interstitial", alternate = ["isAppClickInterOn", "click_ad_type_selection"])
    val isClickAdInterstitial: Boolean = true,

    @SerializedName(value = "is_swipe_ad_interstitial", alternate = ["isRightLeftSwipeInterOn", "swipe_ad_type_selection"])
    val isSwipeAdInterstitial: Boolean = true,

    // Counters & Triggers
    @SerializedName(value = "right_to_left_count", alternate = ["right_to_left", "rightToLeftCount", "rightToLeft", "right_to_left_trigger", "inter_ad_right_to_left_count"])
    val rightToLeftCount: Int = 3,

    @SerializedName(value = "left_to_right_count", alternate = ["left_to_right", "leftToRightCount", "leftToRight", "left_to_right_trigger", "inter_ad_left_to_right_count"])
    val leftToRightCount: Int = 3,

    @SerializedName(value = "inter_ad_counter_trigger", alternate = ["inter_count", "interstitialForwardAdCount", "interCount"])
    val interAdCounterTrigger: Int = 3,

    @SerializedName(value = "inter_ad_back_counter_trigger", alternate = ["inter_back_count", "interstitialBackwardAdCount"])
    val interAdBackCounterTrigger: Int = 3,

    @SerializedName(value = "click_ad_counter_trigger", alternate = ["click_count"])
    val clickAdCounterTrigger: Int = 3,

    // Banner Ads - Screen Specific Enable/Disable
    @SerializedName("banner_ad_enable_splash")
    val bannerAdEnableSplash: Boolean = true,

    @SerializedName(value = "banner_ad_enable_home_screen", alternate = ["banner_ad_enable_home", "banner_ad_enable_contact_home", "banner_ad_enable_launcher_home"])
    val bannerAdEnableHome: Boolean = false,

    @SerializedName("banner_ad_enable_app_drawer")
    val bannerAdEnableAppDrawer: Boolean = true,

    @SerializedName("banner_ad_enable_find_phone")
    val bannerAdEnableFindPhone: Boolean = true,

    @SerializedName(value = "banner_ad_enable_alert_screen", alternate = ["banner_ad_enable_alert", "banner_ad_enable_alert_activity"])
    val bannerAdEnableAlertScreen: Boolean = true,

    @SerializedName(value = "banner_ad_enable_after_call", alternate = ["banner_ad_enable_aftercall", "native_ad_enable_after_call"])
    val bannerAdEnableAfterCall: Boolean = true,

    @SerializedName(value = "banner_ad_enable_language_rect", alternate = ["is_lang_rect_banner_enabled", "banner_ad_enable_language_rectangle"])
    val bannerAdEnableLanguageRect: Boolean = true,

    @SerializedName(value = "banner_ad_enable_onboarding", alternate = ["banner_ad_enable_onboarding_screen"])
    val bannerAdEnableOnboarding: Boolean = true,

    // Banner Ads - Unit IDs
    @SerializedName("banner_ad_id_splash")
    val bannerAdIdSplash: String = DEFAULT_BANNER_ID,

    @SerializedName(value = "banner_ad_id_home_screen", alternate = ["banner_ad_id_home", "banner_ad_id_contact_home", "banner_ad_id_launcher_home"])
    val bannerAdIdHome: String = DEFAULT_BANNER_ID,

    @SerializedName("banner_ad_id_app_drawer")
    val bannerAdIdAppDrawer: String = DEFAULT_BANNER_ID,

    @SerializedName("banner_ad_id_find_phone")
    val bannerAdIdFindPhone: String = DEFAULT_BANNER_ID,

    @SerializedName(value = "banner_ad_id_alert_screen", alternate = ["banner_ad_id_alert", "banner_ad_id_alert_activity"])
    val bannerAdIdAlertScreen: String = DEFAULT_BANNER_ID,

    @SerializedName(value = "banner_ad_id_after_call", alternate = ["banner_ad_id_aftercall", "native_ad_id_after_call"])
    val bannerAdIdAfterCall: String = DEFAULT_BANNER_ID,

    @SerializedName(value = "banner_ad_id_language_rect", alternate = ["banner_ad_id_language_rectangle"])
    val bannerAdIdLanguageRect: String = DEFAULT_BANNER_ID,

    @SerializedName(value = "banner_ad_id_onboarding", alternate = ["banner_ad_id_onboarding_screen"])
    val bannerAdIdOnboarding: String = DEFAULT_BANNER_ID,

    // Native Ads - Screen Specific Enable/Disable
    @SerializedName("native_ad_enable_dashboard")
    val nativeAdEnableDashboard: Boolean = true,

    @SerializedName("native_ad_enable_google_search")
    val nativeAdEnableGoogleSearch: Boolean = true,

    @SerializedName("native_ad_enable_language")
    val nativeAdEnableLanguage: Boolean = true,

    @SerializedName(value = "native_ad_enable_install_uninstall", alternate = ["native_ad_enable_install", "native_ad_enable_app_install"])
    val nativeAdEnableInstallUninstall: Boolean = true,

    // Native Ads - Unit IDs & Configuration
    @SerializedName("native_ad_id_dashboard")
    val nativeAdIdDashboard: String = DEFAULT_NATIVE_ID,

    @SerializedName("native_ad_id_google_search")
    val nativeAdIdGoogleSearch: String = DEFAULT_NATIVE_ID,

    @SerializedName("native_ad_id_language")
    val nativeAdIdLanguage: String = DEFAULT_NATIVE_ID,

    @SerializedName(value = "native_ad_id_install_uninstall", alternate = ["native_ad_id_install", "native_ad_id_app_install"])
    val nativeAdIdInstallUninstall: String = DEFAULT_NATIVE_ID,

    @SerializedName("native_ad_google_search_item_interval")
    val nativeAdGoogleSearchItemInterval: Int = 2,

    // Interstitial & Open Ad Unit IDs & Screen Switches
    @SerializedName(value = "inter_ad_enable_language", alternate = ["is_language_inter_ad_enabled", "inter_ad_enable_language_screen"])
    val interAdEnableLanguage: Boolean = true,

    @SerializedName("inter_ad_id")
    val interAdId: String = DEFAULT_INTER_ID,

    @SerializedName("app_open_ad_id")
    val appOpenAdId: String = DEFAULT_APP_OPEN_ID,

    // Preload Configurations
    @SerializedName(value = "preload_ad_banner", alternate = ["bannerAdPreload"])
    val preloadAdBanner: Boolean = false,

    @SerializedName(value = "preload_ad_native", alternate = ["nativeAdPreload"])
    val preloadAdNative: Boolean = false,

    @SerializedName(value = "preload_ad_interstitial", alternate = ["interAdPreload"])
    val preloadAdInterstitial: Boolean = false,

    @SerializedName(value = "preload_ad_app_open", alternate = ["appOpenAdPreload"])
    val preloadAdAppOpen: Boolean = false,
) {
    // Backward compatibility aliases
    val bannerAdIdContactHome: String get() = bannerAdIdHome
    val bannerAdEnableContactHome: Boolean get() = bannerAdEnableHome
    val bannerAdIdAlertActivity: String get() = bannerAdIdAlertScreen
    val bannerAdEnableAlertActivity: Boolean get() = bannerAdEnableAlertScreen
    val nativeAdEnableAfterCall: Boolean get() = bannerAdEnableAfterCall
    val nativeAdIdAfterCall: String get() = bannerAdIdAfterCall
    val canShowNativeAfterCall: Boolean get() = canShowBannerAfterCall
    val interCount: Int get() = interAdCounterTrigger
    val interBackCount: Int get() = interAdBackCounterTrigger
    val clickCount: Int get() = clickAdCounterTrigger
    val rightToLeft: Int get() = rightToLeftCount
    val leftToRight: Int get() = leftToRightCount
    val bannerAdPreload: Boolean get() = preloadAdBanner
    val nativeAdPreload: Boolean get() = preloadAdNative
    val interAdPreload: Boolean get() = preloadAdInterstitial
    val appOpenAdPreload: Boolean get() = preloadAdAppOpen

    // Banner visibility checks (Requires Master Switch AND Screen Switch AND Valid ID)
    val canShowBanner: Boolean get() = isBannerAdEnabled
    val canShowBannerSplash: Boolean get() = isBannerAdEnabled && bannerAdEnableSplash && bannerAdIdSplash.isNotBlank()
    val canShowBannerHome: Boolean get() = isBannerAdEnabled && bannerAdEnableHome && bannerAdIdHome.isNotBlank()
    val canShowBannerContactHome: Boolean get() = canShowBannerHome
    val canShowBannerAppDrawer: Boolean get() = isBannerAdEnabled && bannerAdEnableAppDrawer && bannerAdIdAppDrawer.isNotBlank()
    val canShowBannerFindPhone: Boolean get() = isBannerAdEnabled && bannerAdEnableFindPhone && bannerAdIdFindPhone.isNotBlank()
    val canShowBannerAlertScreen: Boolean get() = isBannerAdEnabled && bannerAdEnableAlertScreen && bannerAdIdAlertScreen.isNotBlank()
    val canShowBannerAlertActivity: Boolean get() = canShowBannerAlertScreen
    val canShowBannerAfterCall: Boolean get() = isBannerAdEnabled && bannerAdEnableAfterCall && bannerAdIdAfterCall.isNotBlank()
    val canShowBannerLanguageRect: Boolean get() = isBannerAdEnabled && bannerAdEnableLanguageRect && bannerAdIdLanguageRect.isNotBlank()
    val canShowBannerOnboarding: Boolean get() = isBannerAdEnabled && bannerAdEnableOnboarding && bannerAdIdOnboarding.isNotBlank()

    // Native visibility checks (Requires Master Switch AND Screen Switch AND Valid ID)
    val canShowNative: Boolean get() = isNativeAdEnabled
    val canShowNativeDashboard: Boolean get() = isNativeAdEnabled && nativeAdEnableDashboard && nativeAdIdDashboard.isNotBlank()
    val canShowNativeGoogleSearch: Boolean get() = isNativeAdEnabled && nativeAdEnableGoogleSearch && nativeAdIdGoogleSearch.isNotBlank()
    val canShowNativeLanguage: Boolean get() = isNativeAdEnabled && nativeAdEnableLanguage && nativeAdIdLanguage.isNotBlank()
    val canShowNativeInstallUninstall: Boolean get() = isNativeAdEnabled && nativeAdEnableInstallUninstall && nativeAdIdInstallUninstall.isNotBlank()

    // Interstitial & App Open visibility checks (Requires Master Switch AND Valid ID)
    val canShowInter: Boolean get() = isInterAdEnabled && interAdId.isNotBlank()
    val canShowInterLanguage: Boolean get() = isInterAdEnabled && interAdEnableLanguage && interAdId.isNotBlank()
    val canShowAppOpen: Boolean get() = isAppOpenAdEnabled && appOpenAdId.isNotBlank()

    // Action-level visibility checks
    val isClickInter: Boolean get() = isClickAdInterstitial
    val isClickAppOpen: Boolean get() = !isClickAdInterstitial
    val canShowClickAd: Boolean get() = isClickAdEnabled && (if (isClickAdInterstitial) canShowInter else canShowAppOpen)
    val isAppClickInterOn: Boolean get() = isClickAdEnabled && isClickAdInterstitial
    val canShowAppClickInter: Boolean get() = isClickAdEnabled && isClickAdInterstitial && canShowInter
    val canShowAppClickOpen: Boolean get() = isClickAdEnabled && !isClickAdInterstitial && canShowAppOpen

    val isSwipeInter: Boolean get() = isSwipeAdInterstitial
    val isSwipeAppOpen: Boolean get() = !isSwipeAdInterstitial
    val canShowSwipeAd: Boolean get() = isSwipeAdEnabled && (if (isSwipeAdInterstitial) canShowInter else canShowAppOpen)
    val canShowRightToLeftAd: Boolean get() = isSwipeAdEnabled && rightToLeftAdEnable && (if (isSwipeAdInterstitial) canShowInter else canShowAppOpen)
    val canShowLeftToRightAd: Boolean get() = isSwipeAdEnabled && leftToRightAdEnable && (if (isSwipeAdInterstitial) canShowInter else canShowAppOpen)
    val isRightLeftSwipeInterOn: Boolean get() = isSwipeAdEnabled && isSwipeAdInterstitial
    val canShowSwipeInter: Boolean get() = isSwipeAdEnabled && isSwipeAdInterstitial && canShowInter
    val canShowSwipeOpen: Boolean get() = isSwipeAdEnabled && !isSwipeAdInterstitial && canShowAppOpen

    val canShowBackAd: Boolean get() = isBackAdEnabled && canShowInter

    val clickAdTypeSelection: String get() = if (isClickAdInterstitial) "Inter" else "Open"
    val swipeAdTypeSelection: String get() = if (isSwipeAdInterstitial) "Inter" else "Open"

    companion object {
        const val DEFAULT_BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
        const val DEFAULT_NATIVE_ID = "ca-app-pub-3940256099942544/2247696110"
        const val DEFAULT_INTER_ID = "ca-app-pub-3940256099942544/1033173712"
        const val DEFAULT_APP_OPEN_ID = "ca-app-pub-3940256099942544/9257395921"

        val DEFAULT = AdsConfig()
    }
}
