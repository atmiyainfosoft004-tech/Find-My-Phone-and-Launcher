package com.example.findmyphonebyclaplauncher.ads.config

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.findmyphonebyclaplauncher.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.gson.Gson
import java.util.concurrent.CopyOnWriteArraySet

object AdsConfigManager {

    private const val TAG = "RemoteConfig"
    const val KEY_ADS_CONFIG = "ads_config"

    // System & Feature Flags
    const val KEY_SYSTEM_HIDE_NAVIGATION_BAR_AUTO = "system_hide_navigation_bar_auto"

    // Global Ad Type Switches (Master Toggles)
    const val KEY_IS_BANNER_AD_ENABLED = "is_banner_ad_enabled"
    const val KEY_IS_NATIVE_AD_ENABLED = "is_native_ad_enabled"
    const val KEY_IS_INTER_AD_ENABLED = "is_inter_ad_enabled"
    const val KEY_IS_APP_OPEN_AD_ENABLED = "is_app_open_ad_enabled"

    // Action-Level Ad Switches
    const val KEY_IS_CLICK_AD_ENABLED = "is_click_ad_enabled"
    const val KEY_IS_SWIPE_AD_ENABLED = "is_swipe_ad_enabled"
    const val KEY_IS_BACK_AD_ENABLED = "is_back_ad_enabled"

    // Action Ad Format Selectors (true = Interstitial | false = App Open)
    const val KEY_IS_CLICK_AD_INTERSTITIAL = "is_click_ad_interstitial"
    const val KEY_IS_SWIPE_AD_INTERSTITIAL = "is_swipe_ad_interstitial"
    const val KEY_APP_CLICK_INTER_LEGACY = "isAppClickInterOn"
    const val KEY_SWIPE_INTER_LEGACY = "isRightLeftSwipeInterOn"

    // Counters & Triggers
    const val KEY_INTER_COUNTER_TRIGGER = "inter_ad_counter_trigger"
    const val KEY_INTER_BACK_COUNTER_TRIGGER = "inter_ad_back_counter_trigger"
    const val KEY_CLICK_AD_COUNTER_TRIGGER = "click_ad_counter_trigger"
    const val KEY_INTER_COUNT_LEGACY = "inter_count"
    const val KEY_INTER_BACK_COUNT_LEGACY = "inter_back_count"

    // Banner Ads - Screen Specific Enable/Disable
    const val KEY_BANNER_ENABLE_SPLASH = "banner_ad_enable_splash"
    const val KEY_BANNER_ENABLE_HOME_SCREEN = "banner_ad_enable_home_screen"
    const val KEY_BANNER_ENABLE_HOME = "banner_ad_enable_home"
    const val KEY_BANNER_ENABLE_CONTACT_HOME_LEGACY = "banner_ad_enable_contact_home"
    const val KEY_BANNER_ENABLE_APP_DRAWER = "banner_ad_enable_app_drawer"
    const val KEY_BANNER_ENABLE_FIND_PHONE = "banner_ad_enable_find_phone"
    const val KEY_BANNER_ENABLE_ALERT_SCREEN = "banner_ad_enable_alert_screen"
    const val KEY_BANNER_ENABLE_ALERT_ACTIVITY_LEGACY = "banner_ad_enable_alert_activity"

    // Banner Ads - Unit IDs
    const val KEY_BANNER_ID_SPLASH = "banner_ad_id_splash"
    const val KEY_BANNER_ID_HOME_SCREEN = "banner_ad_id_home_screen"
    const val KEY_BANNER_ID_HOME = "banner_ad_id_home"
    const val KEY_BANNER_ID_CONTACT_HOME_LEGACY = "banner_ad_id_contact_home"
    const val KEY_BANNER_ID_APP_DRAWER = "banner_ad_id_app_drawer"
    const val KEY_BANNER_ID_FIND_PHONE = "banner_ad_id_find_phone"
    const val KEY_BANNER_ID_ALERT_SCREEN = "banner_ad_id_alert_screen"
    const val KEY_BANNER_ID_ALERT_ACTIVITY_LEGACY = "banner_ad_id_alert_activity"

