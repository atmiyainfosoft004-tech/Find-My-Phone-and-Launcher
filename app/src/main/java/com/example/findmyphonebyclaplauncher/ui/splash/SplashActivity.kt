package com.example.findmyphonebyclaplauncher.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.findmyphonebyclaplauncher.data.local.UserPreferencesDataSource
import com.example.findmyphonebyclaplauncher.databinding.ActivitySplashBinding
import com.example.findmyphonebyclaplauncher.ui.common.BaseActivity
import com.example.findmyphonebyclaplauncher.ui.findphone.FindPhoneActivity
import com.example.findmyphonebyclaplauncher.ui.onboarding.OnboardingActivity
import com.example.findmyphonebyclaplauncher.ui.onboarding.OnboardingScreen2Activity
import com.example.findmyphonebyclaplauncher.utils.LauncherHelper
import com.example.findmyphonebyclaplauncher.utils.PermissionManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity() {

    private lateinit var binding: ActivitySplashBinding
    private var hasNavigated = false
    private var timeoutJob: Job? = null

    private val TAG = "SplashActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        handleSplashTransition()
    }

    private fun setupWindowInsets() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
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
        val isLanguageSelected = userPrefs.isLanguageSelected
        val isDefaultLauncher = LauncherHelper.isDefaultLauncher(applicationContext)

        fun proceedToNextScreen(reason: String) {
            if (hasNavigated || isFinishing || isDestroyed) return
            hasNavigated = true
            timeoutJob?.cancel()

            Log.d("SplashActivity", "Proceeding to next screen (trigger: $reason)")

            val targetClass = when {
                !isLanguageSelected -> {
                    if (isDefaultLauncher) {
                        Log.e(TAG, "proceedToNextScreen: LanguageActivity", )
                        com.example.findmyphonebyclaplauncher.ui.language.LanguageActivity::class.java
                    } else {
                        Log.e(TAG, "proceedToNextScreen: OnboardingScreen2Activity", )

                        OnboardingScreen2Activity::class.java
                    }
                }
                !isCompleted && !isSkipped -> {
                    Log.e(TAG, "proceedToNextScreen: OnboardingActivity", )

                    OnboardingActivity::class.java
                }

                else -> {
                    Log.e(TAG, "proceedToNextScreen: FindPhoneActivity", )

                    FindPhoneActivity::class.java
                }
            }

            val intent = Intent(this@SplashActivity, targetClass).apply {
                if (targetClass == com.example.findmyphonebyclaplauncher.ui.language.LanguageActivity::class.java) {
                    putExtra(com.example.findmyphonebyclaplauncher.ui.language.LanguageActivity.EXTRA_IS_FIRST_TIME, true)
                    putExtra("isFirstTime", true)
                }
            }
            startActivity(intent)
            finish()
        }

        // Timeout safety mechanism (1.5s splash display duration)
        timeoutJob = lifecycleScope.launch {
            delay(1500)
            proceedToNextScreen("TimeoutReached")
        }
    }

    override fun onDestroy() {
        timeoutJob?.cancel()
        super.onDestroy()
    }
}
