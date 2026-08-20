package com.example.findmyphonebyclaplauncher.ui.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import android.util.Log
import com.example.findmyphonebyclaplauncher.data.local.UserPreferencesDataSource
import com.example.findmyphonebyclaplauncher.databinding.ActivityOnboardingBinding
import com.example.findmyphonebyclaplauncher.ui.common.BaseActivity
import com.example.findmyphonebyclaplauncher.ui.findphone.FindPhoneActivity
import com.example.findmyphonebyclaplauncher.ui.onboarding.adapter.OnboardingPagerAdapter
import com.example.findmyphonebyclaplauncher.ui.onboarding.fragments.OnboardingScreen4Fragment

class OnboardingActivity : BaseActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    val viewModel: OnboardingViewModel by viewModels()
    private var isNavigatingToMain = false
    private var isUserLeaving = false
    private var isAppInBackground = false

    @Volatile
    var isRequestingPermission: Boolean = false

    private var initialPage: Int = OnboardingPagerAdapter.PAGE_SCREEN_1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            initialPage = savedInstanceState.getInt("KEY_SAVED_PAGE", OnboardingPagerAdapter.PAGE_SCREEN_1)
        }
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        setupViewPager(initialPage)
        observeViewModel()
        setupBackPressedHandler()
        loadBannerAd()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::binding.isInitialized) {
            outState.putInt("KEY_SAVED_PAGE", binding.vpOnboarding.currentItem)
        }
    }

    override fun onResume() {
        super.onResume()
        loadBannerAd()
        isUserLeaving = false
        val prefs = UserPreferencesDataSource(this)
        if (prefs.isOnboardingCompleted && !isRequestingPermission) {
            navigateToMain()
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (!isNavigatingToMain && !isFinishing && !isRequestingPermission) {
            isUserLeaving = true
            redirectToHomeFragment("onUserLeaveHint")
        }
    }

    override fun onStop() {
        super.onStop()
        if (isUserLeaving && !isNavigatingToMain && !isFinishing && !isRequestingPermission) {
            isAppInBackground = true
        }
    }

    override fun onRestart() {
        super.onRestart()
        if (isUserLeaving && isAppInBackground && !isNavigatingToMain && !isRequestingPermission) {
            redirectToHomeFragment("onRestart")
        }
    }

    private fun redirectToHomeFragment(source: String) {
        if (isFinishing || isDestroyed || isNavigatingToMain || isRequestingPermission) return
        isNavigatingToMain = true
        Log.d("OnboardingActivity", "Redirecting to HomeFragment due to Home press / background resume (source: $source)")

        val prefs = UserPreferencesDataSource(this)
        prefs.isLanguageSelected = true
        prefs.isOnboardingCompleted = true

        val intent = Intent(this, FindPhoneActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun loadBannerAd() {
        if (!com.example.findmyphonebyclaplauncher.util.NetworkUtil.isNetworkAvailable(this) ||
            !com.example.findmyphonebyclaplauncher.ads.config.AdsConfigManager.config.canShowBannerOnboarding
        ) {
            com.example.findmyphonebyclaplauncher.ads.BannerAdLoader.instance?.hideBannerContainer(
                binding.onboardingBanner.bannerAdFrameLayout,
                binding.onboardingBanner.bannerAdShimmerFrameLayout,
                binding.onboardingBanner.root
            )
            return
        }
        if (binding.onboardingBanner.bannerAdFrameLayout.childCount > 0) return
        com.example.findmyphonebyclaplauncher.ads.LauncherAdsHelper.showOnboardingBanner(
            this,
            binding.onboardingBanner.bannerAdFrameLayout,
            binding.onboardingBanner.bannerAdShimmerFrameLayout,
            binding.onboardingBanner.root
        )
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

    private fun setupViewPager(startPage: Int = OnboardingPagerAdapter.PAGE_SCREEN_1) {
        val adapter = OnboardingPagerAdapter(this)
        binding.vpOnboarding.adapter = adapter
        binding.vpOnboarding.isUserInputEnabled = false

        binding.vpOnboarding.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                for (fragment in supportFragmentManager.fragments) {
                    if (fragment is OnboardingScreen4Fragment) {
                        if (position == OnboardingPagerAdapter.PAGE_SCREEN_4) {
                            fragment.onPageVisible()
                        } else {
                            fragment.onPageInvisible()
                        }
                    }
                }
            }
        })

        binding.vpOnboarding.setCurrentItem(startPage, false)
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

    fun getCurrentPage(): Int = binding.vpOnboarding.currentItem

    fun goToNextPage() {
        val next = binding.vpOnboarding.currentItem + 1
        if (next < OnboardingPagerAdapter.PAGE_COUNT) {
            binding.vpOnboarding.setCurrentItem(next, true)
        } else {
            viewModel.completeOnboarding()
        }
    }

    fun navigateToPage(pageIndex: Int) {
        if (pageIndex in 0 until OnboardingPagerAdapter.PAGE_COUNT) {
            binding.vpOnboarding.setCurrentItem(pageIndex, true)
        }
    }

    private fun navigateToMain() {
        isNavigatingToMain = true
        val intent = Intent(this, FindPhoneActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