    // Native Ads - Screen Specific Enable/Disable
    const val KEY_NATIVE_ENABLE_DASHBOARD = "native_ad_enable_dashboard"
    const val KEY_NATIVE_ENABLE_GOOGLE_SEARCH = "native_ad_enable_google_search"
    const val KEY_NATIVE_ENABLE_LANGUAGE = "native_ad_enable_language"
    const val KEY_NATIVE_ENABLE_AFTER_CALL = "native_ad_enable_after_call"

    // Native Ads - Unit IDs & Configuration
    const val KEY_NATIVE_ID_DASHBOARD = "native_ad_id_dashboard"
    const val KEY_NATIVE_ID_GOOGLE_SEARCH = "native_ad_id_google_search"
    const val KEY_NATIVE_ID_LANGUAGE = "native_ad_id_language"
    const val KEY_NATIVE_ID_AFTER_CALL = "native_ad_id_after_call"
    const val KEY_NATIVE_GOOGLE_SEARCH_ITEM_INTERVAL = "native_ad_google_search_item_interval"

    // Interstitial & Open Ad Controls
    const val KEY_INTER_ID = "inter_ad_id"
    const val KEY_APP_OPEN_ID = "app_open_ad_id"

    // Preload Configurations
    const val KEY_PRELOAD_AD_BANNER = "preload_ad_banner"
    const val KEY_PRELOAD_AD_NATIVE = "preload_ad_native"
    const val KEY_PRELOAD_AD_INTERSTITIAL = "preload_ad_interstitial"
    const val KEY_PRELOAD_AD_APP_OPEN = "preload_ad_app_open"
    const val KEY_BANNER_PRELOAD_LEGACY = "bannerAdPreload"
    const val KEY_NATIVE_PRELOAD_LEGACY = "nativeAdPreload"
    const val KEY_INTER_PRELOAD_LEGACY = "interAdPreload"
    const val KEY_APP_OPEN_PRELOAD_LEGACY = "appOpenAdPreload"

    private val gson = Gson()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val listeners = CopyOnWriteArraySet<OnConfigChangeListener>()

    fun interface OnConfigChangeListener {
        fun onConfigChanged(config: AdsConfig)
    }

    fun addConfigChangeListener(listener: OnConfigChangeListener) {
        listeners.add(listener)
    }

    fun removeConfigChangeListener(listener: OnConfigChangeListener) {
        listeners.remove(listener)
    }

    private fun notifyListeners() {
        val currentConfig = config
        mainHandler.post {
            listeners.forEach { it.onConfigChanged(currentConfig) }
        }
    }

    @Volatile
    var config: AdsConfig = AdsConfig.DEFAULT
        private set

    @Volatile
    var isInitialized: Boolean = false
        private set

    val isBannerAdEnabled: Boolean get() = config.isBannerAdEnabled
    val isNativeAdEnabled: Boolean get() = config.isNativeAdEnabled
    val isInterAdEnabled: Boolean get() = config.isInterAdEnabled
    val isAppOpenAdEnabled: Boolean get() = config.isAppOpenAdEnabled

    val isClickAdEnabled: Boolean get() = config.isClickAdEnabled
    val isSwipeAdEnabled: Boolean get() = config.isSwipeAdEnabled
    val isBackAdEnabled: Boolean get() = config.isBackAdEnabled

    val isClickAdInterstitial: Boolean get() = config.isClickAdInterstitial
    val isSwipeAdInterstitial: Boolean get() = config.isSwipeAdInterstitial

