package com.example.findmyphonebyclaplauncher.config

import android.content.Context
import android.util.Log
import com.example.findmyphonebyclaplauncher.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings

/**
 * Initializes Firebase SDK and manages Firebase Remote Config parameters.
 * Modeled after the Firebase initialization structure used in CallLauncher.
 */
object FirebaseConfigManager {

    private const val TAG = "FirebaseConfigManager"

    @Volatile
    var isInitialized: Boolean = false
        private set

    /**
     * Initializes FirebaseApp (if needed) and configures Remote Config fetch settings and defaults.
     */
    fun initialize(context: Context, onComplete: ((success: Boolean) -> Unit)? = null) {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
                Log.d(TAG, "FirebaseApp initialized")
            }

            val remoteConfig = FirebaseRemoteConfig.getInstance()
            val settings = FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(if (BuildConfig.DEBUG) 0L else 3600L)
                .build()

            remoteConfig.setConfigSettingsAsync(settings)

            // Define initial default remote config parameters
            val defaultParams = mapOf<String, Any>(
                "is_ads_enabled" to true,
                "banner_ad_id" to "",
                "interstitial_ad_id" to "",
                "native_ad_id" to ""
            )
            remoteConfig.setDefaultsAsync(defaultParams)

            isInitialized = true

            remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Remote config fetched and activated successfully")
                } else {
                    Log.e(TAG, "Remote config fetch failed", task.exception)
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
}
