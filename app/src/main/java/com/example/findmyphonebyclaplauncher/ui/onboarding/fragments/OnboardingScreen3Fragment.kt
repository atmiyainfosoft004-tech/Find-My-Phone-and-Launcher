package com.example.findmyphonebyclaplauncher.ui.onboarding.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.findmyphonebyclaplauncher.R
import com.example.findmyphonebyclaplauncher.databinding.FragmentOnboardingScreen3Binding
import com.example.findmyphonebyclaplauncher.ui.onboarding.OnboardingActivity
import com.example.findmyphonebyclaplauncher.ui.onboarding.adapter.OnboardingPagerAdapter
import com.example.findmyphonebyclaplauncher.utils.PermissionManager

/**
 * Screen 3: Permission Request (Audio Access Needed)
 * Tapping Continue checks/requests runtime RECORD_AUDIO permission.
 * If granted -> advances to Screen 4 (OnboardingScreen4Fragment).
 * If denied  -> stays on this page, enabling the user to tap Continue again to re-request.
 */
class OnboardingScreen3Fragment : Fragment() {

    private var _binding: FragmentOnboardingScreen3Binding? = null
    private val binding get() = _binding!!

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        navigateToNextScreen()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingScreen3Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnContinue.setOnClickListener {
            handleContinueClick()
        }
    }

    private fun handleContinueClick() {
        val ctx = context ?: return
        if (PermissionManager.hasRecordAudioPermission(ctx)) {
            navigateToNextScreen()
        } else {
            requestPermissions()
        }
    }

    private fun requestPermissions() {
        val ctx = context ?: return
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(ctx, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            requestPermissionsLauncher.launch(missing.toTypedArray())
        } else {
            navigateToNextScreen()
        }
    }

    private fun navigateToNextScreen() {
        val onboardingActivity = activity as? OnboardingActivity
        if (onboardingActivity != null) {
            onboardingActivity.navigateToPage(OnboardingPagerAdapter.PAGE_SCREEN_4)
        } else {
            try {
                findNavController().navigate(R.id.action_onboardingScreen3_to_onboardingScreen4)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

