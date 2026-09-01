package com.example.findmyphonebyclaplauncher.ads.config

import android.content.Context
import android.util.Log
import com.example.findmyphonebyclaplauncher.BuildConfig
import com.example.findmyphonebyclaplauncher.R
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings

/**
 * Single source of truth for all Remote Config parameters.
 * Provides live dynamic getters directly accessing FirebaseRemoteConfig.
 */
object RemoteConfigRepository {

    private const val TAG = "RemoteConfig"

    // System & Ad Toggles
    const val KEY_SYSTEM_HIDE_NAVIGATION_BAR_AUTO = "system_hide_navigation_bar_auto"
    const val KEY_IS_BANNER_AD_ENABLED = "is_banner_ad_enabled"
    const val KEY_IS_NATIVE_AD_ENABLED = "is_native_ad_enabled"
    const val KEY_IS_INTER_AD_ENABLED = "is_inter_ad_enabled"
    const val KEY_IS_APP_OPEN_AD_ENABLED = "is_app_open_ad_enabled"
    const val KEY_IS_CLICK_AD_ENABLED = "is_click_ad_enabled"
    const val KEY_IS_SWIPE_AD_ENABLED = "is_swipe_ad_enabled"
    const val KEY_RIGHT_TO_LEFT_AD_ENABLE = "right_to_left_ad_enable"
    const val KEY_LEFT_TO_RIGHT_AD_ENABLE = "left_to_right_ad_enable"
    const val KEY_IS_BACK_AD_ENABLED = "is_back_ad_enabled"
    const val KEY_IS_CLICK_AD_INTERSTITIAL = "is_click_ad_interstitial"
    const val KEY_IS_SWIPE_AD_INTERSTITIAL = "is_swipe_ad_interstitial"

    // Trigger Counters
    const val KEY_RIGHT_TO_LEFT_COUNT = "right_to_left_count"
    const val KEY_LEFT_TO_RIGHT_COUNT = "left_to_right_count"
    const val KEY_RIGHT_TO_LEFT_LEGACY = "right_to_left"
    const val KEY_LEFT_TO_RIGHT_LEGACY = "left_to_right"
    const val KEY_INTER_AD_COUNTER_TRIGGER = "inter_ad_counter_trigger"
    const val KEY_INTER_AD_BACK_COUNTER_TRIGGER = "inter_ad_back_counter_trigger"
    const val KEY_CLICK_AD_COUNTER_TRIGGER = "click_ad_counter_trigger"
    const val KEY_NATIVE_AD_GOOGLE_SEARCH_ITEM_INTERVAL = "native_ad_google_search_item_interval"
    const val KEY_FEEDLIST_CONFIG = "feedlist_config"
    const val DEFAULT_FEEDLIST_CONFIG_JSON = """{
  "feedlist": [
    {
      "id": "1",
      "heading": "Forget chatbot training. AI's next big data grab is about learning how humans work.",
      "description": "Discover top trending topics and updates from Google Search",
      "source": "businessinsider.com",
      "timeLabel": "2:30pm, Wed",
      "feedurl": "https://www.businessinsider.com",
      "img_url": ""
    },
    {
      "id": "2",
      "heading": "Inflation is expected to cool again in today's July CPI report",
      "description": "Financial news and updates",
      "source": "businessinsider.com",
      "timeLabel": "2:30pm, Wed",
      "feedurl": "https://www.businessinsider.com",
      "img_url": ""
    },
    {
      "id": "3",
      "heading": "FBI touts 6,000 arrests, 44% homicide drop one year into Trump's DC crime crackdown",
      "description": "National news updates",
      "source": "foxnews.com",
      "timeLabel": "2:30pm, Wed",
      "feedurl": "https://www.foxnews.com",
      "img_url": ""
    },
    {
      "id": "4",
      "heading": "How new Android launchers are changing the home screen experience in 2026",
      "description": "Tech news and mobile launcher trends",
      "source": "androidauthority.com",
      "timeLabel": "1:10pm, Wed",
      "feedurl": "https://www.androidauthority.com",
      "img_url": ""
    },
    {
      "id": "5",
      "heading": "5 ways to cut phone screen time without deleting your favorite apps",
      "description": "Productivity tips and app balance",
      "source": "techcrunch.com",
      "timeLabel": "11:45am, Wed",
      "feedurl": "https://techcrunch.com",
      "img_url": ""
    },
    {
      "id": "6",
      "heading": "Call blocking tips that actually reduce spam without missing important numbers",
      "description": "Phone security and spam call protection",
      "source": "theverge.com",
      "timeLabel": "10:20am, Wed",
      "feedurl": "https://www.theverge.com",
      "img_url": ""
    },
    {
      "id": "7",
      "heading": "Why privacy-first dialers are gaining popularity on Google Play",
      "description": "Android app market trends",
      "source": "9to5google.com",
      "timeLabel": "9:05am, Wed",
      "feedurl": "https://9to5google.com",
      "img_url": ""
    },
    {
      "id": "8",
      "heading": "Smart search features users expect from a modern Android launcher",
      "description": "UI/UX launcher features",
      "source": "androidpolice.com",
      "timeLabel": "8:40am, Wed",
      "feedurl": "https://www.androidpolice.com",
      "img_url": ""
    },
    {
      "id": "9",
      "heading": "Battery myths that still waste hours of phone life every week",
      "description": "Hardware and battery performance tips",
      "source": "wired.com",
      "timeLabel": "7:15am, Wed",
      "feedurl": "https://www.wired.com",
      "img_url": ""
    }
  ]
}"""

