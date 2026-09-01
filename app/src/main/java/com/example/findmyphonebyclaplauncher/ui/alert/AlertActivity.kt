package com.example.findmyphonebyclaplauncher.ui.alert

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.findmyphonebyclaplauncher.App
import com.example.findmyphonebyclaplauncher.ads.LauncherAdsHelper
import com.example.findmyphonebyclaplauncher.ads.config.AdsConfigManager
import com.example.findmyphonebyclaplauncher.databinding.ActivityAlertBinding
import com.example.findmyphonebyclaplauncher.ui.common.BaseActivity
import com.example.findmyphonebyclaplauncher.utils.Constants
import kotlinx.coroutines.launch

/**
 * Full-screen alert activity shown when a clap/whistle sequence triggers Find Phone.
 *
 * Configures window flags to show over the lock screen and keep the screen on.
 * Calls [FindPhoneManager.stopFindPhone] when the user taps STOP or presses Back.
 */
class AlertActivity : BaseActivity() {

    private lateinit var binding: ActivityAlertBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Set window features FIRST before super.onCreate
        requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        // Show over lock screen BEFORE super.onCreate
        enableShowOverLockScreen()

        super.onCreate(savedInstanceState)

        // 2. Hide Support Action Bar if present
        supportActionBar?.hide()

        // 3. Inflate binding and set content view
        binding = ActivityAlertBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Handle explicit stop action from notification button
        if (intent?.action == Constants.ACTION_STOP_ALERT) {
            stopAlert()
            return
        }

        val findPhoneManager = (application as App).findPhoneManager
        if (!findPhoneManager.isAlertActive()) {
            finish()
            return
        }

        setupWindowInsets()
        loadBannerAd()

        binding.btnStopAlert.setOnClickListener {
            stopAlert()
        }

        // Start Lottie Bell animation
        binding.lottieBell.playAnimation()

        // Handle Back press
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                stopAlert()
            }
        })

        // Observe active-alert state so activity auto-closes when duration expires or alert is stopped
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                findPhoneManager.isAlertActiveState.collect { isActive ->
                    if (!isActive && !isFinishing) {
                        finish()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadBannerAd()
    }

    private fun loadBannerAd() {
        if (!com.example.findmyphonebyclaplauncher.util.NetworkUtil.isNetworkAvailable(this) ||
            !AdsConfigManager.config.canShowBannerAlertScreen
        ) {
            com.example.findmyphonebyclaplauncher.ads.BannerAdLoader.instance?.hideBannerContainer(
                binding.alertBanner.bannerAdFrameLayout,
                binding.alertBanner.bannerAdShimmerFrameLayout,
                binding.alertBanner.root
            )
            return
        }
        LauncherAdsHelper.showAlertBanner(
            this,
            binding.alertBanner.bannerAdFrameLayout,
            binding.alertBanner.bannerAdShimmerFrameLayout,
            binding.alertBanner.root
        )
    }

    private fun setupWindowInsets() {
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Constants.ACTION_STOP_ALERT) {
            stopAlert()
        }
    }

    override fun onDestroy() {
        com.example.findmyphonebyclaplauncher.ads.BannerAdLoader.instance?.destroyBanner(binding.alertBanner.bannerAdFrameLayout)
        super.onDestroy()
        // Stop alert only if the activity is explicitly finishing (user stop / duration timeout)
        if (isFinishing && !isChangingConfigurations) {
            stopAlert()
        }
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        stopAlert()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun stopAlert() {
        (application as App).findPhoneManager.stopFindPhone()
        if (!isFinishing) {
            finish()
        }
    }

    private fun enableShowOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
