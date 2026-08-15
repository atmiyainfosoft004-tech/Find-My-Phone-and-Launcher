package com.example.findmyphonebyclaplauncher.util

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsCallback
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession
import androidx.core.content.ContextCompat
import com.example.findmyphonebyclaplauncher.R

object ChromeCustomTabHelper {

    private const val CHROME_PACKAGE = "com.android.chrome"
    private val mainHandler = Handler(Looper.getMainLooper())

    private var connection: CustomTabsServiceConnection? = null
    private var client: CustomTabsClient? = null
    private var session: CustomTabsSession? = null
    private var onTabClosed: (() -> Unit)? = null

    fun warmup(context: Context) {
        if (connection != null || !isChromeAvailable(context)) return
        val appContext = context.applicationContext
        val conn = object : CustomTabsServiceConnection() {
            override fun onCustomTabsServiceConnected(name: ComponentName, tabsClient: CustomTabsClient) {
                client = tabsClient
                tabsClient.warmup(0L)
                session = tabsClient.newSession(object : CustomTabsCallback() {
                    override fun onNavigationEvent(navigationEvent: Int, extras: android.os.Bundle?) {
                        if (navigationEvent == CustomTabsCallback.TAB_HIDDEN) {
                            val callback = onTabClosed
                            onTabClosed = null
                            if (callback != null) mainHandler.post(callback)
                        }
                    }
                })
            }

            override fun onServiceDisconnected(name: ComponentName) {
                client = null
                session = null
            }
        }
        connection = conn
        val bound = CustomTabsClient.bindCustomTabsService(appContext, CHROME_PACKAGE, conn)
        if (!bound) {
            connection = null
        }
    }

    fun release(context: Context) {
        onTabClosed = null
        try {
            connection?.let { context.applicationContext.unbindService(it) }
        } catch (_: Exception) {
        }
        connection = null
        client = null
        session = null
    }

    /**
     * @return true if Chrome Custom Tab was launched. False means caller should open WebView.
     */
    fun openUrl(activity: Activity, url: String?, onTabClosed: (() -> Unit)? = null): Boolean {
        if (url.isNullOrEmpty()) return false
        if (!isChromeAvailable(activity)) return false
        return try {
            this.onTabClosed = onTabClosed
            val builder = CustomTabsIntent.Builder()
            val colorBuilder = CustomTabColorSchemeParams.Builder()
            colorBuilder.setToolbarColor(
                ContextCompat.getColor(activity, R.color.color_web_chrome)
            )
            builder.setDefaultColorSchemeParams(colorBuilder.build())
            builder.setShowTitle(true)
            builder.setShareState(CustomTabsIntent.SHARE_STATE_ON)
            builder.setInstantAppsEnabled(true)
            session?.let { builder.setSession(it) }
            val customTabsIntent = builder.build()
            customTabsIntent.intent.setPackage(CHROME_PACKAGE)
            customTabsIntent.launchUrl(activity, Uri.parse(url))
            true
        } catch (_: Exception) {
            this.onTabClosed = null
            false
        }
    }

    fun isChromeAvailable(context: Context): Boolean {
        return try {
            val pm = context.packageManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(CHROME_PACKAGE, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(CHROME_PACKAGE, 0)
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}
