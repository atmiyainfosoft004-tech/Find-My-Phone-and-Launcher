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

    private const val KEY_BANNER_SPLASH = "banner_ad_id_splash"
    private const val KEY_BANNER_CONTACT_HOME = "banner_ad_id_contact_home"
    private const val KEY_BANNER_APP_DRAWER = "banner_ad_id_app_drawer"
    private const val KEY_NATIVE_DASHBOARD = "native_ad_id_dashboard"
    private const val KEY_NATIVE_GOOGLE_SEARCH = "native_ad_id_google_search"
    private const val KEY_NATIVE_LANGUAGE = "native_ad_id_language"
    private const val KEY_NATIVE_AFTER_CALL = "native_ad_id_after_call"
    private const val KEY_INTER = "inter_ad_id"
    private const val KEY_APP_OPEN = "app_open_ad_id"
    private const val KEY_INTER_COUNT = "inter_count"
    private const val KEY_INTER_BACK_COUNT = "inter_back_count"
    private const val KEY_APP_CLICK_INTER = "isAppClickInterOn"
    private const val KEY_SWIPE_INTER = "isRightLeftSwipeInterOn"
    private const val KEY_BANNER_ON = "isBannerOn"
    private const val KEY_NATIVE_ON = "isNativeOn"
    private const val KEY_INTER_ON = "isInterOn"
    private const val KEY_BANNER_PRELOAD = "bannerAdPreload"
    private const val KEY_NATIVE_PRELOAD = "nativeAdPreload"
    private const val KEY_INTER_PRELOAD = "interAdPreload"
    private const val KEY_APP_OPEN_PRELOAD = "appOpenAdPreload"

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

    fun initialize(context: Context, onComplete: ((success: Boolean) -> Unit)? = null) {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
        val remoteConfig = FirebaseRemoteConfig.getInstance()
        // Zero-cache in Debug builds (0 seconds), 3600 seconds in release builds
        val minimumFetchInterval = if (BuildConfig.DEBUG) 0L else 3600L
        val settings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(minimumFetchInterval)
            .build()
        remoteConfig.setConfigSettingsAsync(settings)

        val defaultMap: Map<String, Any> = mapOf(
            KEY_ADS_CONFIG to gson.toJson(AdsConfig.DEFAULT),
            KEY_BANNER_SPLASH to AdsConfig.DEFAULT_BANNER_ID,
            KEY_BANNER_CONTACT_HOME to AdsConfig.DEFAULT_BANNER_ID,
            KEY_BANNER_APP_DRAWER to AdsConfig.DEFAULT_BANNER_ID,
            KEY_NATIVE_DASHBOARD to AdsConfig.DEFAULT_NATIVE_ID,
            KEY_NATIVE_GOOGLE_SEARCH to AdsConfig.DEFAULT_NATIVE_ID,
            KEY_NATIVE_LANGUAGE to AdsConfig.DEFAULT_NATIVE_ID,
            KEY_NATIVE_AFTER_CALL to AdsConfig.DEFAULT_NATIVE_ID,
            KEY_INTER to AdsConfig.DEFAULT_INTER_ID,
            KEY_APP_OPEN to AdsConfig.DEFAULT_APP_OPEN_ID,
            KEY_INTER_COUNT to 1,
            KEY_INTER_BACK_COUNT to 3,
            KEY_APP_CLICK_INTER to true,
            KEY_SWIPE_INTER to true,
            KEY_BANNER_ON to true,
            KEY_NATIVE_ON to true,
            KEY_INTER_ON to true,
            KEY_BANNER_PRELOAD to false,
            KEY_NATIVE_PRELOAD to false,
            KEY_INTER_PRELOAD to true,
            KEY_APP_OPEN_PRELOAD to false
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
                        Log.d(TAG, "fetchAndActivate SUCCESS. Source: ${remoteConfig.getValue(KEY_ADS_CONFIG).source}. Active config: $config")
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
            bannerAdIdSplash = remoteConfig.optionalString(KEY_BANNER_SPLASH)
                ?: fromJson.bannerAdIdSplash,
            bannerAdIdContactHome = remoteConfig.optionalString(KEY_BANNER_CONTACT_HOME)
                ?: fromJson.bannerAdIdContactHome,
            bannerAdIdAppDrawer = remoteConfig.optionalString(KEY_BANNER_APP_DRAWER)
                ?: fromJson.bannerAdIdAppDrawer,
            nativeAdIdDashboard = remoteConfig.optionalString(KEY_NATIVE_DASHBOARD)
                ?: fromJson.nativeAdIdDashboard,
            nativeAdIdGoogleSearch = remoteConfig.optionalString(KEY_NATIVE_GOOGLE_SEARCH)
                ?: fromJson.nativeAdIdGoogleSearch,
            nativeAdIdLanguage = remoteConfig.optionalString(KEY_NATIVE_LANGUAGE)
                ?: fromJson.nativeAdIdLanguage,
            nativeAdIdAfterCall = remoteConfig.optionalString(KEY_NATIVE_AFTER_CALL)
                ?: fromJson.nativeAdIdAfterCall,
            interAdId = remoteConfig.optionalString(KEY_INTER) ?: fromJson.interAdId,
            appOpenAdId = remoteConfig.optionalString(KEY_APP_OPEN) ?: fromJson.appOpenAdId,
            interCount = (remoteConfig.optionalInt(KEY_INTER_COUNT) ?: fromJson.interCount)
                .coerceAtLeast(1),
            interBackCount = (remoteConfig.optionalInt(KEY_INTER_BACK_COUNT) ?: fromJson.interBackCount)
                .coerceAtLeast(1),
            isAppClickInterOn = remoteConfig.optionalBoolean(KEY_APP_CLICK_INTER)
                ?: fromJson.isAppClickInterOn,
            isRightLeftSwipeInterOn = remoteConfig.optionalBoolean(KEY_SWIPE_INTER)
                ?: fromJson.isRightLeftSwipeInterOn,
            isBannerOn = remoteConfig.optionalBoolean(KEY_BANNER_ON) ?: fromJson.isBannerOn,
            isNativeOn = remoteConfig.optionalBoolean(KEY_NATIVE_ON) ?: fromJson.isNativeOn,
            isInterOn = remoteConfig.optionalBoolean(KEY_INTER_ON) ?: fromJson.isInterOn,
            bannerAdPreload = remoteConfig.optionalBoolean(KEY_BANNER_PRELOAD)
                ?: fromJson.bannerAdPreload,
            nativeAdPreload = remoteConfig.optionalBoolean(KEY_NATIVE_PRELOAD)
                ?: fromJson.nativeAdPreload,
            interAdPreload = remoteConfig.optionalBoolean(KEY_INTER_PRELOAD)
                ?: fromJson.interAdPreload,
            appOpenAdPreload = remoteConfig.optionalBoolean(KEY_APP_OPEN_PRELOAD)
                ?: fromJson.appOpenAdPreload,
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
