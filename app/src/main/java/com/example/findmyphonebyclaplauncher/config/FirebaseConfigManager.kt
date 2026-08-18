package com.example.findmyphonebyclaplauncher.config

import android.content.Context
import android.util.Log
import com.example.findmyphonebyclaplauncher.BuildConfig
import com.example.findmyphonebyclaplauncher.ads.config.AdsConfig
import com.example.findmyphonebyclaplauncher.ads.config.AdsConfigManager
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings

/**
 * Initializes Firebase SDK and manages Firebase Remote Config parameters.
 */
object FirebaseConfigManager {

    private const val TAG = "RemoteConfig"

    const val KEY_PRIVACY_POLICY_URL = "privacy_policy_url"
    const val KEY_TERMS_AND_CONDITIONS_URL = "terms_and_conditions_url"
    const val DEFAULT_PRIVACY_POLICY_URL = "https://example.com/privacy-policy"
    const val DEFAULT_TERMS_AND_CONDITIONS_URL = "https://example.com/terms-of-service"

    @Volatile
    var isInitialized: Boolean = false
        private set

    /**
     * Initializes FirebaseApp (if needed) and configures Remote Config fetch settings, real-time listener, and defaults.
     */
    fun initialize(context: Context, onComplete: ((success: Boolean) -> Unit)? = null) {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
                Log.d(TAG, "FirebaseApp initialized")
            }

            val remoteConfig = FirebaseRemoteConfig.getInstance()
            val minimumFetchInterval = if (BuildConfig.DEBUG) 0L else 3600L
            val settings = FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(minimumFetchInterval)
                .build()

            remoteConfig.setConfigSettingsAsync(settings)

            // Define structured default remote config parameters based on updated schema
            val defaultParams = mapOf<String, Any>(
                "system_hide_navigation_bar_auto" to true,

                "is_banner_ad_enabled" to true,
                "is_native_ad_enabled" to true,
                "is_inter_ad_enabled" to true,
                "is_app_open_ad_enabled" to true,

                "is_click_ad_enabled" to true,
                "is_swipe_ad_enabled" to true,
                "is_back_ad_enabled" to true,

                "is_click_ad_interstitial" to true,
                "is_swipe_ad_interstitial" to true,

                "inter_ad_counter_trigger" to 3,
                "inter_ad_back_counter_trigger" to 3,
                "click_ad_counter_trigger" to 3,

                "banner_ad_enable_splash" to true,
                "banner_ad_enable_home_screen" to false,
                "banner_ad_enable_app_drawer" to true,
                "banner_ad_enable_find_phone" to true,
                "banner_ad_enable_alert_screen" to true,
                "banner_ad_enable_after_call" to true,

                "banner_ad_id_splash" to AdsConfig.DEFAULT_BANNER_ID,
                "banner_ad_id_home_screen" to AdsConfig.DEFAULT_BANNER_ID,
                "banner_ad_id_app_drawer" to AdsConfig.DEFAULT_BANNER_ID,
                "banner_ad_id_find_phone" to AdsConfig.DEFAULT_BANNER_ID,
                "banner_ad_id_alert_screen" to AdsConfig.DEFAULT_BANNER_ID,
                "banner_ad_id_after_call" to AdsConfig.DEFAULT_BANNER_ID,

                "native_ad_enable_dashboard" to true,
                "native_ad_enable_google_search" to true,
                "native_ad_enable_language" to true,
                "native_ad_enable_install_uninstall" to true,

                "native_ad_id_dashboard" to AdsConfig.DEFAULT_NATIVE_ID,
                "native_ad_id_google_search" to AdsConfig.DEFAULT_NATIVE_ID,
                "native_ad_id_language" to AdsConfig.DEFAULT_NATIVE_ID,
                "native_ad_id_install_uninstall" to AdsConfig.DEFAULT_NATIVE_ID,
                "native_ad_google_search_item_interval" to 2,

                "inter_ad_id" to AdsConfig.DEFAULT_INTER_ID,
                "app_open_ad_id" to AdsConfig.DEFAULT_APP_OPEN_ID,

                "preload_ad_banner" to false,
                "preload_ad_native" to false,
                "preload_ad_interstitial" to false,
                "preload_ad_app_open" to false,

                KEY_PRIVACY_POLICY_URL to DEFAULT_PRIVACY_POLICY_URL,
                KEY_TERMS_AND_CONDITIONS_URL to DEFAULT_TERMS_AND_CONDITIONS_URL
            )
            remoteConfig.setDefaultsAsync(defaultParams)

            isInitialized = true
            Log.d(TAG, "FirebaseConfigManager initialized (minimumFetchInterval=$minimumFetchInterval s)")

            // Listen for real-time Remote Config updates
            remoteConfig.addOnConfigUpdateListener(object : ConfigUpdateListener {
                override fun onUpdate(configUpdate: ConfigUpdate) {
                    Log.d(TAG, "Real-time Remote Config updated for keys: ${configUpdate.updatedKeys}")
                    remoteConfig.activate().addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d(TAG, "Real-time Remote Config activated successfully")
                            AdsConfigManager.refresh()
                        } else {
                            Log.e(TAG, "Real-time Remote Config activation failed", task.exception)
                        }
                    }
                }

                override fun onError(error: FirebaseRemoteConfigException) {
                    Log.e(TAG, "Real-time Remote Config update error: ${error.message}", error)
                }
            })

            remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Remote Config fetchAndActivate SUCCESS")
                } else {
                    Log.e(TAG, "Remote Config fetchAndActivate FAILURE. Exception: ${task.exception?.message}", task.exception)
                }
                onComplete?.invoke(task.isSuccessful)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing FirebaseConfigManager", e)
            onComplete?.invoke(false)
        }
    }

    /**
     * Retrieves a string parameter from Remote Config with fallback to static default.
     */
    fun getString(key: String, defaultValue: String = ""): String {
        return try {
            val config = FirebaseRemoteConfig.getInstance()
            val value = config.getString(key)
            if (value.isNotBlank()) value else defaultValue
        } catch (e: Exception) {
            defaultValue
        }
    }

    /**
     * Retrieves a boolean parameter from Remote Config with fallback to static default.
     */
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return try {
            FirebaseRemoteConfig.getInstance().getBoolean(key)
        } catch (e: Exception) {
            defaultValue
        }
    }

    /**
     * Retrieves a long parameter from Remote Config with fallback to static default.
     */
    fun getLong(key: String, defaultValue: Long = 0L): Long {
        return try {
            FirebaseRemoteConfig.getInstance().getLong(key)
        } catch (e: Exception) {
            defaultValue
        }
    }

    /**
     * Retrieves an int parameter from Remote Config with fallback to static default.
     */
    fun getInt(key: String, defaultValue: Int = 0): Int {
        return try {
            FirebaseRemoteConfig.getInstance().getLong(key).toInt()
        } catch (e: Exception) {
            defaultValue
        }
    }

    fun getPrivacyPolicyUrl(): String = getString(KEY_PRIVACY_POLICY_URL, DEFAULT_PRIVACY_POLICY_URL)

    fun getTermsAndConditionsUrl(): String = getString(KEY_TERMS_AND_CONDITIONS_URL, DEFAULT_TERMS_AND_CONDITIONS_URL)
}
