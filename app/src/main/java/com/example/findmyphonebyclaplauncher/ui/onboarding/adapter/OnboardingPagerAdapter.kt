package com.example.findmyphonebyclaplauncher.ui.onboarding.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.findmyphonebyclaplauncher.ui.onboarding.fragments.OnboardingScreen1Fragment
import com.example.findmyphonebyclaplauncher.ui.onboarding.fragments.OnboardingScreen2Fragment
import com.example.findmyphonebyclaplauncher.ui.onboarding.fragments.OnboardingScreen3Fragment
import com.example.findmyphonebyclaplauncher.ui.onboarding.fragments.OnboardingScreen4Fragment
import com.example.findmyphonebyclaplauncher.ui.onboarding.fragments.OnboardingScreen5Fragment
import com.example.findmyphonebyclaplauncher.ui.onboarding.fragments.OnboardingScreen6Fragment

class OnboardingPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    companion object {
        const val PAGE_COUNT = 6
        const val PAGE_SCREEN_2 = 0
        const val PAGE_SCREEN_1 = 1
        const val PAGE_SCREEN_3 = 2
        const val PAGE_SCREEN_4 = 3
        const val PAGE_SCREEN_5 = 4
        const val PAGE_SCREEN_6 = 5
    }

    override fun getItemCount(): Int = PAGE_COUNT

    override fun createFragment(position: Int): Fragment = when (position) {
        PAGE_SCREEN_2 -> OnboardingScreen2Fragment()
        PAGE_SCREEN_1 -> OnboardingScreen1Fragment()
        PAGE_SCREEN_3 -> OnboardingScreen3Fragment()
        PAGE_SCREEN_4 -> OnboardingScreen4Fragment()
        PAGE_SCREEN_5 -> OnboardingScreen5Fragment()
        PAGE_SCREEN_6 -> OnboardingScreen6Fragment()
        else          -> OnboardingScreen2Fragment()
    }
}
