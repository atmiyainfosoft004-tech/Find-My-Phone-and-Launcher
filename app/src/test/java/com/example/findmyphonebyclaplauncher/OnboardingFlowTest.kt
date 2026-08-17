package com.example.findmyphonebyclaplauncher

import com.example.findmyphonebyclaplauncher.ui.onboarding.adapter.OnboardingPagerAdapter
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests verifying that the onboarding sequence strictly starts with Screen 2 followed by Screen 1.
 */
class OnboardingFlowTest {

    @Test
    fun onboardingPagerIndices_areStrictlyReversed() {
        assertEquals("Initial onboarding page (index 0) must be Screen 2", 0, OnboardingPagerAdapter.PAGE_SCREEN_2)
        assertEquals("Second onboarding page (index 1) must be Screen 1", 1, OnboardingPagerAdapter.PAGE_SCREEN_1)
        assertEquals("Third onboarding page (index 2) must be Screen 3", 2, OnboardingPagerAdapter.PAGE_SCREEN_3)
        assertEquals("Fourth onboarding page (index 3) must be Screen 4", 3, OnboardingPagerAdapter.PAGE_SCREEN_4)
        assertEquals("Fifth onboarding page (index 4) must be Screen 5", 4, OnboardingPagerAdapter.PAGE_SCREEN_5)
        assertEquals("Sixth onboarding page (index 5) must be Screen 6", 5, OnboardingPagerAdapter.PAGE_SCREEN_6)
    }
}
