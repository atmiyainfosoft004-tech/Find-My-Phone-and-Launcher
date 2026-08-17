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

            KEY_IS_CLICK_AD_ENABLED to false,
            KEY_IS_SWIPE_AD_ENABLED to false,
            KEY_IS_BACK_AD_ENABLED to true,

            KEY_IS_CLICK_AD_INTERSTITIAL to true,
            KEY_IS_SWIPE_AD_INTERSTITIAL to false,

            KEY_INTER_COUNTER_TRIGGER to 3,
            KEY_INTER_BACK_COUNTER_TRIGGER to 1,
            KEY_CLICK_AD_COUNTER_TRIGGER to 3,

            KEY_BANNER_ENABLE_SPLASH to true,
            KEY_BANNER_ENABLE_HOME_SCREEN to true,
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
                Log.d(TAG, "Real-time config update received for keys: ${configUpdate.updatedKeys}")
                remoteConfig.activate().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        applyFrom(remoteConfig)
                        Log.d(TAG, "Real-time Remote Config activated successfully. Active config: $config")
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
        remoteConfig.fetch(minimumFetchInterval).addOnCompleteListener { fetchTask ->
            if (fetchTask.isSuccessful) {
                remoteConfig.activate().addOnCompleteListener { activateTask ->
                    if (activateTask.isSuccessful) {
                        applyFrom(remoteConfig)
                        Log.d(TAG, "fetchAndActivate SUCCESS. Active config: $config")
                    } else {
                        Log.e(TAG, "fetchAndActivate -> activate FAILED", activateTask.exception)
                    }
                    onComplete?.invoke(activateTask.isSuccessful)
                }
            } else {
                Log.e(TAG, "fetchAndActivate -> fetch FAILED", fetchTask.exception)
                onComplete?.invoke(false)
            }
        }
    }

    fun refresh(onComplete: ((success: Boolean) -> Unit)? = null) {
        fetchAndActivate(onComplete)
    }

    private fun applyFrom(remoteConfig: FirebaseRemoteConfig) {
        config = parse(remoteConfig)
        notifyListeners()
    }

    private fun parse(remoteConfig: FirebaseRemoteConfig): AdsConfig {
        val json = remoteConfig.getString(KEY_ADS_CONFIG)
        val fromJson = runCatching {
            if (json.isBlank()) AdsConfig.DEFAULT
            else gson.fromJson(json, AdsConfig::class.java) ?: AdsConfig.DEFAULT
        }.getOrElse { error ->
            Log.e(TAG, "Invalid $KEY_ADS_CONFIG JSON string: '$json'", error)
            AdsConfig.DEFAULT
        }

        return fromJson.copy(
            systemHideNavigationBarAuto = remoteConfig.optionalBoolean(KEY_SYSTEM_HIDE_NAVIGATION_BAR_AUTO)
                ?: fromJson.systemHideNavigationBarAuto,

            isBannerAdEnabled = remoteConfig.optionalBoolean(KEY_IS_BANNER_AD_ENABLED)
                ?: fromJson.isBannerAdEnabled,
            isNativeAdEnabled = remoteConfig.optionalBoolean(KEY_IS_NATIVE_AD_ENABLED)
                ?: fromJson.isNativeAdEnabled,
            isInterAdEnabled = remoteConfig.optionalBoolean(KEY_IS_INTER_AD_ENABLED)
                ?: fromJson.isInterAdEnabled,
            isAppOpenAdEnabled = remoteConfig.optionalBoolean(KEY_IS_APP_OPEN_AD_ENABLED)
                ?: fromJson.isAppOpenAdEnabled,

            isClickAdEnabled = remoteConfig.optionalBoolean(KEY_IS_CLICK_AD_ENABLED)
                ?: fromJson.isClickAdEnabled,
            isSwipeAdEnabled = remoteConfig.optionalBoolean(KEY_IS_SWIPE_AD_ENABLED)
                ?: fromJson.isSwipeAdEnabled,
            isBackAdEnabled = remoteConfig.optionalBoolean(KEY_IS_BACK_AD_ENABLED)
                ?: fromJson.isBackAdEnabled,

            isClickAdInterstitial = remoteConfig.optionalBoolean(KEY_IS_CLICK_AD_INTERSTITIAL)
                ?: remoteConfig.optionalBoolean(KEY_APP_CLICK_INTER_LEGACY)
                ?: fromJson.isClickAdInterstitial,
            isSwipeAdInterstitial = remoteConfig.optionalBoolean(KEY_IS_SWIPE_AD_INTERSTITIAL)
                ?: remoteConfig.optionalBoolean(KEY_SWIPE_INTER_LEGACY)
                ?: fromJson.isSwipeAdInterstitial,

            interAdCounterTrigger = (remoteConfig.optionalInt(KEY_INTER_COUNTER_TRIGGER)
                ?: remoteConfig.optionalInt(KEY_INTER_COUNT_LEGACY)
                ?: fromJson.interAdCounterTrigger).coerceAtLeast(1),

            interAdBackCounterTrigger = (remoteConfig.optionalInt(KEY_INTER_BACK_COUNTER_TRIGGER)
                ?: remoteConfig.optionalInt(KEY_INTER_BACK_COUNT_LEGACY)
                ?: fromJson.interAdBackCounterTrigger).coerceAtLeast(1),

            clickAdCounterTrigger = (remoteConfig.optionalInt(KEY_CLICK_AD_COUNTER_TRIGGER)
                ?: fromJson.clickAdCounterTrigger).coerceAtLeast(1),

            bannerAdEnableSplash = remoteConfig.optionalBoolean(KEY_BANNER_ENABLE_SPLASH)
                ?: fromJson.bannerAdEnableSplash,
            bannerAdEnableHome = remoteConfig.optionalBoolean(KEY_BANNER_ENABLE_HOME_SCREEN)
                ?: remoteConfig.optionalBoolean(KEY_BANNER_ENABLE_HOME)
                ?: remoteConfig.optionalBoolean(KEY_BANNER_ENABLE_CONTACT_HOME_LEGACY)
                ?: fromJson.bannerAdEnableHome,
            bannerAdEnableAppDrawer = remoteConfig.optionalBoolean(KEY_BANNER_ENABLE_APP_DRAWER)
                ?: fromJson.bannerAdEnableAppDrawer,
            bannerAdEnableFindPhone = remoteConfig.optionalBoolean(KEY_BANNER_ENABLE_FIND_PHONE)
                ?: fromJson.bannerAdEnableFindPhone,
            bannerAdEnableAlertScreen = remoteConfig.optionalBoolean(KEY_BANNER_ENABLE_ALERT_SCREEN)
                ?: remoteConfig.optionalBoolean(KEY_BANNER_ENABLE_ALERT_ACTIVITY_LEGACY)
                ?: fromJson.bannerAdEnableAlertScreen,

            bannerAdIdSplash = remoteConfig.optionalString(KEY_BANNER_ID_SPLASH)
                ?: fromJson.bannerAdIdSplash,
            bannerAdIdHome = remoteConfig.optionalString(KEY_BANNER_ID_HOME_SCREEN)
                ?: remoteConfig.optionalString(KEY_BANNER_ID_HOME)
                ?: remoteConfig.optionalString(KEY_BANNER_ID_CONTACT_HOME_LEGACY)
                ?: fromJson.bannerAdIdHome,
            bannerAdIdAppDrawer = remoteConfig.optionalString(KEY_BANNER_ID_APP_DRAWER)
                ?: fromJson.bannerAdIdAppDrawer,
            bannerAdIdFindPhone = remoteConfig.optionalString(KEY_BANNER_ID_FIND_PHONE)
                ?: fromJson.bannerAdIdFindPhone,
            bannerAdIdAlertScreen = remoteConfig.optionalString(KEY_BANNER_ID_ALERT_SCREEN)
                ?: remoteConfig.optionalString(KEY_BANNER_ID_ALERT_ACTIVITY_LEGACY)
                ?: fromJson.bannerAdIdAlertScreen,

            nativeAdEnableDashboard = remoteConfig.optionalBoolean(KEY_NATIVE_ENABLE_DASHBOARD)
                ?: fromJson.nativeAdEnableDashboard,
            nativeAdEnableGoogleSearch = remoteConfig.optionalBoolean(KEY_NATIVE_ENABLE_GOOGLE_SEARCH)
                ?: fromJson.nativeAdEnableGoogleSearch,
            nativeAdEnableLanguage = remoteConfig.optionalBoolean(KEY_NATIVE_ENABLE_LANGUAGE)
                ?: fromJson.nativeAdEnableLanguage,
            nativeAdEnableAfterCall = remoteConfig.optionalBoolean(KEY_NATIVE_ENABLE_AFTER_CALL)
                ?: fromJson.nativeAdEnableAfterCall,

            nativeAdIdDashboard = remoteConfig.optionalString(KEY_NATIVE_ID_DASHBOARD)
                ?: fromJson.nativeAdIdDashboard,
            nativeAdIdGoogleSearch = remoteConfig.optionalString(KEY_NATIVE_ID_GOOGLE_SEARCH)
                ?: fromJson.nativeAdIdGoogleSearch,
            nativeAdIdLanguage = remoteConfig.optionalString(KEY_NATIVE_ID_LANGUAGE)
                ?: fromJson.nativeAdIdLanguage,
            nativeAdIdAfterCall = remoteConfig.optionalString(KEY_NATIVE_ID_AFTER_CALL)
                ?: fromJson.nativeAdIdAfterCall,

            nativeAdGoogleSearchItemInterval = (remoteConfig.optionalInt(KEY_NATIVE_GOOGLE_SEARCH_ITEM_INTERVAL)
                ?: fromJson.nativeAdGoogleSearchItemInterval).coerceAtLeast(1),

            interAdId = remoteConfig.optionalString(KEY_INTER_ID) ?: fromJson.interAdId,
            appOpenAdId = remoteConfig.optionalString(KEY_APP_OPEN_ID) ?: fromJson.appOpenAdId,

            preloadAdBanner = remoteConfig.optionalBoolean(KEY_PRELOAD_AD_BANNER)
                ?: remoteConfig.optionalBoolean(KEY_BANNER_PRELOAD_LEGACY)
                ?: fromJson.preloadAdBanner,

            preloadAdNative = remoteConfig.optionalBoolean(KEY_PRELOAD_AD_NATIVE)
                ?: remoteConfig.optionalBoolean(KEY_NATIVE_PRELOAD_LEGACY)
                ?: fromJson.preloadAdNative,

            preloadAdInterstitial = remoteConfig.optionalBoolean(KEY_PRELOAD_AD_INTERSTITIAL)
                ?: remoteConfig.optionalBoolean(KEY_INTER_PRELOAD_LEGACY)
                ?: fromJson.preloadAdInterstitial,

            preloadAdAppOpen = remoteConfig.optionalBoolean(KEY_PRELOAD_AD_APP_OPEN)
                ?: remoteConfig.optionalBoolean(KEY_APP_OPEN_PRELOAD_LEGACY)
                ?: fromJson.preloadAdAppOpen,
        )
    }

    private fun FirebaseRemoteConfig.optionalString(key: String): String? {
        val value = getValue(key)
        if (value.source != FirebaseRemoteConfig.VALUE_SOURCE_REMOTE) return null
        return value.asString().takeIf { it.isNotBlank() }
    }

    private fun FirebaseRemoteConfig.optionalBoolean(key: String): Boolean? {
        val value = getValue(key)
        if (value.source != FirebaseRemoteConfig.VALUE_SOURCE_REMOTE) return null
        return value.asBoolean()
    }

    private fun FirebaseRemoteConfig.optionalInt(key: String): Int? {
        val value = getValue(key)
        if (value.source != FirebaseRemoteConfig.VALUE_SOURCE_REMOTE) return null
        return value.asString().toIntOrNull() ?: value.asLong().toInt()
    }
}
