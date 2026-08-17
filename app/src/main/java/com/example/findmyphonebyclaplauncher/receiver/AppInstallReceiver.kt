package com.example.findmyphonebyclaplauncher.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.findmyphonebyclaplauncher.ui.install.AppInstallSuccessActivity

class AppInstallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val packageName = intent.data?.schemeSpecificPart ?: return

        // Skip our own application package
        if (packageName == context.packageName) return

        val isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)

        Log.d("AppInstallReceiver", "Package broadcast received: action=$action, package=$packageName, isReplacing=$isReplacing")
        val cachedLabel = com.example.findmyphonebyclaplauncher.data.cache.AppIconCacheManager.getCachedAppLabel(packageName)
        val cachedIconPath = com.example.findmyphonebyclaplauncher.data.cache.AppIconCacheManager.saveIconBitmapToCacheFile(context, packageName)
        val cachedSize = com.example.findmyphonebyclaplauncher.data.cache.AppIconCacheManager.getCachedAppSize(packageName)

        com.example.findmyphonebyclaplauncher.data.repository.AppRepository.get().invalidatePackageCache(packageName)
        com.example.findmyphonebyclaplauncher.data.repository.AppRepository.get().refreshAppsAsync()

        when (action) {
            Intent.ACTION_PACKAGE_ADDED -> {
                val actionType = if (isReplacing) {
                    AppInstallSuccessActivity.ACTION_TYPE_UPDATED
                } else {
                    AppInstallSuccessActivity.ACTION_TYPE_INSTALLED
                }
                AppInstallSuccessActivity.start(context, packageName, actionType, cachedLabel, cachedIconPath, cachedSize)
            }
            Intent.ACTION_PACKAGE_REMOVED -> {
                // If it is replacing, an ACTION_PACKAGE_ADDED with isReplacing=true will follow shortly
                if (!isReplacing) {
                    AppInstallSuccessActivity.start(context, packageName, AppInstallSuccessActivity.ACTION_TYPE_UNINSTALLED, cachedLabel, cachedIconPath, cachedSize)
                }
            }
        }
    }
}
