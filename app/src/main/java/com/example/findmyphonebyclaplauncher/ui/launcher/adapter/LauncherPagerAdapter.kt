package com.example.findmyphonebyclaplauncher.ui.launcher.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.findmyphonebyclaplauncher.ui.launcher.dashboard.DashboardFragment
import com.example.findmyphonebyclaplauncher.ui.launcher.findphone.FindPhoneFragment
import com.example.findmyphonebyclaplauncher.ui.launcher.home.HomeFragment

class LauncherPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment = when (position) {
        PAGE_FIND_PHONE -> FindPhoneFragment()
        PAGE_HOME -> HomeFragment()
        PAGE_DASHBOARD -> DashboardFragment()
        else -> HomeFragment()
    }

    companion object {
        /** Left: Find My Phone App content (swiped left-to-right from home) */
        const val PAGE_FIND_PHONE = 0
        /** Center: Home launcher screen */
        const val PAGE_HOME = 1
        /** Right: Quick dashboard / suggested & recent apps */
        const val PAGE_DASHBOARD = 2
    }
}
