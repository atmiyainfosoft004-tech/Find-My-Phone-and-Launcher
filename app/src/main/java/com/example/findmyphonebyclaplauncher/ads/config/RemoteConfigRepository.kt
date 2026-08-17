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
    const val KEY_IS_BACK_AD_ENABLED = "is_back_ad_enabled"
    const val KEY_IS_CLICK_AD_INTERSTITIAL = "is_click_ad_interstitial"
    const val KEY_IS_SWIPE_AD_INTERSTITIAL = "is_swipe_ad_interstitial"

    // Trigger Counters
    const val KEY_INTER_AD_COUNTER_TRIGGER = "inter_ad_counter_trigger"
    const val KEY_INTER_AD_BACK_COUNTER_TRIGGER = "inter_ad_back_counter_trigger"
    const val KEY_CLICK_AD_COUNTER_TRIGGER = "click_ad_counter_trigger"
    const val KEY_NATIVE_AD_GOOGLE_SEARCH_ITEM_INTERVAL = "native_ad_google_search_item_interval"

    // Banner Screen Enables & IDs
    const val KEY_BANNER_AD_ENABLE_SPLASH = "banner_ad_enable_splash"
    const val KEY_BANNER_AD_ENABLE_HOME_SCREEN = "banner_ad_enable_home_screen"
    const val KEY_BANNER_AD_ENABLE_APP_DRAWER = "banner_ad_enable_app_drawer"
    const val KEY_BANNER_AD_ENABLE_FIND_PHONE = "banner_ad_enable_find_phone"
    const val KEY_BANNER_AD_ENABLE_ALERT_SCREEN = "banner_ad_enable_alert_screen"

    const val KEY_BANNER_AD_ID_SPLASH = "banner_ad_id_splash"
    const val KEY_BANNER_AD_ID_HOME_SCREEN = "banner_ad_id_home_screen"
    const val KEY_BANNER_AD_ID_APP_DRAWER = "banner_ad_id_app_drawer"
    const val KEY_BANNER_AD_ID_FIND_PHONE = "banner_ad_id_find_phone"
    const val KEY_BANNER_AD_ID_ALERT_SCREEN = "banner_ad_id_alert_screen"

    // Native Screen Enables & IDs
    const val KEY_NATIVE_AD_ENABLE_DASHBOARD = "native_ad_enable_dashboard"
    const val KEY_NATIVE_AD_ENABLE_GOOGLE_SEARCH = "native_ad_enable_google_search"
    const val KEY_NATIVE_AD_ENABLE_LANGUAGE = "native_ad_enable_language"
    const val KEY_NATIVE_AD_ENABLE_AFTER_CALL = "native_ad_enable_after_call"

    const val KEY_NATIVE_AD_ID_DASHBOARD = "native_ad_id_dashboard"
    const val KEY_NATIVE_AD_ID_GOOGLE_SEARCH = "native_ad_id_google_search"
    const val KEY_NATIVE_AD_ID_LANGUAGE = "native_ad_id_language"
    const val KEY_NATIVE_AD_ID_AFTER_CALL = "native_ad_id_after_call"

    // Global Ad IDs & Preload Flags
    const val KEY_INTER_AD_ID = "inter_ad_id"
    const val KEY_APP_OPEN_ID = "app_open_ad_id"
    const val KEY_APP_OPEN_AD_ID = "app_open_ad_id"
    const val KEY_PRELOAD_AD_BANNER = "preload_ad_banner"
    const val KEY_PRELOAD_AD_NATIVE = "preload_ad_native"
    const val KEY_PRELOAD_AD_INTERSTITIAL = "preload_ad_interstitial"
    const val KEY_PRELOAD_AD_APP_OPEN = "preload_ad_app_open"

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
                KEY_IS_BACK_AD_ENABLED to true,
                KEY_IS_CLICK_AD_INTERSTITIAL to true,
                KEY_IS_SWIPE_AD_INTERSTITIAL to true,
                KEY_INTER_AD_COUNTER_TRIGGER to 3,
                KEY_INTER_AD_BACK_COUNTER_TRIGGER to 3,
                KEY_CLICK_AD_COUNTER_TRIGGER to 3,
                KEY_NATIVE_AD_GOOGLE_SEARCH_ITEM_INTERVAL to 2,
                KEY_BANNER_AD_ENABLE_SPLASH to true,
                KEY_BANNER_AD_ENABLE_HOME_SCREEN to false,
                KEY_BANNER_AD_ENABLE_APP_DRAWER to true,
                KEY_BANNER_AD_ENABLE_FIND_PHONE to true,
                KEY_BANNER_AD_ENABLE_ALERT_SCREEN to true,
                KEY_BANNER_AD_ID_SPLASH to AdsConfig.DEFAULT_BANNER_ID,
                KEY_BANNER_AD_ID_HOME_SCREEN to AdsConfig.DEFAULT_BANNER_ID,
                KEY_BANNER_AD_ID_APP_DRAWER to AdsConfig.DEFAULT_BANNER_ID,
                KEY_BANNER_AD_ID_FIND_PHONE to AdsConfig.DEFAULT_BANNER_ID,
                KEY_BANNER_AD_ID_ALERT_SCREEN to AdsConfig.DEFAULT_BANNER_ID,
                KEY_NATIVE_AD_ENABLE_DASHBOARD to true,
                KEY_NATIVE_AD_ENABLE_GOOGLE_SEARCH to true,
                KEY_NATIVE_AD_ENABLE_LANGUAGE to true,
                KEY_NATIVE_AD_ENABLE_AFTER_CALL to true,
                KEY_NATIVE_AD_ID_DASHBOARD to AdsConfig.DEFAULT_NATIVE_ID,
                KEY_NATIVE_AD_ID_GOOGLE_SEARCH to AdsConfig.DEFAULT_NATIVE_ID,
                KEY_NATIVE_AD_ID_LANGUAGE to AdsConfig.DEFAULT_NATIVE_ID,
                KEY_NATIVE_AD_ID_AFTER_CALL to AdsConfig.DEFAULT_NATIVE_ID,
                KEY_INTER_AD_ID to AdsConfig.DEFAULT_INTER_ID,
                KEY_APP_OPEN_ID to AdsConfig.DEFAULT_APP_OPEN_ID,
                KEY_PRELOAD_AD_BANNER to false,
                KEY_PRELOAD_AD_NATIVE to false,
                KEY_PRELOAD_AD_INTERSTITIAL to false,
                KEY_PRELOAD_AD_APP_OPEN to false
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

    val isBackAdEnabled: Boolean
        get() = getBoolean(KEY_IS_BACK_AD_ENABLED, true)

    val isClickAdInterstitial: Boolean
        get() = getBoolean(KEY_IS_CLICK_AD_INTERSTITIAL, true)

    val isSwipeAdInterstitial: Boolean
        get() = getBoolean(KEY_IS_SWIPE_AD_INTERSTITIAL, true)

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

    val bannerAdIdSplash: String get() = getString(KEY_BANNER_AD_ID_SPLASH, AdsConfig.DEFAULT_BANNER_ID)
    val bannerAdIdHome: String get() = getString(KEY_BANNER_AD_ID_HOME_SCREEN, AdsConfig.DEFAULT_BANNER_ID)
    val bannerAdIdAppDrawer: String get() = getString(KEY_BANNER_AD_ID_APP_DRAWER, AdsConfig.DEFAULT_BANNER_ID)
    val bannerAdIdFindPhone: String get() = getString(KEY_BANNER_AD_ID_FIND_PHONE, AdsConfig.DEFAULT_BANNER_ID)
    val bannerAdIdAlertScreen: String get() = getString(KEY_BANNER_AD_ID_ALERT_SCREEN, AdsConfig.DEFAULT_BANNER_ID)

    // Native Screen Enables & IDs
    val nativeAdEnableDashboard: Boolean get() = getBoolean(KEY_NATIVE_AD_ENABLE_DASHBOARD, true)
    val nativeAdEnableGoogleSearch: Boolean get() = getBoolean(KEY_NATIVE_AD_ENABLE_GOOGLE_SEARCH, true)
    val nativeAdEnableLanguage: Boolean get() = getBoolean(KEY_NATIVE_AD_ENABLE_LANGUAGE, true)
    val nativeAdEnableAfterCall: Boolean get() = getBoolean(KEY_NATIVE_AD_ENABLE_AFTER_CALL, true)

    val nativeAdIdDashboard: String get() = getString(KEY_NATIVE_AD_ID_DASHBOARD, AdsConfig.DEFAULT_NATIVE_ID)
    val nativeAdIdGoogleSearch: String get() = getString(KEY_NATIVE_AD_ID_GOOGLE_SEARCH, AdsConfig.DEFAULT_NATIVE_ID)
    val nativeAdIdLanguage: String get() = getString(KEY_NATIVE_AD_ID_LANGUAGE, AdsConfig.DEFAULT_NATIVE_ID)
    val nativeAdIdAfterCall: String get() = getString(KEY_NATIVE_AD_ID_AFTER_CALL, AdsConfig.DEFAULT_NATIVE_ID)

    // Global Ad IDs & Preload
    val interAdId: String get() = getString(KEY_INTER_AD_ID, AdsConfig.DEFAULT_INTER_ID)
    val appOpenAdId: String get() = getString(KEY_APP_OPEN_ID, AdsConfig.DEFAULT_APP_OPEN_ID)

    val preloadAdBanner: Boolean get() = getBoolean(KEY_PRELOAD_AD_BANNER, false)
    val preloadAdNative: Boolean get() = getBoolean(KEY_PRELOAD_AD_NATIVE, false)
    val preloadAdInterstitial: Boolean get() = getBoolean(KEY_PRELOAD_AD_INTERSTITIAL, false)
    val preloadAdAppOpen: Boolean get() = getBoolean(KEY_PRELOAD_AD_APP_OPEN, false)
}
