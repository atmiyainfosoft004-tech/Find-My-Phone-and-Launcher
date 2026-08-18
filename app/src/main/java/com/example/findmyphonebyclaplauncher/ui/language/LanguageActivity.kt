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
import com.example.findmyphonebyclaplauncher.databinding.ActivityLanguageBinding
import com.example.findmyphonebyclaplauncher.ui.common.BaseActivity
import com.example.findmyphonebyclaplauncher.ui.findphone.FindPhoneActivity
import com.example.findmyphonebyclaplauncher.util.finishWithSlideAnimation
import com.example.findmyphonebyclaplauncher.utils.LocaleHelper

class LanguageActivity : BaseActivity() {

    private lateinit var binding: ActivityLanguageBinding
    private var isFirstTime: Boolean = false
    private lateinit var adapter: LanguageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLanguageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        isFirstTime = intent.getBooleanExtra(EXTRA_IS_FIRST_TIME, false)

        setupWindowInsets()
        setupHeader()
        setupRecyclerView()
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

    private fun setupHeader() {
        if (isFirstTime) {
            binding.txtTitle.text = getString(R.string.select_your_language)
            binding.btnBack.isVisible = false
            (binding.txtTitle.layoutParams as? androidx.constraintlayout.widget.ConstraintLayout.LayoutParams)?.apply {
                startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                startToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
                marginStart = 0
            }
        } else {
            binding.txtTitle.text = getString(R.string.change_language)
            binding.btnBack.isVisible = true
            (binding.txtTitle.layoutParams as? androidx.constraintlayout.widget.ConstraintLayout.LayoutParams)?.apply {
                startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
                startToEnd = binding.btnBack.id
                marginStart = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._12sdp)
            }
        }
    }

    private fun setupRecyclerView() {
        val currentLocale = LocaleHelper.getLocale(this)
        val languageList = getSupportedLanguages(currentLocale)

        adapter = LanguageAdapter(languageList) { _ ->
            // Tapping a language card item ONLY updates selection state in memory.
            // NO navigation or activity reload is triggered on item tap.
        }
        binding.rvLanguages.layoutManager = LinearLayoutManager(this)
        binding.rvLanguages.adapter = adapter
    }

    private fun getSupportedLanguages(currentCode: String): List<LanguageItem> {
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
            val isSelected = item.code.equals(currentCode, ignoreCase = true)
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
            val selectedItem = adapter.getSelectedItem() ?: return@setOnClickListener

            // 1. Persist the chosen language via LocaleHelper / SharedPreferences and update locale
            LocaleHelper.setLocale(this, selectedItem.code)

            // 2. Perform navigation
            if (isFirstTime) {
                // If first time launch, navigate directly to FindPhoneActivity (Home screen) and clear stack
                val intent = Intent(this, FindPhoneActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            } else {
                // If opened from Menu screen, finish and return to the previous screen
                finishWithSlideAnimation()
            }
        }
    }

    private fun handleBackPress() {
        if (isFirstTime) {
            // Block back press when launched for first time in onboarding flow
            return
        }
        com.example.findmyphonebyclaplauncher.ads.LauncherAdsHelper.showBackAd(this) {
            finishWithSlideAnimation()
        }
    }

    companion object {
        const val EXTRA_IS_FIRST_TIME = "extra_is_first_time"
    }
}
