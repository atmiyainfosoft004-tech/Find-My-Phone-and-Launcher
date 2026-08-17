package com.example.findmyphonebyclaplauncher.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.findmyphonebyclaplauncher.ads.config.AdsConfigManager
import com.example.findmyphonebyclaplauncher.data.local.UserPreferencesDataSource
import com.example.findmyphonebyclaplauncher.databinding.ActivitySplashBinding
import com.example.findmyphonebyclaplauncher.ui.findphone.FindPhoneActivity
import com.example.findmyphonebyclaplauncher.ui.onboarding.OnboardingActivity
import com.example.findmyphonebyclaplauncher.utils.LauncherHelper
import com.example.findmyphonebyclaplauncher.utils.PermissionManager
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private var hasNavigated = false
    private var timeoutJob: Job? = null

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

        fun proceedToNextScreen(reason: String) {
            if (hasNavigated || isFinishing || isDestroyed) return
            hasNavigated = true
            timeoutJob?.cancel()

            Log.d("SplashActivity", "Proceeding to next screen (trigger: $reason)")

            val targetClass = if (isDefaultLauncher && (hasPermission || isCompleted || isSkipped)) {
                FindPhoneActivity::class.java
            } else {
                OnboardingActivity::class.java
            }

            startActivity(Intent(this@SplashActivity, targetClass))
            finish()
        }

        // Fallback / Timeout safety mechanism (3.5s) to guarantee the user is never stuck
        timeoutJob = lifecycleScope.launch {
            delay(3500L)
            proceedToNextScreen("TimeoutFallback")
        }

        // Initialize and synchronize with Firebase Remote Config fetchAndActivate()
        AdsConfigManager.initialize(applicationContext) { success ->
            // In addition to AdsConfigManager, directly listen to FirebaseRemoteConfig addOnCompleteListener
            val remoteConfig = FirebaseRemoteConfig.getInstance()
            remoteConfig.fetchAndActivate().addOnCompleteListener(this) { task ->
                Log.d("SplashActivity", "RemoteConfig fetchAndActivate complete: isSuccessful=${task.isSuccessful}")
                lifecycleScope.launch {
                    // Small delay (minimum splash duration for smooth UX)
                    delay(1200L)
                    proceedToNextScreen("RemoteConfigComplete")
                }
            }
        }
    }

    override fun onDestroy() {
        timeoutJob?.cancel()
        super.onDestroy()
    }
}