    // Banner Screen Enables & IDs
    const val KEY_BANNER_AD_ENABLE_SPLASH = "banner_ad_enable_splash"
    const val KEY_BANNER_AD_ENABLE_HOME_SCREEN = "banner_ad_enable_home_screen"
    const val KEY_BANNER_AD_ENABLE_APP_DRAWER = "banner_ad_enable_app_drawer"
    const val KEY_BANNER_AD_ENABLE_FIND_PHONE = "banner_ad_enable_find_phone"
    const val KEY_BANNER_AD_ENABLE_ALERT_SCREEN = "banner_ad_enable_alert_screen"
    const val KEY_BANNER_AD_ENABLE_AFTER_CALL = "banner_ad_enable_after_call"
    const val KEY_BANNER_AD_ENABLE_LANGUAGE_RECT = "banner_ad_enable_language_rect"
    const val KEY_BANNER_AD_ENABLE_ONBOARDING = "banner_ad_enable_onboarding"

    const val KEY_BANNER_AD_ID_SPLASH = "banner_ad_id_splash"
    const val KEY_BANNER_AD_ID_HOME_SCREEN = "banner_ad_id_home_screen"
    const val KEY_BANNER_AD_ID_APP_DRAWER = "banner_ad_id_app_drawer"
    const val KEY_BANNER_AD_ID_FIND_PHONE = "banner_ad_id_find_phone"
    const val KEY_BANNER_AD_ID_ALERT_SCREEN = "banner_ad_id_alert_screen"
    const val KEY_BANNER_AD_ID_AFTER_CALL = "banner_ad_id_after_call"
    const val KEY_BANNER_AD_ID_LANGUAGE_RECT = "banner_ad_id_language_rect"
    const val KEY_BANNER_AD_ID_ONBOARDING = "banner_ad_id_onboarding"

    // Native Screen Enables & IDs
    const val KEY_NATIVE_AD_ENABLE_DASHBOARD = "native_ad_enable_dashboard"
    const val KEY_NATIVE_AD_ENABLE_GOOGLE_SEARCH = "native_ad_enable_google_search"
    const val KEY_NATIVE_AD_ENABLE_LANGUAGE = "native_ad_enable_language"
    const val KEY_NATIVE_AD_ENABLE_INSTALL_UNINSTALL = "native_ad_enable_install_uninstall"

    const val KEY_NATIVE_AD_ID_DASHBOARD = "native_ad_id_dashboard"
    const val KEY_NATIVE_AD_ID_GOOGLE_SEARCH = "native_ad_id_google_search"
    const val KEY_NATIVE_AD_ID_LANGUAGE = "native_ad_id_language"
    const val KEY_NATIVE_AD_ID_INSTALL_UNINSTALL = "native_ad_id_install_uninstall"

