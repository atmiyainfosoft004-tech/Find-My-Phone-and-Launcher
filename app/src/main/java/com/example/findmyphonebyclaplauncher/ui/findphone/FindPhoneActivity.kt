package com.example.findmyphonebyclaplauncher.ui.findphone

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.findmyphonebyclaplauncher.R
import com.example.findmyphonebyclaplauncher.data.local.UserPreferencesDataSource
import com.example.findmyphonebyclaplauncher.databinding.ActivityLauncherBinding
import com.example.findmyphonebyclaplauncher.ui.launcher.LauncherHostFragment
import com.example.findmyphonebyclaplauncher.ui.launcher.adapter.LauncherPagerAdapter
import com.example.findmyphonebyclaplauncher.utils.LauncherHelper

class FindPhoneActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLauncherBinding
    private lateinit var prefs: UserPreferencesDataSource

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = UserPreferencesDataSource(this)

        if (!prefs.isOnboardingCompleted && !prefs.isOnboardingSkipped) {
            startActivity(Intent(this, com.example.findmyphonebyclaplauncher.ui.onboarding.OnboardingActivity::class.java))
            finish()
            return
        }

        enableEdgeToEdge()
        showSystemWallpaperBehindWindow()
        binding = ActivityLauncherBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configureTransparentSystemBars()
        applyLauncherPageSystemBars(LauncherPagerAdapter.PAGE_HOME)
        setupBackPressedHandler()
        handleHomeIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleHomeIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        configureTransparentSystemBars()
        val host = supportFragmentManager.findFragmentById(R.id.launcherHostContainer) as? LauncherHostFragment
        host?.reapplyPageSystemBars()
        com.example.findmyphonebyclaplauncher.ads.config.AdsConfigManager.fetchAndActivate()
    }

    private fun showSystemWallpaperBehindWindow() {
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    private fun configureTransparentSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        @Suppress("DEPRECATION")
        run {
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
    }

    fun applyLauncherPageSystemBars(page: Int) {
        configureTransparentSystemBars()
        val lightTheme = page == LauncherPagerAdapter.PAGE_FIND_PHONE
        WindowInsetsControllerCompat(window, binding.root).apply {
            isAppearanceLightStatusBars = lightTheme
            isAppearanceLightNavigationBars = lightTheme
        }
    }

    private fun handleHomeIntent(intent: Intent?) {
        if (intent == null) return
        if (!intent.hasCategory(Intent.CATEGORY_HOME)) return
        val host = supportFragmentManager.findFragmentById(R.id.launcherHostContainer) as? LauncherHostFragment
        host?.onHomePressed()
    }

    private fun setupBackPressedHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val host = supportFragmentManager.findFragmentById(R.id.launcherHostContainer) as? LauncherHostFragment
                if (host?.onLauncherBackPressed() == true) {
                    return
                }
                if (LauncherHelper.isDefaultLauncher(this@FindPhoneActivity)) {
                    moveTaskToBack(true)
                } else {
                    finish()
                }
            }
        })
    }
}
