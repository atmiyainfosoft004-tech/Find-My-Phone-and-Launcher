package com.example.findmyphonebyclaplauncher.ui.onboarding.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.findmyphonebyclaplauncher.databinding.FragmentOnboardingScreen6Binding
import com.example.findmyphonebyclaplauncher.ui.onboarding.OnboardingViewModel

class OnboardingScreen6Fragment : Fragment() {

    private var _binding: FragmentOnboardingScreen6Binding? = null
    private val binding get() = _binding!!

    private val viewModel: OnboardingViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingScreen6Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnGotIt.setOnClickListener {
            viewModel.completeOnboarding()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
