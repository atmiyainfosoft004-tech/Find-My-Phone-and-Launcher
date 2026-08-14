package com.example.findmyphonebyclaplauncher.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.findmyphonebyclaplauncher.data.local.UserPreferencesDataSource
import com.example.findmyphonebyclaplauncher.databinding.ActivitySplashBinding
import com.example.findmyphonebyclaplauncher.ui.findphone.FindPhoneActivity
import com.example.findmyphonebyclaplauncher.ui.onboarding.OnboardingActivity
import com.example.findmyphonebyclaplauncher.utils.LauncherHelper
import com.example.findmyphonebyclaplauncher.utils.PermissionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()

        if (savedInstanceState == null) {
            handleSplashTransition()
        }
    }

    private fun setupWindowInsets() {
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.root.setPadding(
                binding.root.paddingLeft,
                systemBars.top,
                binding.root.paddingRight,
                systemBars.bottom
            )
            insets
        }
    }

    private fun handleSplashTransition() {
        val userPrefs = UserPreferencesDataSource(applicationContext)
        val isCompleted = userPrefs.isOnboardingCompleted
        val isSkipped = userPrefs.isOnboardingSkipped
        val hasPermission = PermissionManager.hasRecordAudioPermission(applicationContext)
        val isDefaultLauncher = LauncherHelper.isDefaultLauncher(applicationContext)

        lifecycleScope.launch {
            delay(1500L) // Display new Figma Splash screen for 1.5 seconds
            if (isFinishing || isDestroyed) return@launch

            // If default launcher is set AND (permission given or onboarding completed/skipped) -> launch FindPhoneActivity.
            // Otherwise -> launch OnboardingActivity.
            val targetClass = if (isDefaultLauncher && (hasPermission || isCompleted || isSkipped)) {
                FindPhoneActivity::class.java
            } else {
                OnboardingActivity::class.java
            }

            startActivity(Intent(this@SplashActivity, targetClass))
            finish()
        }
    }
}
