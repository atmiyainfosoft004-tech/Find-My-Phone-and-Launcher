package com.example.findmyphonebyclaplauncher.ui.onboarding.fragments

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.findmyphonebyclaplauncher.databinding.FragmentOnboardingScreen2Binding
import com.example.findmyphonebyclaplauncher.ui.onboarding.OnboardingActivity
import com.example.findmyphonebyclaplauncher.ui.onboarding.adapter.OnboardingPagerAdapter
import com.example.findmyphonebyclaplauncher.utils.LauncherHelper

class OnboardingScreen2Fragment : Fragment() {

    private var _binding: FragmentOnboardingScreen2Binding? = null
    private val binding get() = _binding!!

    private var awaitingSettingsReturn = false
    private var isNavigated = false

    private val defaultRoleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d(TAG, "defaultRoleLauncher ActivityResultCallback received: resultCode=${result.resultCode}")
        checkDefaultHomeAppAndProceed(forceProceed = true, source = "ActivityResultCallback")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingScreen2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isNavigated = false
        Log.d(TAG, "onViewCreated: isDefaultLauncher=${isDefaultLauncher()}")

        binding.btnContinue.setOnClickListener {
            val isDefault = isDefaultLauncher()
            Log.d(TAG, "btnContinue clicked: isDefault=$isDefault, awaitingSettingsReturn=$awaitingSettingsReturn")
            if (isDefault || awaitingSettingsReturn) {
                checkDefaultHomeAppAndProceed(forceProceed = true, source = "btnContinue_Proceed")
            } else {
                requestDefaultLauncher()
            }
        }

        binding.cardFindMyPhoneOption.setOnClickListener {
            Log.d(TAG, "cardFindMyPhoneOption clicked")
            requestDefaultLauncher()
        }
    }

    override fun onResume() {
        super.onResume()
        val isDefault = isDefaultLauncher()
        Log.d(TAG, "onResume: isDefault=$isDefault, awaitingSettingsReturn=$awaitingSettingsReturn, isNavigated=$isNavigated")

        // Dual-validation: If app is confirmed default launcher or returning from system prompt
        if (!isNavigated && (isDefault || awaitingSettingsReturn)) {
            checkDefaultHomeAppAndProceed(forceProceed = awaitingSettingsReturn, source = "onResume_DualCheck")
        }
    }

    private fun requestDefaultLauncher() {
        if (isNavigated) {
            Log.d(TAG, "requestDefaultLauncher: Aborted, already navigated")
            return
        }
        awaitingSettingsReturn = true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = requireContext().getSystemService(Context.ROLE_SERVICE) as? RoleManager
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                if (roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                    Log.d(TAG, "requestDefaultLauncher: ROLE_HOME is already held -> proceeding immediately")
                    checkDefaultHomeAppAndProceed(forceProceed = true, source = "RoleHeldDirect")
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
            Log.d(TAG, "No system launcher settings intent could be launched -> proceeding with fallback")
            checkDefaultHomeAppAndProceed(forceProceed = true, source = "NoIntentFallback")
        }
    }

    private fun isDefaultLauncher(): Boolean {
        val ctx = context ?: return false
        val roleHeld = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = ctx.getSystemService(Context.ROLE_SERVICE) as? RoleManager
            roleManager?.isRoleHeld(RoleManager.ROLE_HOME) == true
        } else {
            false
        }

        val pmDefault = runCatching {
            val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
            val resolveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ctx.packageManager.resolveActivity(
                    intent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                ctx.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            }
            resolveInfo?.activityInfo?.packageName == ctx.packageName
        }.getOrDefault(false)

        return roleHeld || pmDefault
    }

    private fun checkDefaultHomeAppAndProceed(
        forceProceed: Boolean = false,
        source: String = "unknown"
    ) {
        if (!isAdded || isNavigated) {
            Log.d(TAG, "checkDefaultHomeAppAndProceed: Aborted (isAdded=$isAdded, isNavigated=$isNavigated, source=$source)")
            return
        }

        val isDefault = isDefaultLauncher()
        Log.d(TAG, "checkDefaultHomeAppAndProceed: isDefault=$isDefault, forceProceed=$forceProceed, source=$source")

        if (isDefault || forceProceed) {
            isNavigated = true
            awaitingSettingsReturn = false
            navigateToScreen1(source)
        }
    }

    private fun navigateToScreen1(source: String) {
        Log.d(TAG, "navigateToScreen1: Triggering navigation strictly to OnboardingScreen1Fragment (index 1) [source: $source]")
        (activity as? OnboardingActivity)?.navigateToPage(OnboardingPagerAdapter.PAGE_SCREEN_1)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "DefaultHomeDebug"
    }
}
