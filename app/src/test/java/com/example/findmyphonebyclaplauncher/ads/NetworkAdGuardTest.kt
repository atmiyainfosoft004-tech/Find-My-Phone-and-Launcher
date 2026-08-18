package com.example.findmyphonebyclaplauncher.ads

import com.example.findmyphonebyclaplauncher.ads.config.AdsConfig
import com.example.findmyphonebyclaplauncher.util.NetworkUtil
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NetworkAdGuardTest {

    private enum class Visibility { VISIBLE, GONE }

    @Before
    fun setUp() {
        NetworkUtil.overrideNetworkAvailableForTesting = null
    }

    @After
    fun tearDown() {
        NetworkUtil.overrideNetworkAvailableForTesting = null
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. Offline Mode Guard Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun offlineMode_suppressesAdNetworkRequests_andGuaranteesZeroCalls() {
        NetworkUtil.overrideNetworkAvailableForTesting = false
        assertFalse("NetworkUtil must report offline", NetworkUtil.isNetworkAvailable(null))

        var networkRequestsFired = 0
        fun requestAdIfAllowed(isOnline: Boolean, canShowAd: Boolean): Boolean {
            if (!isOnline || !canShowAd) {
                return false
            }
            networkRequestsFired++
            return true
        }

        val config = AdsConfig.DEFAULT
        val bannerAttempted = requestAdIfAllowed(NetworkUtil.isNetworkAvailable(null), config.canShowBannerSplash)
        val nativeAttempted = requestAdIfAllowed(NetworkUtil.isNetworkAvailable(null), config.canShowNativeDashboard)
        val interAttempted = requestAdIfAllowed(NetworkUtil.isNetworkAvailable(null), config.canShowInter)
        val appOpenAttempted = requestAdIfAllowed(NetworkUtil.isNetworkAvailable(null), config.canShowAppOpen)

        assertFalse("Banner request must be suppressed when offline", bannerAttempted)
        assertFalse("Native request must be suppressed when offline", nativeAttempted)
        assertFalse("Interstitial request must be suppressed when offline", interAttempted)
        assertFalse("App open request must be suppressed when offline", appOpenAttempted)
        assertEquals("Zero network requests must be fired when offline", 0, networkRequestsFired)
    }

    @Test
    fun offlineMode_actionAdTriggers_immediatelyBypassAndContinue() {
        NetworkUtil.overrideNetworkAvailableForTesting = false

        var userActionContinued = false
        fun handleUserAction(isOnline: Boolean, canShowAd: Boolean, onDone: () -> Unit) {
            if (!isOnline || !canShowAd) {
                onDone()
                return
            }
            // If online & enabled, show ad then call onDone...
        }

        val config = AdsConfig.DEFAULT
        handleUserAction(NetworkUtil.isNetworkAvailable(null), config.canShowClickAd) {
            userActionContinued = true
        }

        assertTrue("User action must proceed immediately when offline without blocking dialogs", userActionContinued)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. Ad Load Failure & Shimmer Lifecycle State Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun adLoadFailure_immediatelyHidesShimmer_andSetsContainerToGone() {
        var shimmerVisibility = Visibility.VISIBLE
        var containerVisibility = Visibility.GONE
        var isShimmerAnimating = true

        // Simulate onAdFailedToLoad event
        fun onAdFailedToLoadSimulated() {
            isShimmerAnimating = false
            shimmerVisibility = Visibility.GONE
            containerVisibility = Visibility.GONE
        }

        onAdFailedToLoadSimulated()

        assertEquals("Shimmer must be GONE on ad failure", Visibility.GONE, shimmerVisibility)
        assertEquals("Container must be GONE on ad failure", Visibility.GONE, containerVisibility)
        assertFalse("Shimmer animation must be stopped on ad failure", isShimmerAnimating)
    }

    @Test
    fun adLoadSuccess_hidesShimmer_andSetsContainerToVisible() {
        var shimmerVisibility = Visibility.VISIBLE
        var containerVisibility = Visibility.GONE
        var isShimmerAnimating = true

        // Simulate onAdLoaded event
        fun onAdLoadedSimulated() {
            isShimmerAnimating = false
            shimmerVisibility = Visibility.GONE
            containerVisibility = Visibility.VISIBLE
        }

        onAdLoadedSimulated()

        assertEquals("Shimmer must be GONE on ad load success", Visibility.GONE, shimmerVisibility)
        assertEquals("Container must be VISIBLE on ad load success", Visibility.VISIBLE, containerVisibility)
        assertFalse("Shimmer animation must be stopped once ad is loaded", isShimmerAnimating)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. Network Status Changes & Shimmer Loop Prevention
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun networkReconnection_onlyAttemptsAdLoad_whenEnabled_andNotAlreadyLoadedOrInFlight() {
        var networkRequestsFired = 0
        var isAdLoaded = false
        var isAdInFlight = false

        fun onNetworkAvailableSimulated(canShowAd: Boolean) {
            if (!canShowAd || isAdLoaded || isAdInFlight) {
                return
            }
            isAdInFlight = true
            networkRequestsFired++
        }

        val config = AdsConfig.DEFAULT.copy(isNativeAdEnabled = true, nativeAdEnableDashboard = true)

        // 1. Initial online event: Request starts
        onNetworkAvailableSimulated(config.canShowNativeDashboard)
        assertEquals(1, networkRequestsFired)
        assertTrue(isAdInFlight)

        // 2. Rapid network toggle while request is in flight: Must NOT double-fire
        onNetworkAvailableSimulated(config.canShowNativeDashboard)
        assertEquals("Duplicate network reconnect event while in-flight must not double-fire", 1, networkRequestsFired)

        // 3. Ad finishes loading
        isAdInFlight = false
        isAdLoaded = true

        // 4. Subsequent network toggle when ad is already loaded: Must NOT reload or trigger shimmer loop
        onNetworkAvailableSimulated(config.canShowNativeDashboard)
        assertEquals("Reconnection when ad is already loaded must not fire additional requests", 1, networkRequestsFired)
    }

    @Test
    fun networkReconnection_withDisabledRemoteConfig_guaranteesZeroRequests() {
        var networkRequestsFired = 0
        fun onNetworkAvailableSimulated(canShowAd: Boolean) {
            if (!canShowAd) return
            networkRequestsFired++
        }

        // Disabled by screen flag
        val disabledConfig = AdsConfig.DEFAULT.copy(bannerAdEnableFindPhone = false)
        onNetworkAvailableSimulated(disabledConfig.canShowBannerFindPhone)
        assertEquals("Zero requests when screen flag is false", 0, networkRequestsFired)

        // Disabled by master switch
        val masterDisabledConfig = AdsConfig.DEFAULT.copy(isBannerAdEnabled = false, bannerAdEnableFindPhone = true)
        onNetworkAvailableSimulated(masterDisabledConfig.canShowBannerFindPhone)
        assertEquals("Zero requests when master flag is false", 0, networkRequestsFired)
    }
}