    fun initialize(context: Context, onComplete: ((success: Boolean) -> Unit)? = null) {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
        val remoteConfig = FirebaseRemoteConfig.getInstance()
        val minimumFetchInterval = if (BuildConfig.DEBUG) 0L else 3600L
        val settings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(minimumFetchInterval)
            .build()
        remoteConfig.setConfigSettingsAsync(settings)

        val defaultMap: Map<String, Any> = mapOf(
            KEY_ADS_CONFIG to gson.toJson(AdsConfig.DEFAULT),
            KEY_SYSTEM_HIDE_NAVIGATION_BAR_AUTO to true,

            KEY_IS_BANNER_AD_ENABLED to true,
            KEY_IS_NATIVE_AD_ENABLED to true,
            KEY_IS_INTER_AD_ENABLED to true,
            KEY_IS_APP_OPEN_AD_ENABLED to true,

            KEY_IS_CLICK_AD_ENABLED to true,
            KEY_IS_SWIPE_AD_ENABLED to true,
            KEY_IS_BACK_AD_ENABLED to true,

            KEY_IS_CLICK_AD_INTERSTITIAL to true,
            KEY_IS_SWIPE_AD_INTERSTITIAL to true,

            KEY_INTER_COUNTER_TRIGGER to 3,
            KEY_INTER_BACK_COUNTER_TRIGGER to 3,
            KEY_CLICK_AD_COUNTER_TRIGGER to 3,

            KEY_BANNER_ENABLE_SPLASH to true,
            KEY_BANNER_ENABLE_HOME_SCREEN to false,
            KEY_BANNER_ENABLE_APP_DRAWER to true,
            KEY_BANNER_ENABLE_FIND_PHONE to true,
            KEY_BANNER_ENABLE_ALERT_SCREEN to true,

            KEY_BANNER_ID_SPLASH to AdsConfig.DEFAULT_BANNER_ID,
            KEY_BANNER_ID_HOME_SCREEN to AdsConfig.DEFAULT_BANNER_ID,
            KEY_BANNER_ID_APP_DRAWER to AdsConfig.DEFAULT_BANNER_ID,
            KEY_BANNER_ID_FIND_PHONE to AdsConfig.DEFAULT_BANNER_ID,
            KEY_BANNER_ID_ALERT_SCREEN to AdsConfig.DEFAULT_BANNER_ID,

            KEY_NATIVE_ENABLE_DASHBOARD to true,
            KEY_NATIVE_ENABLE_GOOGLE_SEARCH to true,
            KEY_NATIVE_ENABLE_LANGUAGE to true,
            KEY_NATIVE_ENABLE_AFTER_CALL to true,

            KEY_NATIVE_ID_DASHBOARD to AdsConfig.DEFAULT_NATIVE_ID,
            KEY_NATIVE_ID_GOOGLE_SEARCH to AdsConfig.DEFAULT_NATIVE_ID,
            KEY_NATIVE_ID_LANGUAGE to AdsConfig.DEFAULT_NATIVE_ID,
            KEY_NATIVE_ID_AFTER_CALL to AdsConfig.DEFAULT_NATIVE_ID,
            KEY_NATIVE_GOOGLE_SEARCH_ITEM_INTERVAL to 2,

            KEY_INTER_ID to AdsConfig.DEFAULT_INTER_ID,
            KEY_APP_OPEN_ID to AdsConfig.DEFAULT_APP_OPEN_ID,

            KEY_PRELOAD_AD_BANNER to false,
            KEY_PRELOAD_AD_NATIVE to false,
            KEY_PRELOAD_AD_INTERSTITIAL to false,
            KEY_PRELOAD_AD_APP_OPEN to false
        )
        remoteConfig.setDefaultsAsync(defaultMap)

        applyFrom(remoteConfig)
        isInitialized = true
        Log.d(TAG, "AdsConfigManager initialized (minimumFetchInterval=$minimumFetchInterval s)")

        // Real-time listener for dynamic Remote Config updates
        remoteConfig.addOnConfigUpdateListener(object : ConfigUpdateListener {
            override fun onUpdate(configUpdate: ConfigUpdate) {
                Log.d(TAG, "Real-time Remote Config update received for keys: ${configUpdate.updatedKeys}")
                remoteConfig.activate().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        applyFrom(remoteConfig)
                        Log.d(TAG, "Real-time Remote Config activated. Updated is_click_ad_interstitial=${config.isClickAdInterstitial}, is_swipe_ad_interstitial=${config.isSwipeAdInterstitial}")
                    } else {
                        Log.e(TAG, "Failed to activate real-time Remote Config", task.exception)
                    }
                }
            }

            override fun onError(error: FirebaseRemoteConfigException) {
                Log.e(TAG, "Real-time Remote Config update error: ${error.message}", error)
            }
        })

        // On-start fetch and activate
        fetchAndActivate(onComplete)
    }

    fun fetchAndActivate(onComplete: ((success: Boolean) -> Unit)? = null) {
        val remoteConfig = FirebaseRemoteConfig.getInstance()
        val minimumFetchInterval = if (BuildConfig.DEBUG) 0L else 3600L
        val settings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(minimumFetchInterval)
            .build()
        remoteConfig.setConfigSettingsAsync(settings)

        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                applyFrom(remoteConfig)
                Log.d(TAG, "fetchAndActivate SUCCESS. Active is_click_ad_interstitial=${config.isClickAdInterstitial}, is_swipe_ad_interstitial=${config.isSwipeAdInterstitial}")
                Log.d(TAG, "Active config: $config")
            } else {
                Log.e(TAG, "fetchAndActivate FAILED", task.exception)
            }
            onComplete?.invoke(task.isSuccessful)
        }
    }

    fun refresh(onComplete: ((success: Boolean) -> Unit)? = null) {
        fetchAndActivate(onComplete)
    }

    private fun applyFrom(remoteConfig: FirebaseRemoteConfig) {
        val newConfig = getActiveAdsConfig(remoteConfig)
        config = newConfig
        Log.d(TAG, "applyFrom: Successfully updated active AdsConfig instance.")
        Log.d(TAG, "is_click_ad_interstitial=${newConfig.isClickAdInterstitial} (Format: ${if (newConfig.isClickAdInterstitial) "Interstitial" else "App Open"})")
        Log.d(TAG, "is_swipe_ad_interstitial=${newConfig.isSwipeAdInterstitial} (Format: ${if (newConfig.isSwipeAdInterstitial) "Interstitial" else "App Open"})")
        Log.d(TAG, "is_click_ad_enabled=${newConfig.isClickAdEnabled}, is_swipe_ad_enabled=${newConfig.isSwipeAdEnabled}")
        Log.d(TAG, "click_ad_counter_trigger=${newConfig.clickAdCounterTrigger}, inter_ad_counter_trigger=${newConfig.interAdCounterTrigger}, inter_ad_back_counter_trigger=${newConfig.interAdBackCounterTrigger}")
        notifyListeners()
    }

    fun getActiveAdsConfig(remoteConfig: FirebaseRemoteConfig): AdsConfig {
        val json = remoteConfig.getString(KEY_ADS_CONFIG)
        val fromJson = if (json.isNotBlank()) {
            runCatching { gson.fromJson(json, AdsConfig::class.java) }.getOrNull()
        } else null

        val parsed = AdsConfig(
            systemHideNavigationBarAuto = fromJson?.systemHideNavigationBarAuto
                ?: remoteConfig.extractBoolean(KEY_SYSTEM_HIDE_NAVIGATION_BAR_AUTO, default = true),

            isBannerAdEnabled = fromJson?.isBannerAdEnabled
                ?: remoteConfig.extractBoolean(KEY_IS_BANNER_AD_ENABLED, default = true),
            isNativeAdEnabled = fromJson?.isNativeAdEnabled
                ?: remoteConfig.extractBoolean(KEY_IS_NATIVE_AD_ENABLED, default = true),
            isInterAdEnabled = fromJson?.isInterAdEnabled
                ?: remoteConfig.extractBoolean(KEY_IS_INTER_AD_ENABLED, default = true),
            isAppOpenAdEnabled = fromJson?.isAppOpenAdEnabled
                ?: remoteConfig.extractBoolean(KEY_IS_APP_OPEN_AD_ENABLED, default = true),

            isClickAdEnabled = fromJson?.isClickAdEnabled
                ?: remoteConfig.extractBoolean(KEY_IS_CLICK_AD_ENABLED, default = true),
            isSwipeAdEnabled = fromJson?.isSwipeAdEnabled
                ?: remoteConfig.extractBoolean(KEY_IS_SWIPE_AD_ENABLED, default = true),
            isBackAdEnabled = fromJson?.isBackAdEnabled
                ?: remoteConfig.extractBoolean(KEY_IS_BACK_AD_ENABLED, default = true),

            isClickAdInterstitial = fromJson?.isClickAdInterstitial
                ?: remoteConfig.extractBoolean(KEY_IS_CLICK_AD_INTERSTITIAL, KEY_APP_CLICK_INTER_LEGACY, default = true),
            isSwipeAdInterstitial = fromJson?.isSwipeAdInterstitial
                ?: remoteConfig.extractBoolean(KEY_IS_SWIPE_AD_INTERSTITIAL, KEY_SWIPE_INTER_LEGACY, default = true),

            interAdCounterTrigger = fromJson?.interAdCounterTrigger
                ?: remoteConfig.extractInt(KEY_INTER_COUNTER_TRIGGER, KEY_INTER_COUNT_LEGACY, default = 3).coerceAtLeast(1),
            interAdBackCounterTrigger = fromJson?.interAdBackCounterTrigger
                ?: remoteConfig.extractInt(KEY_INTER_BACK_COUNTER_TRIGGER, KEY_INTER_BACK_COUNT_LEGACY, default = 3).let { if (it > 0) it else 3 },
            clickAdCounterTrigger = fromJson?.clickAdCounterTrigger
                ?: remoteConfig.extractInt(KEY_CLICK_AD_COUNTER_TRIGGER, default = 3).let { if (it > 0) it else 3 },

            bannerAdEnableSplash = fromJson?.bannerAdEnableSplash
                ?: remoteConfig.extractBoolean(KEY_BANNER_ENABLE_SPLASH, default = true),
            bannerAdEnableHome = fromJson?.bannerAdEnableHome
                ?: remoteConfig.extractBoolean(KEY_BANNER_ENABLE_HOME_SCREEN, KEY_BANNER_ENABLE_HOME, KEY_BANNER_ENABLE_CONTACT_HOME_LEGACY, default = false),
            bannerAdEnableAppDrawer = fromJson?.bannerAdEnableAppDrawer
                ?: remoteConfig.extractBoolean(KEY_BANNER_ENABLE_APP_DRAWER, default = true),
            bannerAdEnableFindPhone = fromJson?.bannerAdEnableFindPhone
                ?: remoteConfig.extractBoolean(KEY_BANNER_ENABLE_FIND_PHONE, default = true),
            bannerAdEnableAlertScreen = fromJson?.bannerAdEnableAlertScreen
                ?: remoteConfig.extractBoolean(KEY_BANNER_ENABLE_ALERT_SCREEN, KEY_BANNER_ENABLE_ALERT_ACTIVITY_LEGACY, default = true),

            bannerAdIdSplash = fromJson?.bannerAdIdSplash
                ?: remoteConfig.extractString(KEY_BANNER_ID_SPLASH, default = AdsConfig.DEFAULT_BANNER_ID),
            bannerAdIdHome = fromJson?.bannerAdIdHome
                ?: remoteConfig.extractString(KEY_BANNER_ID_HOME_SCREEN, KEY_BANNER_ID_HOME, KEY_BANNER_ID_CONTACT_HOME_LEGACY, default = AdsConfig.DEFAULT_BANNER_ID),
            bannerAdIdAppDrawer = fromJson?.bannerAdIdAppDrawer
                ?: remoteConfig.extractString(KEY_BANNER_ID_APP_DRAWER, default = AdsConfig.DEFAULT_BANNER_ID),
            bannerAdIdFindPhone = fromJson?.bannerAdIdFindPhone
                ?: remoteConfig.extractString(KEY_BANNER_ID_FIND_PHONE, default = AdsConfig.DEFAULT_BANNER_ID),
            bannerAdIdAlertScreen = fromJson?.bannerAdIdAlertScreen
                ?: remoteConfig.extractString(KEY_BANNER_ID_ALERT_SCREEN, KEY_BANNER_ID_ALERT_ACTIVITY_LEGACY, default = AdsConfig.DEFAULT_BANNER_ID),

            nativeAdEnableDashboard = fromJson?.nativeAdEnableDashboard
                ?: remoteConfig.extractBoolean(KEY_NATIVE_ENABLE_DASHBOARD, default = true),
            nativeAdEnableGoogleSearch = fromJson?.nativeAdEnableGoogleSearch
                ?: remoteConfig.extractBoolean(KEY_NATIVE_ENABLE_GOOGLE_SEARCH, default = true),
            nativeAdEnableLanguage = fromJson?.nativeAdEnableLanguage
                ?: remoteConfig.extractBoolean(KEY_NATIVE_ENABLE_LANGUAGE, default = true),
            nativeAdEnableAfterCall = fromJson?.nativeAdEnableAfterCall
                ?: remoteConfig.extractBoolean(KEY_NATIVE_ENABLE_AFTER_CALL, default = true),

            nativeAdIdDashboard = fromJson?.nativeAdIdDashboard
                ?: remoteConfig.extractString(KEY_NATIVE_ID_DASHBOARD, default = AdsConfig.DEFAULT_NATIVE_ID),
            nativeAdIdGoogleSearch = fromJson?.nativeAdIdGoogleSearch
                ?: remoteConfig.extractString(KEY_NATIVE_ID_GOOGLE_SEARCH, default = AdsConfig.DEFAULT_NATIVE_ID),
            nativeAdIdLanguage = fromJson?.nativeAdIdLanguage
                ?: remoteConfig.extractString(KEY_NATIVE_ID_LANGUAGE, default = AdsConfig.DEFAULT_NATIVE_ID),
            nativeAdIdAfterCall = fromJson?.nativeAdIdAfterCall
                ?: remoteConfig.extractString(KEY_NATIVE_ID_AFTER_CALL, default = AdsConfig.DEFAULT_NATIVE_ID),
            nativeAdGoogleSearchItemInterval = fromJson?.nativeAdGoogleSearchItemInterval
                ?: remoteConfig.extractInt(KEY_NATIVE_GOOGLE_SEARCH_ITEM_INTERVAL, default = 2).coerceAtLeast(1),

            interAdId = fromJson?.interAdId
                ?: remoteConfig.extractString(KEY_INTER_ID, default = AdsConfig.DEFAULT_INTER_ID),
            appOpenAdId = fromJson?.appOpenAdId
                ?: remoteConfig.extractString(KEY_APP_OPEN_ID, default = AdsConfig.DEFAULT_APP_OPEN_ID),

            preloadAdBanner = fromJson?.preloadAdBanner
                ?: remoteConfig.extractBoolean(KEY_PRELOAD_AD_BANNER, KEY_BANNER_PRELOAD_LEGACY, default = false),
            preloadAdNative = fromJson?.preloadAdNative
                ?: remoteConfig.extractBoolean(KEY_PRELOAD_AD_NATIVE, KEY_NATIVE_PRELOAD_LEGACY, default = false),
            preloadAdInterstitial = fromJson?.preloadAdInterstitial
                ?: remoteConfig.extractBoolean(KEY_PRELOAD_AD_INTERSTITIAL, KEY_INTER_PRELOAD_LEGACY, default = false),
            preloadAdAppOpen = fromJson?.preloadAdAppOpen
                ?: remoteConfig.extractBoolean(KEY_PRELOAD_AD_APP_OPEN, KEY_APP_OPEN_PRELOAD_LEGACY, default = false),
        )

        return parsed
    }

    private fun parse(remoteConfig: FirebaseRemoteConfig): AdsConfig {
        return getActiveAdsConfig(remoteConfig)
    }

    private fun FirebaseRemoteConfig.extractBoolean(vararg keys: String, default: Boolean): Boolean {
        for (key in keys) {
            val value = getValue(key)
            if (value.source != FirebaseRemoteConfig.VALUE_SOURCE_STATIC) {
                val str = value.asString().trim()
                if (str.equals("true", ignoreCase = true)) return true
                if (str.equals("false", ignoreCase = true)) return false
                return value.asBoolean()
            }
        }
        return default
    }

    private fun FirebaseRemoteConfig.extractInt(vararg keys: String, default: Int): Int {
        for (key in keys) {
            val value = getValue(key)
            if (value.source != FirebaseRemoteConfig.VALUE_SOURCE_STATIC) {
                return value.asString().trim().toIntOrNull() ?: value.asLong().toInt()
            }
        }
        return default
    }

    private fun FirebaseRemoteConfig.extractString(vararg keys: String, default: String): String {
        for (key in keys) {
            val value = getValue(key)
            if (value.source != FirebaseRemoteConfig.VALUE_SOURCE_STATIC) {
                val str = value.asString().trim()
                if (str.isNotBlank()) return str
            }
        }
        return default
    }
}
