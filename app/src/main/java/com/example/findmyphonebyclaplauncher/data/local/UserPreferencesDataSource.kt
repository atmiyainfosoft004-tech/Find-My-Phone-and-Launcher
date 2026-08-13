package com.example.findmyphonebyclaplauncher.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.findmyphonebyclaplauncher.utils.Constants

/**
 * Low-level data source that reads/writes user preferences via SharedPreferences.
 * Activities and services must NOT access SharedPreferences directly — use this class.
 */
class UserPreferencesDataSource(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        Constants.PREFS_NAME,
        Context.MODE_PRIVATE
    )

    var isOnboardingCompleted: Boolean
        get() = prefs.getBoolean(Constants.KEY_ONBOARDING_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(Constants.KEY_ONBOARDING_COMPLETED, value).apply()

    var isOnboardingSkipped: Boolean
        get() = prefs.getBoolean(Constants.KEY_ONBOARDING_SKIPPED, false)
        set(value) = prefs.edit().putBoolean(Constants.KEY_ONBOARDING_SKIPPED, value).apply()

    var isFirstLaunch: Boolean
        get() = prefs.getBoolean(Constants.KEY_FIRST_LAUNCH, true)
        set(value) = prefs.edit().putBoolean(Constants.KEY_FIRST_LAUNCH, value).apply()

    var isClapDetectionEnabled: Boolean
        get() = prefs.getBoolean(Constants.KEY_CLAP_DETECTION_ENABLED, false)
        set(value) = prefs.edit().putBoolean(Constants.KEY_CLAP_DETECTION_ENABLED, value).apply()

    var isWhistleDetectionEnabled: Boolean
        get() = prefs.getBoolean(Constants.KEY_WHISTLE_DETECTION_ENABLED, false)
        set(value) = prefs.edit().putBoolean(Constants.KEY_WHISTLE_DETECTION_ENABLED, value).apply()

    var isFindPhoneEnabled: Boolean
        get() = prefs.getBoolean(Constants.KEY_FIND_PHONE_ENABLED, false)
        set(value) = prefs.edit().putBoolean(Constants.KEY_FIND_PHONE_ENABLED, value).apply()

    var isBatteryOptimizationGuidanceShown: Boolean
        get() = prefs.getBoolean(Constants.KEY_BATTERY_OPTIMIZATION_GUIDANCE_SHOWN, false)
        set(value) = prefs.edit().putBoolean(Constants.KEY_BATTERY_OPTIMIZATION_GUIDANCE_SHOWN, value).apply()

    var isSoundAlertEnabled: Boolean
        get() = prefs.getBoolean(Constants.KEY_SOUND_ALERT_ENABLED, true)
        set(value) = prefs.edit().putBoolean(Constants.KEY_SOUND_ALERT_ENABLED, value).apply()

    var isFlashlightEnabled: Boolean
        get() = prefs.getBoolean(Constants.KEY_FLASHLIGHT_ENABLED, true)
        set(value) = prefs.edit().putBoolean(Constants.KEY_FLASHLIGHT_ENABLED, value).apply()

    var isVibrationEnabled: Boolean
        get() = prefs.getBoolean(Constants.KEY_VIBRATION_ENABLED, true)
        set(value) = prefs.edit().putBoolean(Constants.KEY_VIBRATION_ENABLED, value).apply()

    var selectedAlertSound: String
        get() = prefs.getString(Constants.KEY_SELECTED_ALERT_SOUND, "whistle") ?: "whistle"
        set(value) = prefs.edit().putString(Constants.KEY_SELECTED_ALERT_SOUND, value).apply()

    var selectedAlertDuration: Int
        get() = prefs.getInt(Constants.KEY_SELECTED_ALERT_DURATION, 30)
        set(value) = prefs.edit().putInt(Constants.KEY_SELECTED_ALERT_DURATION, value).apply()

    var alertSoundVolume: Int
        get() = prefs.getInt(Constants.KEY_ALERT_SOUND_VOLUME, 50)
        set(value) = prefs.edit().putInt(Constants.KEY_ALERT_SOUND_VOLUME, value).apply()
}
