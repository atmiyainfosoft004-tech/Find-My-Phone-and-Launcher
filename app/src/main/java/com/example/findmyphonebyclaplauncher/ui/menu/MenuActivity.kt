package com.example.findmyphonebyclaplauncher.ui.menu

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.findmyphonebyclaplauncher.databinding.ActivityMenuBinding

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
            finish()
        }

        binding.cardAbout.setOnClickListener {
            openPlayStorePage()
        }

        binding.cardHelp.setOnClickListener {
            openPlayStorePage()
        }

        binding.cardPrivacy.setOnClickListener {
            openInAppUrl(PRIVACY_POLICY_URL)
        }

        binding.cardTerms.setOnClickListener {
            openInAppUrl(TERMS_OF_SERVICE_URL)
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
     * Falls back to a standard ACTION_VIEW intent if Custom Tabs cannot be launched.
     */
    private fun openInAppUrl(url: String) {
        try {
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
            customTabsIntent.launchUrl(this, Uri.parse(url))
        } catch (e: Exception) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    companion object {
        private const val PRIVACY_POLICY_URL = "https://example.com/privacy-policy"
        private const val TERMS_OF_SERVICE_URL = "https://example.com/terms-of-service"
    }
}
