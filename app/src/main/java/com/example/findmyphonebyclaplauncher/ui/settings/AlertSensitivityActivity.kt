package com.example.findmyphonebyclaplauncher.ui.settings

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.findmyphonebyclaplauncher.R
import com.example.findmyphonebyclaplauncher.data.local.UserPreferencesDataSource
import com.example.findmyphonebyclaplauncher.databinding.ActivityAlertSensitivityBinding
import com.example.findmyphonebyclaplauncher.util.finishWithSlideAnimation

class AlertSensitivityActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlertSensitivityBinding
    private lateinit var prefs: UserPreferencesDataSource

    private var selectedSensitivity = "Medium"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlertSensitivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = UserPreferencesDataSource(this)
        selectedSensitivity = prefs.alertSensitivity

        setupWindowInsets()
        updateSelectionUI(selectedSensitivity)
        setupListeners()
    }

    private fun setupWindowInsets() {
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
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
                com.example.findmyphonebyclaplauncher.ads.LauncherAdsHelper.showBackAd(this@AlertSensitivityActivity) {
                    finishWithSlideAnimation()
                }
            }
        })

        binding.cardLowSensitivity.setOnClickListener {
            selectedSensitivity = "Low"
            updateSelectionUI("Low")
        }

        binding.cardMediumSensitivity.setOnClickListener {
            selectedSensitivity = "Medium"
            updateSelectionUI("Medium")
        }

        binding.cardHighSensitivity.setOnClickListener {
            selectedSensitivity = "High"
            updateSelectionUI("High")
        }

        binding.btnSave.setOnClickListener {
            prefs.alertSensitivity = selectedSensitivity
            if (prefs.isClapDetectionEnabled || prefs.isWhistleDetectionEnabled) {
                val intent = com.example.findmyphonebyclaplauncher.service.SoundDetectionService.updateIntent(
                    this,
                    prefs.isClapDetectionEnabled,
                    prefs.isWhistleDetectionEnabled
                )
                startService(intent)
            }
            com.example.findmyphonebyclaplauncher.ads.LauncherAdsHelper.showBackAd(this) {
                finishWithSlideAnimation()
            }
        }
    }

    private fun updateSelectionUI(sensitivity: String) {
        val primaryColor = ContextCompat.getColor(this, R.color.color_primary)
        val defaultText = ContextCompat.getColor(this, R.color.color_onboarding_text_primary)
        val whiteColor = ContextCompat.getColor(this, R.color.white)
        val strokeDefault = ContextCompat.getColor(this, R.color.color_card_stroke_light)
        val selectedBgColor = ContextCompat.getColor(this, R.color.color_card_master_bg)

        binding.cardLowSensitivity.setCardBackgroundColor(whiteColor)
        binding.cardLowSensitivity.strokeColor = strokeDefault
        binding.cardLowSensitivity.strokeWidth = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._1sdp)
        binding.imgRadioLow.setImageResource(R.drawable.ic_radio_unchecked)
        binding.txtLow.setTextColor(defaultText)

        binding.cardMediumSensitivity.setCardBackgroundColor(whiteColor)
        binding.cardMediumSensitivity.strokeColor = strokeDefault
        binding.cardMediumSensitivity.strokeWidth = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._1sdp)
        binding.imgRadioMedium.setImageResource(R.drawable.ic_radio_unchecked)
        binding.txtMedium.setTextColor(defaultText)

        binding.cardHighSensitivity.setCardBackgroundColor(whiteColor)
        binding.cardHighSensitivity.strokeColor = strokeDefault
        binding.cardHighSensitivity.strokeWidth = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._1sdp)
        binding.imgRadioHigh.setImageResource(R.drawable.ic_radio_unchecked)
        binding.txtHigh.setTextColor(defaultText)

        when (sensitivity) {
            "Low" -> {
                binding.cardLowSensitivity.setCardBackgroundColor(selectedBgColor)
                binding.cardLowSensitivity.strokeColor = primaryColor
                binding.cardLowSensitivity.strokeWidth = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._2sdp)
                binding.imgRadioLow.setImageResource(R.drawable.ic_radio_checked)
                binding.txtLow.setTextColor(primaryColor)
            }
            "High" -> {
                binding.cardHighSensitivity.setCardBackgroundColor(selectedBgColor)
                binding.cardHighSensitivity.strokeColor = primaryColor
                binding.cardHighSensitivity.strokeWidth = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._2sdp)
                binding.imgRadioHigh.setImageResource(R.drawable.ic_radio_checked)
                binding.txtHigh.setTextColor(primaryColor)
            }
            else -> { // Medium
                binding.cardMediumSensitivity.setCardBackgroundColor(selectedBgColor)
                binding.cardMediumSensitivity.strokeColor = primaryColor
                binding.cardMediumSensitivity.strokeWidth = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._2sdp)
                binding.imgRadioMedium.setImageResource(R.drawable.ic_radio_checked)
                binding.txtMedium.setTextColor(primaryColor)
            }
        }
    }
}
