package com.example.findmyphonebyclaplauncher.ui.onboarding

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.findmyphonebyclaplauncher.R
import com.example.findmyphonebyclaplauncher.databinding.FragmentOnboardingScreen2Binding
import com.example.findmyphonebyclaplauncher.ui.common.BaseActivity
import com.example.findmyphonebyclaplauncher.ui.language.LanguageActivity
import com.example.findmyphonebyclaplauncher.utils.LauncherHelper

class OnboardingScreen2Activity : BaseActivity() {

    private lateinit var binding: FragmentOnboardingScreen2Binding
    @Volatile
    private var isNavigated = false

    private val defaultRoleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d(TAG, "defaultRoleLauncher result received: resultCode=${result.resultCode}")
        checkAndProceedIfDefault(source = "ActivityResultCallback")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentOnboardingScreen2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        setupBackPressedHandler()

        binding.btnContinue.setOnClickListener {
            if (isDefaultLauncher()) {
                navigateToLanguageScreen()
            } else {
                requestDefaultLauncher()
            }
        }
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

    private fun setupBackPressedHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Kills the app process completely on back press
                finishAffinity()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        binding.shimmerBtnContinue.startShimmer()
        checkAndProceedIfDefault(source = "onResume")
    }

    override fun onPause() {
        binding.shimmerBtnContinue.stopShimmer()
        super.onPause()
    }

    private fun checkAndProceedIfDefault(source: String) {
        if (isNavigated) return

        val isDefault = isDefaultLauncher()
        Log.d(TAG, "checkAndProceedIfDefault: isDefault=$isDefault, source=$source")

        if (isDefault) {
            navigateToLanguageScreen()
        }
    }

    private fun requestDefaultLauncher() {
        if (isNavigated) return

        if (isDefaultLauncher()) {
            navigateToLanguageScreen()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(Context.ROLE_SERVICE) as? RoleManager
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                if (roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                    navigateToLanguageScreen()
                    return
                }
                var roleIntentLaunched = false
                runCatching {
                    Log.d(TAG, "Launching RoleManager.ROLE_HOME request intent")
                    val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
                    defaultRoleLauncher.launch(intent)
                    roleIntentLaunched = true
                }.onFailure { e ->
                    Log.e(TAG, "Failed to launch RoleManager intent: ${e.message}", e)
                }
                if (roleIntentLaunched) return
            }
        }

        var launched = false
        runCatching {
            Log.d(TAG, "Launching ACTION_HOME_SETTINGS intent")
            defaultRoleLauncher.launch(Intent(Settings.ACTION_HOME_SETTINGS))
            launched = true
        }.onFailure { e ->
            Log.e(TAG, "Failed to launch ACTION_HOME_SETTINGS: ${e.message}", e)
        }
        if (launched) return

        runCatching {
            Log.d(TAG, "Launching ACTION_MANAGE_DEFAULT_APPS_SETTINGS intent")
            defaultRoleLauncher.launch(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
            launched = true
        }.onFailure { e ->
            Log.e(TAG, "Failed to launch ACTION_MANAGE_DEFAULT_APPS_SETTINGS: ${e.message}", e)
        }
        if (launched) return

        runCatching {
            Log.d(TAG, "Launching Intent.ACTION_MAIN CATEGORY_HOME intent")
            defaultRoleLauncher.launch(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME))
            launched = true
        }.onFailure { e ->
            Log.e(TAG, "Failed to launch CATEGORY_HOME intent: ${e.message}", e)
        }

        if (!launched) {
            Log.d(TAG, "No system launcher settings intent could be launched")
            Toast.makeText(this, R.string.set_as_default_launcher_required, Toast.LENGTH_SHORT).show()
        }
    }

    private fun isDefaultLauncher(): Boolean {
        return LauncherHelper.isDefaultLauncher(this)
    }

    private fun navigateToLanguageScreen() {
        runOnUiThread {
            if (isNavigated) return@runOnUiThread
            isNavigated = true
            Log.d(TAG, "navigateToLanguageScreen: Navigating strictly to LanguageActivity")
            val intent = Intent(this, LanguageActivity::class.java).apply {
                putExtra(LanguageActivity.EXTRA_IS_FIRST_TIME, true)
                putExtra("isFirstTime", true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
    }

    companion object {
        private const val TAG = "OnboardingScreen2Activity"
    }
}
