package com.example.findmyphonebyclaplauncher.ui.menu

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.findmyphonebyclaplauncher.config.FirebaseConfigManager
import com.example.findmyphonebyclaplauncher.databinding.ActivityMenuBinding
import com.example.findmyphonebyclaplauncher.util.finishWithSlideAnimation

class MenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMenuBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        setupListeners()
    }

    private fun setupWindowInsets() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.layoutHeader.setPadding(
                binding.layoutHeader.paddingLeft,
                systemBars.top + resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._12sdp),
                binding.layoutHeader.paddingRight,
                binding.layoutHeader.paddingBottom
            )
            insets
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            com.example.findmyphonebyclaplauncher.ads.LauncherAdsHelper.showBackAd(this) {
                finishWithSlideAnimation()
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                com.example.findmyphonebyclaplauncher.ads.LauncherAdsHelper.showBackAd(this@MenuActivity) {
                    finishWithSlideAnimation()
                }
            }
        })

        binding.cardAbout.setOnClickListener {
            openPlayStorePage()
        }

        binding.cardHelp.setOnClickListener {
            openPlayStorePage()
        }

        binding.cardPrivacy.setOnClickListener {
            val privacyUrl = FirebaseConfigManager.getString(
                FirebaseConfigManager.KEY_PRIVACY_POLICY_URL,
                DEFAULT_PRIVACY_POLICY_URL
            )
            openInAppUrl(privacyUrl, DEFAULT_PRIVACY_POLICY_URL)
        }

        binding.cardTerms.setOnClickListener {
            val termsUrl = FirebaseConfigManager.getString(
                FirebaseConfigManager.KEY_TERMS_AND_CONDITIONS_URL,
                DEFAULT_TERMS_OF_SERVICE_URL
            )
            openInAppUrl(termsUrl, DEFAULT_TERMS_OF_SERVICE_URL)
        }
    }

    /**
     * Opens the app's official Google Play Store page.
     * Attempts direct market URI scheme first, falling back to browser URL if Play Store app is unavailable.
     */
    private fun openPlayStorePage() {
        val appPackageName = packageName
        try {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=$appPackageName")
                )
            )
        } catch (e: ActivityNotFoundException) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")
                )
            )
        }
    }

    /**
     * Opens the specified URL using Android Custom Tabs for a seamless in-app browsing experience.
     * Falls back to a standard ACTION_VIEW intent if Custom Tabs cannot be launched,
     * and uses the fallback default URL if the supplied URL string is null or empty.
     */
    private fun openInAppUrl(url: String?, fallbackUrl: String = DEFAULT_PRIVACY_POLICY_URL) {
        val targetUrl = if (!url.isNullOrBlank()) url.trim() else fallbackUrl
        val finalUrl = if (targetUrl.isBlank()) fallbackUrl else targetUrl

        try {
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
            customTabsIntent.launchUrl(this, Uri.parse(finalUrl))
        } catch (e: Exception) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl)))
            } catch (ex: Exception) {
                Log.e("MenuActivity", "Failed to launch URL intent for: $finalUrl", ex)
            }
        }
    }

    companion object {
        private const val DEFAULT_PRIVACY_POLICY_URL = "https://example.com/privacy-policy"
        private const val DEFAULT_TERMS_OF_SERVICE_URL = "https://example.com/terms-of-service"
    }
}
