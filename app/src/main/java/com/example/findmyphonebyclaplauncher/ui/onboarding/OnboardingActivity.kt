package com.example.findmyphonebyclaplauncher.ui.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.findmyphonebyclaplauncher.databinding.ActivityOnboardingBinding
import com.example.findmyphonebyclaplauncher.ui.common.BaseActivity
import com.example.findmyphonebyclaplauncher.ui.findphone.FindPhoneActivity
import com.example.findmyphonebyclaplauncher.ui.onboarding.adapter.OnboardingPagerAdapter

class OnboardingActivity : BaseActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    val viewModel: OnboardingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        setupViewPager()
        observeViewModel()
        setupBackPressedHandler()
    }

    private fun setupWindowInsets() {
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true
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

    private fun setupViewPager() {
        val adapter = OnboardingPagerAdapter(this)
        binding.vpOnboarding.adapter = adapter
        binding.vpOnboarding.isUserInputEnabled = false

        binding.vpOnboarding.setCurrentItem(OnboardingPagerAdapter.PAGE_SCREEN_1, false)
    }

    private fun observeViewModel() {
        viewModel.onboardingComplete.observe(this) { complete ->
            if (complete) navigateToMain()
        }
    }

    private fun setupBackPressedHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Ignore back button clicks/gestures across onboarding
            }
        })
    }

    fun goToNextPage() {
        val next = binding.vpOnboarding.currentItem + 1
        if (next < OnboardingPagerAdapter.PAGE_COUNT) {
            binding.vpOnboarding.currentItem = next
        } else {
            viewModel.completeOnboarding()
        }
    }

    fun navigateToPage(pageIndex: Int) {
        if (pageIndex in 0 until OnboardingPagerAdapter.PAGE_COUNT) {
            binding.vpOnboarding.currentItem = pageIndex
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, FindPhoneActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
