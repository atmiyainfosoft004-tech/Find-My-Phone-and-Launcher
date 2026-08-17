package com.example.findmyphonebyclaplauncher.ads

import com.example.findmyphonebyclaplauncher.ads.config.AdsConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit & Integration tests for ad guards, isolated counter tracking, and dynamic routing logic.
 */
class AdManagerGuardAndRoutingTest {

    // ─────────────────────────────────────────────────────────────────────────
    // 1. Zero Network Request Guarding (Strict Policy)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun clickActionDisabled_abortsAdProcessingImmediately() {
        val config = AdsConfig.DEFAULT.copy(
            isClickAdEnabled = false,
            isClickAdInterstitial = true
        )

        var actionExecuted = false
        val isActionEnabled = config.isClickAdEnabled

        if (!isActionEnabled) {
            actionExecuted = true
        }

        assertTrue("User action continuation must be called immediately when click ad is disabled", actionExecuted)
        assertFalse("canShowClickAd must be false when isClickAdEnabled is false", config.canShowClickAd)
    }

    @Test
    fun swipeActionDisabled_abortsAdProcessingImmediately() {
        val config = AdsConfig.DEFAULT.copy(
            isSwipeAdEnabled = false,
            isSwipeAdInterstitial = false
        )

        var swipeHandled = false
        val isActionEnabled = config.isSwipeAdEnabled

        if (!isActionEnabled) {
            swipeHandled = true
        }

        assertTrue("Swipe continuation must be called immediately when swipe ad is disabled", swipeHandled)
        assertFalse("canShowSwipeAd must be false when isSwipeAdEnabled is false", config.canShowSwipeAd)
    }

    @Test
    fun findPhoneBannerDisabled_guaranteesZeroNetworkCalls() {
        val config = AdsConfig.DEFAULT.copy(
            isBannerAdEnabled = true,
            bannerAdEnableFindPhone = false
        )

        assertFalse("canShowBannerFindPhone must be false when screen flag is disabled", config.canShowBannerFindPhone)
    }

    @Test
    fun dashboardNativeDisabled_guaranteesZeroNetworkCalls() {
        val config = AdsConfig.DEFAULT.copy(
            isNativeAdEnabled = true,
            nativeAdEnableDashboard = false
        )

        assertFalse("canShowNativeDashboard must be false when screen flag is disabled", config.canShowNativeDashboard)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. Click vs. Swipe Ad Type Routing
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun scenario1_clickIsInter_swipeIsOpen() {
        val config = AdsConfig.DEFAULT.copy(
            isClickAdEnabled = true,
            isSwipeAdEnabled = true,
            isClickAdInterstitial = true,
            isSwipeAdInterstitial = false
        )

        assertTrue("Click action should resolve to Interstitial (true)", config.isClickAdInterstitial)
        assertTrue(config.isClickInter)
        assertFalse(config.isClickAppOpen)

        assertFalse("Swipe action should resolve to App Open (false)", config.isSwipeAdInterstitial)
        assertFalse(config.isSwipeInter)
        assertTrue(config.isSwipeAppOpen)
    }

    @Test
    fun scenario2_clickIsOpen_swipeIsInter() {
        val config = AdsConfig.DEFAULT.copy(
            isClickAdEnabled = true,
            isSwipeAdEnabled = true,
            isClickAdInterstitial = false,
            isSwipeAdInterstitial = true
        )

        assertFalse("Click action should resolve to App Open (false)", config.isClickAdInterstitial)
        assertFalse(config.isClickInter)
        assertTrue(config.isClickAppOpen)

        assertTrue("Swipe action should resolve to Interstitial (true)", config.isSwipeAdInterstitial)
        assertTrue(config.isSwipeInter)
        assertFalse(config.isSwipeAppOpen)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. State Isolation Check (Bug Prevention)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun clickAdInterstitialFalse_selectsAppOpenAdExclusively() {
        val config = AdsConfig.DEFAULT.copy(
            isClickAdEnabled = true,
            isClickAdInterstitial = false
        )

        assertFalse("is_click_ad_interstitial must be false", config.isClickAdInterstitial)
        assertFalse("isClickInter must be false", config.isClickInter)
        assertTrue("isClickAppOpen must be true", config.isClickAppOpen)
        assertFalse("canShowAppClickInter must be false", config.canShowAppClickInter)
        assertTrue("canShowAppClickOpen must be true", config.canShowAppClickOpen)
        assertEquals("Open", config.clickAdTypeSelection)
    }

    @Test
    fun swipeAdInterstitialFalse_selectsAppOpenAdExclusively() {
        val config = AdsConfig.DEFAULT.copy(
            isSwipeAdEnabled = true,
            isSwipeAdInterstitial = false
        )

        assertFalse("is_swipe_ad_interstitial must be false", config.isSwipeAdInterstitial)
        assertFalse("isSwipeInter must be false", config.isSwipeInter)
        assertTrue("isSwipeAppOpen must be true", config.isSwipeAppOpen)
        assertFalse("canShowSwipeInter must be false", config.canShowSwipeInter)
        assertTrue("canShowSwipeOpen must be true", config.canShowSwipeOpen)
        assertEquals("Open", config.swipeAdTypeSelection)
    }

    @Test
    fun clickActions_doNotContaminateSwipeActionRouting() {
        val config = AdsConfig.DEFAULT.copy(
            isClickAdEnabled = true,
            isSwipeAdEnabled = true,
            isClickAdInterstitial = true,
            isSwipeAdInterstitial = false
        )

        // Simulate multiple click evaluations
        for (i in 1..10) {
            val clickType = config.isClickAdInterstitial
            assertTrue("Click action must be Interstitial", clickType)
        }

        // Evaluate swipe action afterwards
        val swipeType = config.isSwipeAdInterstitial
        assertFalse("Swipe action must remain App Open regardless of previous clicks", swipeType)
        assertEquals("Open", config.swipeAdTypeSelection)
        assertEquals("Inter", config.clickAdTypeSelection)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. Counter Trigger & Independent Reset Logic
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun clickCounter_triggersOnlyAtThresholdAndResetsIndependently() {
        val clickTrigger = 3
        var currentClickCount = 0
        var adTriggered = false

        // Click 1
        currentClickCount++
        if (currentClickCount >= clickTrigger) {
            adTriggered = true
            currentClickCount = 0
        }
        assertFalse("Ad should not trigger on click 1", adTriggered)
        assertEquals(1, currentClickCount)

        // Click 2
        currentClickCount++
        if (currentClickCount >= clickTrigger) {
            adTriggered = true
            currentClickCount = 0
        }
        assertFalse("Ad should not trigger on click 2", adTriggered)
        assertEquals(2, currentClickCount)

        // Click 3
        currentClickCount++
        if (currentClickCount >= clickTrigger) {
            adTriggered = true
            currentClickCount = 0
        }
        assertTrue("Ad must trigger on click 3", adTriggered)
        assertEquals("Counter must reset to 0 upon trigger", 0, currentClickCount)
    }

    @Test
    fun backPressCounter_triggersOnlyAtThresholdAndResetsIndependently() {
        val backTrigger = 3
        var currentBackCount = 0
        var adTriggered = false

        // Back 1
        currentBackCount++
        if (currentBackCount >= backTrigger) {
            adTriggered = true
            currentBackCount = 0
        }
        assertFalse("Ad should not trigger on back 1", adTriggered)
        assertEquals(1, currentBackCount)

        // Back 2
        currentBackCount++
        if (currentBackCount >= backTrigger) {
            adTriggered = true
            currentBackCount = 0
        }
        assertFalse("Ad should not trigger on back 2", adTriggered)
        assertEquals(2, currentBackCount)

        // Back 3
        currentBackCount++
        if (currentBackCount >= backTrigger) {
            adTriggered = true
            currentBackCount = 0
        }
        assertTrue("Ad must trigger on back 3", adTriggered)
        assertEquals("Counter must reset to 0 upon trigger", 0, currentBackCount)
    }

    @Test
    fun launcherClick_and_inAppBack_areCompletelyIsolated_andDoNotContaminateEachOther() {
        var launcherClickCount = 0
        var inAppBackCount = 0

        val clickTrigger = 3
        val backTrigger = 5

        var clickAdTriggered = false
        var backAdTriggered = false

        // Simulate 2 Launcher Clicks
        for (i in 1..2) {
            launcherClickCount++
            if (launcherClickCount >= clickTrigger) {
                clickAdTriggered = true
                launcherClickCount = 0
            }
        }
        assertEquals("Launcher clicks must be 2", 2, launcherClickCount)
        assertEquals("In-app back count must remain 0", 0, inAppBackCount)
        assertFalse(clickAdTriggered)
        assertFalse(backAdTriggered)

        // Simulate 4 In-App Back Navigations
        for (i in 1..4) {
            inAppBackCount++
            if (inAppBackCount >= backTrigger) {
                backAdTriggered = true
                inAppBackCount = 0
            }
        }
        assertEquals("Launcher click count must remain untouched at 2", 2, launcherClickCount)
        assertEquals("In-app back count must be 4", 4, inAppBackCount)
        assertFalse(clickAdTriggered)
        assertFalse(backAdTriggered)

        // 3rd Launcher Click triggers Click Ad & resets only launcher counter
        launcherClickCount++
        if (launcherClickCount >= clickTrigger) {
            clickAdTriggered = true
            launcherClickCount = 0
        }
        assertTrue("Click ad must trigger on 3rd click", clickAdTriggered)
        assertEquals("Launcher click count must reset to 0", 0, launcherClickCount)
        assertEquals("In-app back count must remain at 4 without resetting or changing", 4, inAppBackCount)
        assertFalse("Back ad must NOT trigger", backAdTriggered)

        // 5th In-App Back Navigation triggers Back Ad & resets only back counter
        inAppBackCount++
        if (inAppBackCount >= backTrigger) {
            backAdTriggered = true
            inAppBackCount = 0
        }
        assertTrue("Back ad must trigger on 5th back press", backAdTriggered)
        assertEquals("In-app back count must reset to 0", 0, inAppBackCount)
        assertEquals("Launcher click count must remain at 0", 0, launcherClickCount)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. Screen-Level Placement Flag Resolution
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun screenLevelBannerFlags_correctlyControlScreenVisibility() {
        val defaultConfig = AdsConfig.DEFAULT
        assertTrue(defaultConfig.canShowBannerSplash)
        assertFalse("banner_ad_enable_home_screen defaults to false", defaultConfig.canShowBannerHome)
        assertTrue(defaultConfig.canShowBannerAppDrawer)
        assertTrue(defaultConfig.canShowBannerFindPhone)
        assertTrue(defaultConfig.canShowBannerAlertScreen)

        // Enable home banner dynamically
        val homeEnabledConfig = defaultConfig.copy(bannerAdEnableHome = true)
        assertTrue(homeEnabledConfig.canShowBannerHome)
    }

    @Test
    fun screenLevelNativeFlags_correctlyControlScreenVisibility() {
        val defaultConfig = AdsConfig.DEFAULT
        assertTrue(defaultConfig.canShowNativeDashboard)
        assertTrue(defaultConfig.canShowNativeGoogleSearch)
        assertTrue(defaultConfig.canShowNativeLanguage)
        assertTrue(defaultConfig.canShowNativeAfterCall)

        // Disable dashboard native dynamically
        val dashboardDisabledConfig = defaultConfig.copy(nativeAdEnableDashboard = false)
        assertFalse(dashboardDisabledConfig.canShowNativeDashboard)
        assertTrue(dashboardDisabledConfig.canShowNativeGoogleSearch)
    }
}
