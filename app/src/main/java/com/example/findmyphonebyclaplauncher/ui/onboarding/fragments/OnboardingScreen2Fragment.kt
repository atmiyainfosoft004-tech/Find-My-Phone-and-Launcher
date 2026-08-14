package com.example.findmyphonebyclaplauncher.ui.onboarding.fragments

import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.findmyphonebyclaplauncher.databinding.FragmentOnboardingScreen2Binding
import com.example.findmyphonebyclaplauncher.ui.onboarding.OnboardingActivity

class OnboardingScreen2Fragment : Fragment() {

    private var _binding: FragmentOnboardingScreen2Binding? = null
    private val binding get() = _binding!!

    private var awaitingSettingsReturn = false

    private val homeRoleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        checkLauncherAndProceed()
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

        binding.btnContinue.setOnClickListener { requestDefaultLauncher() }
        binding.cardFindMyPhoneOption.setOnClickListener { requestDefaultLauncher() }
    }

    override fun onResume() {
        super.onResume()
        if (awaitingSettingsReturn) {
            checkLauncherAndProceed()
        }
    }

    private fun requestDefaultLauncher() {
        awaitingSettingsReturn = true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = requireContext().getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                if (roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                    (activity as? OnboardingActivity)?.goToNextPage()
                    return
                }
                var roleIntentLaunched = false
                runCatching {
                    val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
                    homeRoleLauncher.launch(intent)
                    roleIntentLaunched = true
                }
                if (roleIntentLaunched) return
            }
        }

        var launched = false
        runCatching {
            homeRoleLauncher.launch(Intent(Settings.ACTION_HOME_SETTINGS))
            launched = true
        }
        if (launched) return

        runCatching {
            homeRoleLauncher.launch(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
            launched = true
        }
        if (launched) return

        runCatching {
            homeRoleLauncher.launch(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME))
        }
    }

    private fun isDefaultLauncher(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = requireContext().getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                return roleManager.isRoleHeld(RoleManager.ROLE_HOME)
            }
        }
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolve = requireContext().packageManager.resolveActivity(intent, 0)
        return resolve?.activityInfo?.packageName == requireContext().packageName
    }

    private fun checkLauncherAndProceed() {
        if (isAdded) {
            if (isDefaultLauncher()) {
                awaitingSettingsReturn = false
                (activity as? OnboardingActivity)?.goToNextPage()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