    // Global Ad IDs & Preload Flags
    const val KEY_INTER_AD_ENABLE_LANGUAGE = "inter_ad_enable_language"
    const val KEY_INTER_AD_ID = "inter_ad_id"
    const val KEY_APP_OPEN_ID = "app_open_ad_id"
    const val KEY_APP_OPEN_AD_ID = "app_open_ad_id"
    const val KEY_PRELOAD_AD_BANNER = "preload_ad_banner"
    const val KEY_PRELOAD_AD_NATIVE = "preload_ad_native"
    const val KEY_PRELOAD_AD_INTERSTITIAL = "preload_ad_interstitial"
    const val KEY_PRELOAD_AD_APP_OPEN = "preload_ad_app_open"

    // URLs
    const val KEY_PRIVACY_POLICY_URL = "privacy_policy_url"
    const val KEY_TERMS_AND_CONDITIONS_URL = "terms_and_conditions_url"
    const val DEFAULT_PRIVACY_POLICY_URL = "https://example.com/privacy-policy"
    const val DEFAULT_TERMS_AND_CONDITIONS_URL = "https://example.com/terms-of-service"

    private val remoteConfig: FirebaseRemoteConfig?
        get() = runCatching { FirebaseRemoteConfig.getInstance() }.getOrNull()

    fun initialize(context: Context, onComplete: ((success: Boolean) -> Unit)? = null) {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }

            val config = FirebaseRemoteConfig.getInstance()
            val minimumFetchInterval = if (BuildConfig.DEBUG) 0L else 3600L
            val settings = FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(minimumFetchInterval)
                .build()
            config.setConfigSettingsAsync(settings)

            val defaultMap = mapOf<String, Any>(
                KEY_SYSTEM_HIDE_NAVIGATION_BAR_AUTO to true,
                KEY_IS_BANNER_AD_ENABLED to true,
                KEY_IS_NATIVE_AD_ENABLED to true,
                KEY_IS_INTER_AD_ENABLED to true,
                KEY_IS_APP_OPEN_AD_ENABLED to true,
                KEY_IS_CLICK_AD_ENABLED to true,
                KEY_IS_SWIPE_AD_ENABLED to true,
                KEY_RIGHT_TO_LEFT_AD_ENABLE to true,
                KEY_LEFT_TO_RIGHT_AD_ENABLE to true,
                KEY_IS_BACK_AD_ENABLED to true,
                KEY_IS_CLICK_AD_INTERSTITIAL to true,
                KEY_IS_SWIPE_AD_INTERSTITIAL to true,
                KEY_RIGHT_TO_LEFT_COUNT to 3,
                KEY_LEFT_TO_RIGHT_COUNT to 3,
                KEY_INTER_AD_COUNTER_TRIGGER to 3,
                KEY_INTER_AD_BACK_COUNTER_TRIGGER to 3,
                KEY_CLICK_AD_COUNTER_TRIGGER to 3,
                KEY_NATIVE_AD_GOOGLE_SEARCH_ITEM_INTERVAL to 2,
                KEY_BANNER_AD_ENABLE_SPLASH to true,
                KEY_BANNER_AD_ENABLE_HOME_SCREEN to false,
                KEY_BANNER_AD_ENABLE_APP_DRAWER to true,
                KEY_BANNER_AD_ENABLE_FIND_PHONE to true,
                KEY_BANNER_AD_ENABLE_ALERT_SCREEN to true,
                KEY_BANNER_AD_ENABLE_AFTER_CALL to true,
                KEY_BANNER_AD_ENABLE_LANGUAGE_RECT to true,
                KEY_BANNER_AD_ENABLE_ONBOARDING to true,
                KEY_BANNER_AD_ID_SPLASH to AdsConfig.DEFAULT_BANNER_ID,
                KEY_BANNER_AD_ID_HOME_SCREEN to AdsConfig.DEFAULT_BANNER_ID,
                KEY_BANNER_AD_ID_APP_DRAWER to AdsConfig.DEFAULT_BANNER_ID,
                KEY_BANNER_AD_ID_FIND_PHONE to AdsConfig.DEFAULT_BANNER_ID,
                KEY_BANNER_AD_ID_ALERT_SCREEN to AdsConfig.DEFAULT_BANNER_ID,
                KEY_BANNER_AD_ID_AFTER_CALL to AdsConfig.DEFAULT_BANNER_ID,
                KEY_BANNER_AD_ID_LANGUAGE_RECT to AdsConfig.DEFAULT_BANNER_ID,
                KEY_BANNER_AD_ID_ONBOARDING to AdsConfig.DEFAULT_BANNER_ID,
                KEY_NATIVE_AD_ENABLE_DASHBOARD to true,
                KEY_NATIVE_AD_ENABLE_GOOGLE_SEARCH to true,
                KEY_NATIVE_AD_ENABLE_LANGUAGE to true,
                KEY_NATIVE_AD_ENABLE_INSTALL_UNINSTALL to true,
                KEY_NATIVE_AD_ID_DASHBOARD to AdsConfig.DEFAULT_NATIVE_ID,
                KEY_NATIVE_AD_ID_GOOGLE_SEARCH to AdsConfig.DEFAULT_NATIVE_ID,
                KEY_NATIVE_AD_ID_LANGUAGE to AdsConfig.DEFAULT_NATIVE_ID,
                KEY_NATIVE_AD_ID_INSTALL_UNINSTALL to AdsConfig.DEFAULT_NATIVE_ID,
                KEY_INTER_AD_ENABLE_LANGUAGE to true,
                KEY_INTER_AD_ID to AdsConfig.DEFAULT_INTER_ID,
                KEY_APP_OPEN_ID to AdsConfig.DEFAULT_APP_OPEN_ID,
                KEY_PRELOAD_AD_BANNER to false,
                KEY_PRELOAD_AD_NATIVE to false,
                KEY_PRELOAD_AD_INTERSTITIAL to false,
                KEY_PRELOAD_AD_APP_OPEN to false,
                KEY_PRIVACY_POLICY_URL to DEFAULT_PRIVACY_POLICY_URL,
                KEY_TERMS_AND_CONDITIONS_URL to DEFAULT_TERMS_AND_CONDITIONS_URL,
                KEY_FEEDLIST_CONFIG to DEFAULT_FEEDLIST_CONFIG_JSON
            )
            config.setDefaultsAsync(defaultMap)

