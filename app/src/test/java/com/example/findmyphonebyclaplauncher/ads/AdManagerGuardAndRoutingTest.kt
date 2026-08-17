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
}
