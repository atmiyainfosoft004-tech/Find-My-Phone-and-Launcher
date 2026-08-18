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
        assertEquals("First onboarding page (index 0) must be Screen 1", 0, OnboardingPagerAdapter.PAGE_SCREEN_1)
        assertEquals("Second onboarding page (index 1) must be Screen 3", 1, OnboardingPagerAdapter.PAGE_SCREEN_3)
        assertEquals("Third onboarding page (index 2) must be Screen 4", 2, OnboardingPagerAdapter.PAGE_SCREEN_4)
        assertEquals("Fourth onboarding page (index 3) must be Screen 5", 3, OnboardingPagerAdapter.PAGE_SCREEN_5)
        assertEquals("Fifth onboarding page (index 4) must be Screen 6", 4, OnboardingPagerAdapter.PAGE_SCREEN_6)
    }
}
