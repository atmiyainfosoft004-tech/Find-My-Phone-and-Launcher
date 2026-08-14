package com.example.findmyphonebyclaplauncher.ui.onboarding.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.findmyphonebyclaplauncher.ui.onboarding.fragments.OnboardingScreen1Fragment
import com.example.findmyphonebyclaplauncher.ui.onboarding.fragments.OnboardingScreen3Fragment
import com.example.findmyphonebyclaplauncher.ui.onboarding.fragments.OnboardingScreen4Fragment
import com.example.findmyphonebyclaplauncher.ui.onboarding.fragments.OnboardingScreen5Fragment
import com.example.findmyphonebyclaplauncher.ui.onboarding.fragments.OnboardingScreen6Fragment

class OnboardingPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    companion object {
        const val PAGE_COUNT = 5
        const val PAGE_SCREEN_1 = 0
        const val PAGE_SCREEN_3 = 1
        const val PAGE_SCREEN_4 = 2
        const val PAGE_SCREEN_5 = 3
        const val PAGE_SCREEN_6 = 4
    }

    override fun getItemCount(): Int = PAGE_COUNT

    override fun createFragment(position: Int): Fragment = when (position) {
        PAGE_SCREEN_1 -> OnboardingScreen1Fragment()
        PAGE_SCREEN_3 -> OnboardingScreen3Fragment()
        PAGE_SCREEN_4 -> OnboardingScreen4Fragment()
        PAGE_SCREEN_5 -> OnboardingScreen5Fragment()
        PAGE_SCREEN_6 -> OnboardingScreen6Fragment()
        else          -> OnboardingScreen1Fragment()
    }
}
