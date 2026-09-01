package com.example.findmyphonebyclaplauncher.ui.language

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.findmyphonebyclaplauncher.R
import com.example.findmyphonebyclaplauncher.ads.LauncherAdsHelper
import com.example.findmyphonebyclaplauncher.ads.config.RemoteConfigRepository
import com.example.findmyphonebyclaplauncher.data.local.UserPreferencesDataSource
import com.example.findmyphonebyclaplauncher.databinding.ActivityLanguageBinding
import com.example.findmyphonebyclaplauncher.ui.common.BaseActivity
import com.example.findmyphonebyclaplauncher.ui.onboarding.OnboardingActivity
import com.example.findmyphonebyclaplauncher.util.finishWithSlideAnimation
import com.example.findmyphonebyclaplauncher.utils.LocaleHelper

class LanguageActivity : BaseActivity() {

    private lateinit var binding: ActivityLanguageBinding
    private var isFirstTime: Boolean = false
    private lateinit var adapter: LanguageAdapter

    private var isUserLeaving = false
    private var isAppInBackground = false
    private var isNavigatingToNextScreen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLanguageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        isFirstTime = intent.getBooleanExtra(EXTRA_IS_FIRST_TIME, false) || intent.getBooleanExtra("isFirstTime", false)

        setupWindowInsets()
        setupHeader()
        setupRecyclerView(savedInstanceState)
        setupListeners()
        loadBannerAd()
        if (isFirstTime) {
            LauncherAdsHelper.preloadLanguageInterstitial(this)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::adapter.isInitialized) {
            adapter.getSelectedItem()?.let {
                outState.putString(KEY_SAVED_SELECTED_LANGUAGE_CODE, it.code)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        isUserLeaving = false
        loadBannerAd()
        if (isFirstTime) {
            LauncherAdsHelper.preloadLanguageInterstitial(this)
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (isFirstTime && !isNavigatingToNextScreen && !isFinishing) {
            isUserLeaving = true
            redirectToHomeFragment("onUserLeaveHint")
        }
    }

    override fun onStop() {
        super.onStop()
        if (isFirstTime && isUserLeaving && !isNavigatingToNextScreen && !isFinishing) {
            isAppInBackground = true
        }
    }

    override fun onRestart() {
        super.onRestart()
        if (isFirstTime && isUserLeaving && isAppInBackground && !isNavigatingToNextScreen) {
            redirectToHomeFragment("onRestart")
        }
    }

    private fun redirectToHomeFragment(source: String) {
        if (isFinishing || isDestroyed || isNavigatingToNextScreen) return
        android.util.Log.d("LanguageActivity", "Redirecting to HomeFragment due to Home press / background resume (source: $source)")

        val prefs = UserPreferencesDataSource(this)
        prefs.isLanguageSelected = true
        prefs.isOnboardingCompleted = true

        val intent = Intent(this, com.example.findmyphonebyclaplauncher.ui.findphone.FindPhoneActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun loadBannerAd() {
        val config = com.example.findmyphonebyclaplauncher.ads.config.AdsConfigManager.config
        val isNetworkAvailable = com.example.findmyphonebyclaplauncher.util.NetworkUtil.isNetworkAvailable(this)
        android.util.Log.d(
            "LanguageActivity",
            "loadBannerAd: NetworkAvailable=$isNetworkAvailable, isBannerAdEnabled=${config.isBannerAdEnabled}, bannerAdEnableLanguageRect=${config.bannerAdEnableLanguageRect}, bannerAdIdLanguageRect='${config.bannerAdIdLanguageRect}', canShowBannerLanguageRect=${config.canShowBannerLanguageRect}"
        )
        if (!isNetworkAvailable || !config.canShowBannerLanguageRect) {
            android.util.Log.w(
                "LanguageActivity",
                "Suppressing Language rectangle banner ad. NetworkAvailable=$isNetworkAvailable, canShowBannerLanguageRect=${config.canShowBannerLanguageRect}"
            )
            com.example.findmyphonebyclaplauncher.ads.BannerAdLoader.instance?.hideBannerContainer(
                binding.languageBanner.bannerAdFrameLayout,
                binding.languageBanner.bannerAdShimmerFrameLayout,
                binding.languageBanner.root
            )
            return
        }
        com.example.findmyphonebyclaplauncher.ads.LauncherAdsHelper.showLanguageRectBanner(
            this,
            binding.languageBanner.bannerAdFrameLayout,
            binding.languageBanner.bannerAdShimmerFrameLayout,
            binding.languageBanner.root
        )
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

    override fun updateLocalizedTexts() {
        if (isFirstTime) {
            binding.txtTitle.text = getString(R.string.select_your_language)
        } else {
            binding.txtTitle.text = getString(R.string.change_language)
        }
    }

    private fun setupHeader() {
        updateLocalizedTexts()
        if (isFirstTime) {
            binding.btnBack.isVisible = false
            (binding.txtTitle.layoutParams as? androidx.constraintlayout.widget.ConstraintLayout.LayoutParams)?.apply {
                startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                startToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
                marginStart = 0
            }
        } else {
            binding.btnBack.isVisible = true
            (binding.txtTitle.layoutParams as? androidx.constraintlayout.widget.ConstraintLayout.LayoutParams)?.apply {
                startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
                startToEnd = binding.btnBack.id
                marginStart = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._12sdp)
            }
        }
    }

    private fun setupRecyclerView(savedInstanceState: Bundle? = null) {
        val initialSelectedCode = if (isFirstTime) {
            null
        } else {
            val currentLocale = LocaleHelper.getLocale(this)
            val prefsLang = UserPreferencesDataSource(this).selectedLanguage
            if (currentLocale.isNotBlank()) currentLocale else prefsLang
        }

        val languageList = getSupportedLanguages(initialSelectedCode)

        adapter = LanguageAdapter(languageList) { _ ->
            updateDoneButtonState()
        }
        binding.rvLanguages.layoutManager = LinearLayoutManager(this)
        binding.rvLanguages.adapter = adapter

        val savedCode = savedInstanceState?.getString(KEY_SAVED_SELECTED_LANGUAGE_CODE)
        if (savedCode != null) {
            val index = languageList.indexOfFirst { it.code.equals(savedCode, ignoreCase = true) }
            if (index != -1) {
                adapter.setSelectedPosition(index)
            }
        }

        updateDoneButtonState()
    }

    private fun updateDoneButtonState() {
        val hasSelection = adapter.getSelectedItem() != null
        val shouldShowDone = if (isFirstTime) {
            hasSelection
        } else {
            true
        }
        binding.btnApplyLanguage.isVisible = shouldShowDone
        binding.btnApplyLanguage.isEnabled = shouldShowDone && (hasSelection || !isFirstTime)
    }

    private fun getSupportedLanguages(selectedCode: String? = null): List<LanguageItem> {
        val rawList = listOf(
            LanguageItem("en", "English", "English"),
            LanguageItem("hi", "Hindi", "हिन्दी"),
            LanguageItem("es", "Spanish", "Español"),
            LanguageItem("fr", "French", "Français"),
            LanguageItem("de", "German", "Deutsch"),
            LanguageItem("in", "Indonesian", "Bahasa Indonesia"),
            LanguageItem("ru", "Russian", "Русский"),
            LanguageItem("zh", "Chinese", "中文")
        )

        return rawList.map { item ->
            val isSelected = selectedCode != null && item.code.equals(selectedCode, ignoreCase = true)
            item.copy(isSelected = isSelected)
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            handleBackPress()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackPress()
            }
        })

        // Done / Checkmark action button (Top Right): Saves preference and navigates
        binding.btnApplyLanguage.setOnClickListener {
            if (isNavigatingToNextScreen) return@setOnClickListener
            val selectedItem = adapter.getSelectedItem() ?: return@setOnClickListener

            isNavigatingToNextScreen = true

            // 1. Mark language as selected in UserPreferencesDataSource
            val prefs = UserPreferencesDataSource(this)
            prefs.isLanguageSelected = true

            // 2. Ensure onboarding is NOT completed yet for initial setup flow
            if (isFirstTime) {
                prefs.isOnboardingCompleted = false
            }

            // 3. Persist the chosen language via LocaleHelper / SharedPreferences and update locale
            LocaleHelper.setLocale(this, selectedItem.code)

            // 4. Interstitial ad flow executes ONLY for the first-time Language onboarding flow
            if (isFirstTime) {
                LauncherAdsHelper.showLanguageDoneInterstitial(this) {
                    proceedToNextScreen()
                }
            } else {
                // Opened from Settings/Menu -> Zero Interstitial ad requests, zero loading dialog
                proceedToNextScreen()
            }
        }
    }

    private fun proceedToNextScreen() {
        if (isFinishing || isDestroyed) return
        if (isFirstTime) {
            // If first time launch, navigate explicitly to OnboardingActivity and clear stack
            val intent = Intent(this@LanguageActivity, OnboardingActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        } else {
            // If opened from Menu screen, finish and return seamlessly to the Menu screen
            finishWithSlideAnimation()
        }
    }

    private fun handleBackPress() {
        if (isFirstTime || isNavigatingToNextScreen) {
            // Block back press when launched for first time in onboarding flow or during navigation
            return
        }
        LauncherAdsHelper.showBackAd(this) {
            finishWithSlideAnimation()
        }
    }

    override fun onDestroy() {
        com.example.findmyphonebyclaplauncher.ads.BannerAdLoader.instance?.destroyBanner(binding.languageBanner.bannerAdFrameLayout)
        super.onDestroy()
    }

    companion object {
        const val EXTRA_IS_FIRST_TIME = "extra_is_first_time"
        private const val KEY_SAVED_SELECTED_LANGUAGE_CODE = "key_saved_selected_language_code"
    }
}
