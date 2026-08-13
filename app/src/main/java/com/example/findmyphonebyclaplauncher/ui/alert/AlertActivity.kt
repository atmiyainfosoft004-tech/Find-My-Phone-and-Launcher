package com.example.findmyphonebyclaplauncher.ui.alert

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.findmyphonebyclaplauncher.App
import com.example.findmyphonebyclaplauncher.databinding.ActivityAlertBinding
import com.example.findmyphonebyclaplauncher.utils.Constants

/**
 * Full-screen alert activity shown when a clap/whistle sequence triggers Find Phone.
 *
 * Configures window flags to show over the lock screen and keep the screen on.
 * Calls [FindPhoneManager.stopFindPhone] when the user taps STOP.
 */
class AlertActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlertBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        // Show over lock screen BEFORE super.onCreate / setContentView
        enableShowOverLockScreen()
        super.onCreate(savedInstanceState)
        binding = ActivityAlertBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()

        binding.btnStopAlert.setOnClickListener {
            stopAlert()
        }

        // If this activity is opened via the notification action, also stop there
        if (intent?.action == Constants.ACTION_STOP_ALERT) {
            stopAlert()
        }
    }

    private fun setupWindowInsets() {
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
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

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        if (intent.action == Constants.ACTION_STOP_ALERT) {
            stopAlert()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Safety net: stop alert if activity is destroyed by the system
        stopAlert()
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        // Prevent dismissing the alert with Back
        // User must explicitly tap the STOP button
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun stopAlert() {
        (application as App).findPhoneManager.stopFindPhone()
        finish()
    }

    private fun enableShowOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
