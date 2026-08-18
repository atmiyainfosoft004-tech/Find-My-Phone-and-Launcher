package com.example.findmyphonebyclaplauncher.ui.settings

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.example.findmyphonebyclaplauncher.R
import com.example.findmyphonebyclaplauncher.databinding.ActivitySettingsBinding
import com.example.findmyphonebyclaplauncher.utils.BatteryOptimizationManager
import com.example.findmyphonebyclaplauncher.utils.OemCompatibilityManager
import com.google.android.material.snackbar.Snackbar

import androidx.activity.OnBackPressedCallback
import com.example.findmyphonebyclaplauncher.ui.common.BaseActivity
import com.example.findmyphonebyclaplauncher.util.finishWithSlideAnimation

class SettingsActivity : BaseActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        setupToolbar()
        setupBatteryOptimization()
        setupOemSection()
    }

    private fun setupWindowInsets() {
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.toolbar.setPadding(
                binding.toolbar.paddingLeft,
                systemBars.top,
                binding.toolbar.paddingRight,
                binding.toolbar.paddingBottom
            )
            binding.root.setPadding(
                binding.root.paddingLeft,
                0,
                binding.root.paddingRight,
                systemBars.bottom
            )
            insets
        }
    }

    override fun onResume() {
        super.onResume()
        refreshBatteryStatus()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            com.example.findmyphonebyclaplauncher.ads.LauncherAdsHelper.showBackAd(this) {
                finishWithSlideAnimation()
            }
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                com.example.findmyphonebyclaplauncher.ads.LauncherAdsHelper.showBackAd(this@SettingsActivity) {
                    finishWithSlideAnimation()
                }
            }
        })
    }

    private fun setupBatteryOptimization() {
        refreshBatteryStatus()

        binding.btnBatteryOptimization.setOnClickListener {
            BatteryOptimizationManager.requestIgnoreBatteryOptimizations(this)
        }
    }

    private fun setupOemSection() {
        val hasOemSettings = OemCompatibilityManager.hasKnownAutostartSettings()
        binding.cardOemAutostart.isVisible = hasOemSettings

        if (hasOemSettings) {
            binding.txtOemBrand.text = getString(
                R.string.oem_autostart_description,
                OemCompatibilityManager.deviceBrand.replaceFirstChar { it.uppercase() }
            )

            binding.btnOemAutostart.setOnClickListener {
                val opened = OemCompatibilityManager.openAutostartSettings(this)
                if (!opened) {
                    Snackbar.make(
                        binding.root,
                        getString(R.string.oem_settings_not_found),
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun refreshBatteryStatus() {
        val isIgnoring = BatteryOptimizationManager.isIgnoringBatteryOptimizations(this)
        if (isIgnoring) {
            binding.txtBatteryStatus.text  = getString(R.string.battery_status_unrestricted)
            binding.txtBatteryStatus.setTextColor(getColor(R.color.color_success))
            binding.btnBatteryOptimization.isVisible = false
        } else {
            binding.txtBatteryStatus.text  = getString(R.string.battery_status_restricted)
            binding.txtBatteryStatus.setTextColor(getColor(R.color.color_warning))
            binding.btnBatteryOptimization.isVisible = true
        }
    }
}
