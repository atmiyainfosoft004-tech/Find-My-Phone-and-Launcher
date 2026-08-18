package com.example.findmyphonebyclaplauncher.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

object NetworkUtil {

    private const val TAG = "NetworkUtil"
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    var overrideNetworkAvailableForTesting: Boolean? = null

    /**
     * Checks if the device currently has active internet connectivity.
     */
    fun isNetworkAvailable(context: Context?): Boolean {
        overrideNetworkAvailableForTesting?.let { return it }
        if (context == null) return false

        return try {
            val cm = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
            val activeNetwork = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking network connectivity", e)
            false
        }
    }

    /**
     * Registers a ConnectivityManager.NetworkCallback.
     * Returns the created callback instance so it can be unregistered later.
     */
    fun registerNetworkCallback(
        context: Context,
        onAvailable: () -> Unit,
        onLost: (() -> Unit)? = null
    ): ConnectivityManager.NetworkCallback? {
        return try {
            val cm = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return null

            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    mainHandler.post {
                        onAvailable.invoke()
                    }
                }

                override fun onLost(network: Network) {
                    mainHandler.post {
                        onLost?.invoke()
                    }
                }
            }

            cm.registerNetworkCallback(networkRequest, callback)
            callback
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register network callback", e)
            null
        }
    }

    /**
     * Safely unregisters a previously registered ConnectivityManager.NetworkCallback.
     */
    fun unregisterNetworkCallback(
        context: Context,
        callback: ConnectivityManager.NetworkCallback?
    ) {
        if (callback == null) return
        try {
            val cm = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            cm?.unregisterNetworkCallback(callback)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister network callback", e)
        }
    }

    /**
     * Lifecycle-aware network observation.
     * Automatically registers on creation and unregisters on ON_DESTROY of the LifecycleOwner.
     */
    fun observeNetwork(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        onAvailable: () -> Unit,
        onLost: (() -> Unit)? = null
    ) {
        val callback = registerNetworkCallback(context, onAvailable, onLost) ?: return

        lifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                unregisterNetworkCallback(context, callback)
                owner.lifecycle.removeObserver(this)
            }
        })
    }
}
