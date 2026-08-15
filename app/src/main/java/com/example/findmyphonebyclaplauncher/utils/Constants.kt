package com.example.findmyphonebyclaplauncher.utils

import android.media.AudioFormat

/**
 * Centralized constants for the Find My Phone by Clap Launcher app.
 * All detection thresholds are configurable here to facilitate tuning on real devices.
 */
object Constants {

    // ─────────────────────────────────────────────────────────────────────────
    // General
    // ─────────────────────────────────────────────────────────────────────────
    const val TAG = "FindMyPhone"

    // ─────────────────────────────────────────────────────────────────────────
    // Notification Channels & IDs
    // ─────────────────────────────────────────────────────────────────────────
    const val CHANNEL_ID_DETECTION = "channel_sound_detection"
    const val CHANNEL_ID_ALERT     = "channel_find_phone_alert"

    const val NOTIFICATION_ID_DETECTION = 1001
    const val NOTIFICATION_ID_ALERT     = 1002

    // ─────────────────────────────────────────────────────────────────────────
    // SharedPreferences Keys
    // ─────────────────────────────────────────────────────────────────────────
    const val PREFS_NAME = "find_my_phone_prefs"

    const val KEY_ONBOARDING_COMPLETED               = "is_onboarding_completed"
    const val KEY_ONBOARDING_SKIPPED                 = "is_onboarding_skipped"
    const val KEY_FIRST_LAUNCH                      = "is_first_launch"
    const val KEY_CLAP_DETECTION_ENABLED              = "is_clap_detection_enabled"
    const val KEY_WHISTLE_DETECTION_ENABLED           = "is_whistle_detection_enabled"
    const val KEY_FIND_PHONE_ENABLED                  = "is_find_phone_enabled"
    const val KEY_BATTERY_OPTIMIZATION_GUIDANCE_SHOWN = "is_battery_optimization_guidance_shown"
    const val KEY_SOUND_ALERT_ENABLED                 = "is_sound_alert_enabled"
    const val KEY_FLASHLIGHT_ENABLED                  = "is_flashlight_enabled"
    const val KEY_VIBRATION_ENABLED                   = "is_vibration_enabled"
    const val KEY_SELECTED_ALERT_SOUND                = "selected_alert_sound"
    const val KEY_SELECTED_ALERT_DURATION             = "selected_alert_duration"
    const val KEY_ALERT_SOUND_VOLUME                  = "alert_sound_volume"

    // ─────────────────────────────────────────────────────────────────────────
    // Clap Detection Thresholds
    // ─────────────────────────────────────────────────────────────────────────

    /** How many claps are required to trigger Find Phone. */
    const val CLAP_COUNT_REQUIRED = 3
    const val CLAP_DETECTION_THRESHOLD = 2500.0
    const val CLAP_SILENCE_THRESHOLD = 1200.0
    const val MIN_CLAP_INTERVAL_MS = 180L
    const val MAX_CLAP_INTERVAL_MS = 1500L
    const val MAX_CLAP_SEQUENCE_DURATION_MS = 4500L
    const val CLAP_MIN_DURATION_MS = 15L
    const val CLAP_MAX_DURATION_MS = 180L

    // ─────────────────────────────────────────────────────────────────────────
    // Whistle Detection Thresholds
    // ─────────────────────────────────────────────────────────────────────────

    const val WHISTLE_MIN_FREQUENCY_HZ = 1000.0
    const val WHISTLE_MAX_FREQUENCY_HZ = 3500.0
    const val WHISTLE_AMPLITUDE_THRESHOLD = 1000.0
    const val WHISTLE_SUSTAINED_MS = 280L
    const val WHISTLE_MIN_DURATION_MS = 280L
    const val WHISTLE_MAX_DURATION_MS = 3000L

    // ─────────────────────────────────────────────────────────────────────────
    // Audio Recording
    // ─────────────────────────────────────────────────────────────────────────
    const val SAMPLE_RATE    = 44100
    const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    const val AUDIO_FORMAT   = AudioFormat.ENCODING_PCM_16BIT

    /** Multiplier on top of min buffer size for extra headroom. */
    const val BUFFER_SIZE_FACTOR = 2

    // ─────────────────────────────────────────────────────────────────────────
    // Service Actions
    // ─────────────────────────────────────────────────────────────────────────
    const val ACTION_START_DETECTION  = "action_start_detection"
    const val ACTION_STOP_DETECTION   = "action_stop_detection"
    const val ACTION_UPDATE_SETTINGS  = "action_update_settings"
    const val ACTION_STOP_ALERT       = "action_stop_alert"

    // ─────────────────────────────────────────────────────────────────────────
    // Intent Extras
    // ─────────────────────────────────────────────────────────────────────────
    const val EXTRA_CLAP_ENABLED    = "extra_clap_enabled"
    const val EXTRA_WHISTLE_ENABLED = "extra_whistle_enabled"
}
