package com.example.findmyphonebyclaplauncher.analytics

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics

object AnalyticsHelper {
    private const val TAG = "AnalyticsHelper"
    private var firebaseAnalytics: FirebaseAnalytics? = null

    fun init(context: Context) {
        try {
            firebaseAnalytics = FirebaseAnalytics.getInstance(context)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize FirebaseAnalytics", e)
        }
    }

    fun logEvent(eventName: String, params: Bundle? = null) {
        try {
            firebaseAnalytics?.logEvent(eventName, params)
            Log.d(TAG, "Event logged: $eventName")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log event: $eventName", e)
        }
    }

    fun logAds(context: Context, adEvent: String) {
        val bundle = Bundle().apply {
            putString("ad_event_name", adEvent)
        }
        logEvent("ad_tracker", bundle)
        Log.d("AdsTracker", "ad_event: $adEvent")
    }
}
