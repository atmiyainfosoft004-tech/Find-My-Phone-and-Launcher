package com.example.findmyphonebyclaplauncher.ui.onboarding.fragments

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.findmyphonebyclaplauncher.R
import com.example.findmyphonebyclaplauncher.databinding.FragmentOnboardingScreen2Binding
import com.example.findmyphonebyclaplauncher.ui.onboarding.OnboardingActivity
import com.example.findmyphonebyclaplauncher.ui.onboarding.adapter.OnboardingPagerAdapter
import com.example.findmyphonebyclaplauncher.utils.LauncherHelper

class OnboardingScreen2Fragment : Fragment() {

    private var _binding: FragmentOnboardingScreen2Binding? = null
    private val binding get() = _binding!!

    private var isNavigated = false

    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            // Do nothing: block back press on fragment level
        }
    }

    private val defaultRoleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d(TAG, "defaultRoleLauncher result received: resultCode=${result.resultCode}")
        checkAndProceedIfDefault(source = "ActivityResultCallback")
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

        // Register in-screen back-press handler strictly for this screen
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressedCallback)

        // Trigger default prompt ONLY when btnContinue is clicked
        binding.btnContinue.setOnClickListener {
            if (isDefaultLauncher()) {
                proceedToNextScreen(source = "btnContinue_AlreadyDefault")
            } else {
                requestDefaultLauncher()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Real-time verification: automatically proceed if app is confirmed as default launcher
        checkAndProceedIfDefault(source = "onResume")
    }

    private fun checkAndProceedIfDefault(source: String) {
        if (!isAdded || isNavigated) return

        val isDefault = isDefaultLauncher()
        Log.d(TAG, "checkAndProceedIfDefault: isDefault=$isDefault, source=$source")

        if (isDefault) {
            proceedToNextScreen(source)
        }
    }

    private fun requestDefaultLauncher() {
        if (isNavigated) return

        if (isDefaultLauncher()) {
            proceedToNextScreen(source = "requestDefaultLauncher_AlreadyDefault")
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = requireContext().getSystemService(Context.ROLE_SERVICE) as? RoleManager
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                if (roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                    proceedToNextScreen(source = "RoleHeldDirect")
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
            context?.let { ctx ->
                Toast.makeText(ctx, R.string.set_as_default_launcher_required, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isDefaultLauncher(): Boolean {
        val ctx = context ?: return false
        return LauncherHelper.isDefaultLauncher(ctx)
    }

    private fun proceedToNextScreen(source: String) {
        if (isNavigated) return
        isNavigated = true
        backPressedCallback.isEnabled = false
        backPressedCallback.remove()
        Log.d(TAG, "proceedToNextScreen: Navigating to LanguageActivity (source: $source)")
        val intent = Intent(requireContext(), com.example.findmyphonebyclaplauncher.ui.language.LanguageActivity::class.java).apply {
            putExtra(com.example.findmyphonebyclaplauncher.ui.language.LanguageActivity.EXTRA_IS_FIRST_TIME, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        activity?.finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        backPressedCallback.isEnabled = false
        backPressedCallback.remove()
        _binding = null
    }

    companion object {
        private const val TAG = "DefaultHomeDebug"
    }
}