            config.addOnConfigUpdateListener(object : ConfigUpdateListener {
                override fun onUpdate(configUpdate: ConfigUpdate) {
                    Log.d(TAG, "Real-time config update keys: ${configUpdate.updatedKeys}")
                    config.activate().addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d(TAG, "Updated inter_ad_back_counter_trigger: ${getBackAdCounterTrigger()}")
                            AdsConfigManager.refresh()
                        }
                    }
                }

                override fun onError(error: FirebaseRemoteConfigException) {
                    Log.e(TAG, "Real-time config error: ${error.message}", error)
                }
            })

            fetchAndActivate(onComplete)
        } catch (e: Exception) {
            Log.e(TAG, "Initialization failed", e)
            onComplete?.invoke(false)
        }
    }

    fun fetchAndActivate(onComplete: ((success: Boolean) -> Unit)? = null) {
        val config = remoteConfig ?: run {
            onComplete?.invoke(false)
            return
        }
        val minimumFetchInterval = if (BuildConfig.DEBUG) 0L else 3600L
        val settings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(minimumFetchInterval)
            .build()
        config.setConfigSettingsAsync(settings)

        config.fetchAndActivate().addOnCompleteListener { task ->
            Log.d(TAG, "Fetch successful: ${task.isSuccessful} | Updated inter_ad_back_counter_trigger: ${getBackAdCounterTrigger()}")
            onComplete?.invoke(task.isSuccessful)
        }
    }

    // Generic live accessors
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return remoteConfig?.getBoolean(key) ?: defaultValue
    }

    fun getLong(key: String, defaultValue: Long = 0L): Long {
        return remoteConfig?.getLong(key) ?: defaultValue
    }

    fun getInt(key: String, defaultValue: Int = 0): Int {
        return remoteConfig?.getLong(key)?.toInt() ?: defaultValue
    }

    fun getString(key: String, defaultValue: String = ""): String {
        val str = remoteConfig?.getString(key)
        return if (str.isNullOrBlank()) defaultValue else str
    }

    // Dynamic Live Property Getters
    val isSystemHideNavigationBarAuto: Boolean
        get() = getBoolean(KEY_SYSTEM_HIDE_NAVIGATION_BAR_AUTO, true)

    val isBannerAdEnabled: Boolean
        get() = getBoolean(KEY_IS_BANNER_AD_ENABLED, true)

    val isNativeAdEnabled: Boolean
        get() = getBoolean(KEY_IS_NATIVE_AD_ENABLED, true)

    val isInterAdEnabled: Boolean
        get() = getBoolean(KEY_IS_INTER_AD_ENABLED, true)

    val isAppOpenAdEnabled: Boolean
        get() = getBoolean(KEY_IS_APP_OPEN_AD_ENABLED, true)

    val isClickAdEnabled: Boolean
        get() = getBoolean(KEY_IS_CLICK_AD_ENABLED, true)

    val isSwipeAdEnabled: Boolean
        get() = getBoolean(KEY_IS_SWIPE_AD_ENABLED, true)

    val isRightToLeftAdEnabled: Boolean
        get() = getBoolean(KEY_RIGHT_TO_LEFT_AD_ENABLE, true)

    val isLeftToRightAdEnabled: Boolean
        get() = getBoolean(KEY_LEFT_TO_RIGHT_AD_ENABLE, true)

    val isBackAdEnabled: Boolean
        get() = getBoolean(KEY_IS_BACK_AD_ENABLED, true)

    val isClickAdInterstitial: Boolean
        get() = getBoolean(KEY_IS_CLICK_AD_INTERSTITIAL, true)

    val isSwipeAdInterstitial: Boolean
        get() = getBoolean(KEY_IS_SWIPE_AD_INTERSTITIAL, true)

    fun getRightToLeftCount(): Int {
        val count = getInt(KEY_RIGHT_TO_LEFT_COUNT, 3)
        return if (count > 0) count else 3
    }

    fun getLeftToRightCount(): Int {
        val count = getInt(KEY_LEFT_TO_RIGHT_COUNT, 3)
        return if (count > 0) count else 3
    }

    fun getRightToLeftTrigger(): Int = getRightToLeftCount()
    fun getLeftToRightTrigger(): Int = getLeftToRightCount()

    fun getInterAdCounterTrigger(): Int {
        val count = getInt(KEY_INTER_AD_COUNTER_TRIGGER, 3)
        return if (count > 0) count else 3
    }

    fun getBackAdCounterTrigger(): Int {
        val count = getInt(KEY_INTER_AD_BACK_COUNTER_TRIGGER, 3)
        return if (count > 0) count else 3
    }

    fun getClickAdCounterTrigger(): Int {
        val count = getInt(KEY_CLICK_AD_COUNTER_TRIGGER, 3)
        return if (count > 0) count else 3
    }

    fun getGoogleSearchItemInterval(): Int {
        val interval = getInt(KEY_NATIVE_AD_GOOGLE_SEARCH_ITEM_INTERVAL, 2)
        return if (interval > 0) interval else 2
    }

    // Banner Screen Enables & IDs
    val bannerAdEnableSplash: Boolean get() = getBoolean(KEY_BANNER_AD_ENABLE_SPLASH, true)
    val bannerAdEnableHome: Boolean get() = getBoolean(KEY_BANNER_AD_ENABLE_HOME_SCREEN, false)
    val bannerAdEnableAppDrawer: Boolean get() = getBoolean(KEY_BANNER_AD_ENABLE_APP_DRAWER, true)
    val bannerAdEnableFindPhone: Boolean get() = getBoolean(KEY_BANNER_AD_ENABLE_FIND_PHONE, true)
    val bannerAdEnableAlertScreen: Boolean get() = getBoolean(KEY_BANNER_AD_ENABLE_ALERT_SCREEN, true)
    val bannerAdEnableAfterCall: Boolean get() = getBoolean(KEY_BANNER_AD_ENABLE_AFTER_CALL, true)
    val bannerAdEnableLanguageRect: Boolean get() = getBoolean(KEY_BANNER_AD_ENABLE_LANGUAGE_RECT, true)
    val bannerAdEnableOnboarding: Boolean get() = getBoolean(KEY_BANNER_AD_ENABLE_ONBOARDING, true)

    val bannerAdIdSplash: String get() = getString(KEY_BANNER_AD_ID_SPLASH, AdsConfig.DEFAULT_BANNER_ID)
    val bannerAdIdHome: String get() = getString(KEY_BANNER_AD_ID_HOME_SCREEN, AdsConfig.DEFAULT_BANNER_ID)
    val bannerAdIdAppDrawer: String get() = getString(KEY_BANNER_AD_ID_APP_DRAWER, AdsConfig.DEFAULT_BANNER_ID)
    val bannerAdIdFindPhone: String get() = getString(KEY_BANNER_AD_ID_FIND_PHONE, AdsConfig.DEFAULT_BANNER_ID)
    val bannerAdIdAlertScreen: String get() = getString(KEY_BANNER_AD_ID_ALERT_SCREEN, AdsConfig.DEFAULT_BANNER_ID)
    val bannerAdIdAfterCall: String get() = getString(KEY_BANNER_AD_ID_AFTER_CALL, AdsConfig.DEFAULT_BANNER_ID)
    val bannerAdIdLanguageRect: String get() = getString(KEY_BANNER_AD_ID_LANGUAGE_RECT, AdsConfig.DEFAULT_BANNER_ID)
    val bannerAdIdOnboarding: String get() = getString(KEY_BANNER_AD_ID_ONBOARDING, AdsConfig.DEFAULT_BANNER_ID)

    // Native Screen Enables & IDs
    val nativeAdEnableDashboard: Boolean get() = getBoolean(KEY_NATIVE_AD_ENABLE_DASHBOARD, true)
    val nativeAdEnableGoogleSearch: Boolean get() = getBoolean(KEY_NATIVE_AD_ENABLE_GOOGLE_SEARCH, true)
    val nativeAdEnableLanguage: Boolean get() = getBoolean(KEY_NATIVE_AD_ENABLE_LANGUAGE, true)
    val nativeAdEnableInstallUninstall: Boolean get() = getBoolean(KEY_NATIVE_AD_ENABLE_INSTALL_UNINSTALL, true)
    val nativeAdEnableAfterCall: Boolean get() = bannerAdEnableAfterCall

    val nativeAdIdDashboard: String get() = getString(KEY_NATIVE_AD_ID_DASHBOARD, AdsConfig.DEFAULT_NATIVE_ID)
    val nativeAdIdGoogleSearch: String get() = getString(KEY_NATIVE_AD_ID_GOOGLE_SEARCH, AdsConfig.DEFAULT_NATIVE_ID)
    val nativeAdIdLanguage: String get() = getString(KEY_NATIVE_AD_ID_LANGUAGE, AdsConfig.DEFAULT_NATIVE_ID)
    val nativeAdIdInstallUninstall: String get() = getString(KEY_NATIVE_AD_ID_INSTALL_UNINSTALL, AdsConfig.DEFAULT_NATIVE_ID)
    val nativeAdIdAfterCall: String get() = bannerAdIdAfterCall

    // Global Ad IDs & Preload
    val interAdEnableLanguage: Boolean get() = getBoolean(KEY_INTER_AD_ENABLE_LANGUAGE, true)
    val canShowInterLanguage: Boolean get() = isInterAdEnabled && interAdEnableLanguage && interAdId.isNotBlank()
    val interAdId: String get() = getString(KEY_INTER_AD_ID, AdsConfig.DEFAULT_INTER_ID)
    val appOpenAdId: String get() = getString(KEY_APP_OPEN_ID, AdsConfig.DEFAULT_APP_OPEN_ID)

    val preloadAdBanner: Boolean get() = getBoolean(KEY_PRELOAD_AD_BANNER, false)
    val preloadAdNative: Boolean get() = getBoolean(KEY_PRELOAD_AD_NATIVE, false)
    val preloadAdInterstitial: Boolean get() = getBoolean(KEY_PRELOAD_AD_INTERSTITIAL, false)
    val preloadAdAppOpen: Boolean get() = getBoolean(KEY_PRELOAD_AD_APP_OPEN, false)

    // Dynamic Policy & Feed URLs
    val privacyPolicyUrl: String get() = getString(KEY_PRIVACY_POLICY_URL, DEFAULT_PRIVACY_POLICY_URL)
    val termsAndConditionsUrl: String get() = getString(KEY_TERMS_AND_CONDITIONS_URL, DEFAULT_TERMS_AND_CONDITIONS_URL)
    val feedlistConfigJson: String get() = getString(KEY_FEEDLIST_CONFIG, DEFAULT_FEEDLIST_CONFIG_JSON)
}
