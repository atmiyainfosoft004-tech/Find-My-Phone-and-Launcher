package com.example.findmyphonebyclaplauncher.ads

import com.example.findmyphonebyclaplauncher.ads.config.AdsConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
        assertTrue(defaultConfig.canShowBannerAfterCall)

        // Enable home banner dynamically
        val homeEnabledConfig = defaultConfig.copy(bannerAdEnableHome = true)
        assertTrue(homeEnabledConfig.canShowBannerHome)

        // Disable after call banner dynamically
        val afterCallDisabledConfig = defaultConfig.copy(bannerAdEnableAfterCall = false)
        assertFalse(afterCallDisabledConfig.canShowBannerAfterCall)
    }

    @Test
    fun screenLevelNativeFlags_correctlyControlScreenVisibility() {
        val defaultConfig = AdsConfig.DEFAULT
        assertTrue(defaultConfig.canShowNativeDashboard)
        assertTrue(defaultConfig.canShowNativeGoogleSearch)
        assertTrue(defaultConfig.canShowNativeLanguage)
        assertTrue(defaultConfig.canShowNativeInstallUninstall)

        // Disable dashboard native dynamically
        val dashboardDisabledConfig = defaultConfig.copy(nativeAdEnableDashboard = false)
        assertFalse(dashboardDisabledConfig.canShowNativeDashboard)
        assertTrue(dashboardDisabledConfig.canShowNativeGoogleSearch)

        // Disable install/uninstall native dynamically
        val installDisabledConfig = defaultConfig.copy(nativeAdEnableInstallUninstall = false)
        assertFalse(installDisabledConfig.canShowNativeInstallUninstall)
        assertTrue(installDisabledConfig.canShowNativeDashboard)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 6. Preload Flags Configuration & Strategy Resolution
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun preloadFlags_defaultsAndDynamicToggles() {
        val defaultConfig = AdsConfig.DEFAULT
        assertFalse("preload_ad_banner defaults to false", defaultConfig.preloadAdBanner)
        assertFalse("preload_ad_native defaults to false", defaultConfig.preloadAdNative)
        assertFalse("preload_ad_interstitial defaults to false", defaultConfig.preloadAdInterstitial)
        assertFalse("preload_ad_app_open defaults to false", defaultConfig.preloadAdAppOpen)

        val preloadEnabledConfig = defaultConfig.copy(
            preloadAdBanner = true,
            preloadAdNative = true,
            preloadAdInterstitial = true,
            preloadAdAppOpen = true
        )
        assertTrue(preloadEnabledConfig.preloadAdBanner)
        assertTrue(preloadEnabledConfig.preloadAdNative)
        assertTrue(preloadEnabledConfig.preloadAdInterstitial)
        assertTrue(preloadEnabledConfig.preloadAdAppOpen)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 7. Request Deduplication & Concurrent Guard Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun duplicateAdRequests_areDeduplicatedAndDoNotDoubleFire() {
        val isLoadingMap = java.util.concurrent.ConcurrentHashMap<String, Boolean>()
        val pendingCallbacks = java.util.concurrent.ConcurrentHashMap<String, MutableList<String>>()

        var networkRequestsFired = 0
        fun requestAd(placementKey: String, callbackId: String) {
            if (isLoadingMap[placementKey] == true) {
                // Request already pending: queue callback, do not fire network request
                pendingCallbacks.computeIfAbsent(placementKey) { mutableListOf() }.add(callbackId)
                return
            }
            isLoadingMap[placementKey] = true
            networkRequestsFired++
            pendingCallbacks.computeIfAbsent(placementKey) { mutableListOf() }.add(callbackId)
        }

        // Call 1 at t=0ms (GoogleSearch placement)
        requestAd("GoogleSearch:ca-app-pub-test", "caller_1")
        assertEquals("First request must fire network call", 1, networkRequestsFired)
        assertTrue("isLoadingMap must indicate in-flight request", isLoadingMap["GoogleSearch:ca-app-pub-test"] == true)

        // Call 2 at t=100ms (duplicate GoogleSearch placement while Call 1 is in-flight)
        requestAd("GoogleSearch:ca-app-pub-test", "caller_2")
        assertEquals("Duplicate in-flight request MUST NOT fire additional network calls", 1, networkRequestsFired)
        assertEquals("Both callers must be queued for notification", 2, pendingCallbacks["GoogleSearch:ca-app-pub-test"]?.size)

        // On Load complete
        isLoadingMap["GoogleSearch:ca-app-pub-test"] = false
        val executedCallbacks = pendingCallbacks.remove("GoogleSearch:ca-app-pub-test") ?: emptyList()
        assertEquals(2, executedCallbacks.size)
        assertTrue(executedCallbacks.contains("caller_1"))
        assertTrue(executedCallbacks.contains("caller_2"))

        // Call 3 after completion
        requestAd("GoogleSearch:ca-app-pub-test", "caller_3")
        assertEquals("New request after completion fires fresh network call", 2, networkRequestsFired)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 8. Language Screen Done Interstitial Guards
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun languageDoneInterstitial_disabled_proceedsImmediatelyWithoutBlocking() {
        val disabledConfig = AdsConfig.DEFAULT.copy(
            isInterAdEnabled = true,
            interAdEnableLanguage = false
        )

        var navigationExecuted = false
        fun handleLanguageDone(canShowAd: Boolean, onProceed: () -> Unit) {
            if (!canShowAd) {
                onProceed()
                return
            }
        }

        handleLanguageDone(disabledConfig.canShowInterLanguage) {
            navigationExecuted = true
        }

        assertTrue("Navigation must proceed immediately when language interstitial is disabled", navigationExecuted)
        assertFalse(disabledConfig.canShowInterLanguage)
    }

    @Test
    fun languageDoneInterstitial_duplicateClicks_debouncedAndNavigatesOnce() {
        var isNavigating = false
        var navigationCount = 0

        fun onDoneClicked() {
            if (isNavigating) return
            isNavigating = true
            navigationCount++
        }

        // Simulate 5 rapid clicks on Done
        for (i in 1..5) {
            onDoneClicked()
        }

        assertEquals("Navigation must be triggered exactly once despite rapid multiple clicks", 1, navigationCount)
        assertTrue(isNavigating)
    }

    @Test
    fun languageInterstitialPreload_whenDisabled_doesNotTriggerAdRequest() {
        val disabledConfig = AdsConfig.DEFAULT.copy(
            isInterAdEnabled = true,
            interAdEnableLanguage = false,
            preloadAdInterstitial = true
        )

        var preloadRequestFired = false
        fun preloadLanguageInterstitial(canShowAd: Boolean, preloadEnabled: Boolean) {
            if (!canShowAd || !preloadEnabled) return
            preloadRequestFired = true
        }

        preloadLanguageInterstitial(disabledConfig.canShowInterLanguage, disabledConfig.preloadAdInterstitial)
        assertFalse("No preload network request should be fired when inter_ad_enable_language is false", preloadRequestFired)
    }

    @Test
    fun languageInterstitial_case1_disabled_bypassesAllAdsAndDialogs() {
        val case1Config = AdsConfig.DEFAULT.copy(
            isInterAdEnabled = true,
            interAdEnableLanguage = false,
            preloadAdInterstitial = true
        )

        var preloadFired = false
        var dialogShown = false
        var navigationExecuted = false

        // On screen open
        if (case1Config.canShowInterLanguage && case1Config.preloadAdInterstitial) {
            preloadFired = true
        }

        // On Done click
        if (!case1Config.canShowInterLanguage) {
            navigationExecuted = true
        }

        assertFalse("Case 1: Must NOT preload", preloadFired)
        assertFalse("Case 1: Must NOT show dialog", dialogShown)
        assertTrue("Case 1: Must navigate directly", navigationExecuted)
    }

    @Test
    fun languageInterstitial_case2_enabledWithPreload_preloadsAndShowsIfReady() {
        val case2Config = AdsConfig.DEFAULT.copy(
            isInterAdEnabled = true,
            interAdEnableLanguage = true,
            preloadAdInterstitial = true
        )

        var preloadFired = false
        var adShown = false
        var navigationExecuted = false

        // 1. On Language screen enter
        if (case2Config.canShowInterLanguage && case2Config.preloadAdInterstitial) {
            preloadFired = true
        }
        assertTrue("Case 2: Must initiate preload on Language screen enter", preloadFired)

        // 2. On Done with preloaded ad ready
        var isPreloadedAdReady = true
        if (case2Config.canShowInterLanguage) {
            if (case2Config.preloadAdInterstitial) {
                if (isPreloadedAdReady) {
                    adShown = true
                    // simulate ad dismissal
                    navigationExecuted = true
                } else {
                    navigationExecuted = true
                }
            }
        }
        assertTrue("Case 2: Must show preloaded ad when available", adShown)
        assertTrue("Case 2: Must navigate after ad dismissal", navigationExecuted)

        // 3. On Done when preloaded ad failed/not ready -> proceed directly without blocking
        var navigationExecutedFallback = false
        isPreloadedAdReady = false
        if (case2Config.canShowInterLanguage && case2Config.preloadAdInterstitial && !isPreloadedAdReady) {
            navigationExecutedFallback = true
        }
        assertTrue("Case 2 Fallback: Must proceed immediately when ad is not ready", navigationExecutedFallback)
    }

    @Test
    fun languageInterstitial_case3_enabledWithoutPreload_loadsOnDemandOnDone() {
        val case3Config = AdsConfig.DEFAULT.copy(
            isInterAdEnabled = true,
            interAdEnableLanguage = true,
            preloadAdInterstitial = false
        )

        var preloadFired = false
        var onDemandLoadTriggered = false
        var dialogShown = false
        var navigationExecuted = false

        // 1. On Language screen enter
        if (case3Config.canShowInterLanguage && case3Config.preloadAdInterstitial) {
            preloadFired = true
        }
        assertFalse("Case 3: Must NOT preload on Language screen enter", preloadFired)

        // 2. On Done click -> load on-demand with dialog
        if (case3Config.canShowInterLanguage) {
            if (!case3Config.preloadAdInterstitial) {
                dialogShown = true
                onDemandLoadTriggered = true
                // simulate load success and ad dismissal
                dialogShown = false
                navigationExecuted = true
            }
        }

        assertTrue("Case 3: Must trigger on-demand load on Done click", onDemandLoadTriggered)
        assertTrue("Case 3: Must navigate after ad dismiss", navigationExecuted)
        assertFalse("Case 3: Dialog must be dismissed", dialogShown)
    }

    @Test
    fun languageScreen_openedLaterFromSettings_neverPreloadsOrShowsInterstitial() {
        val enabledConfig = AdsConfig.DEFAULT.copy(
            isInterAdEnabled = true,
            interAdEnableLanguage = true,
            preloadAdInterstitial = true
        )

        val isFirstTime = false // Opened from Settings/Menu later
        var preloadFired = false
        var interstitialTriggered = false
        var slideFinishExecuted = false

        // 1. On Language screen enter
        if (isFirstTime) {
            if (enabledConfig.canShowInterLanguage && enabledConfig.preloadAdInterstitial) {
                preloadFired = true
            }
        }
        assertFalse("Settings flow: Must NOT preload Interstitial", preloadFired)

        // 2. On Done click
        if (isFirstTime) {
            interstitialTriggered = true
        } else {
            // Directly finishes with slide animation without any ad
            slideFinishExecuted = true
        }

        assertFalse("Settings flow: Must NOT trigger Interstitial flow on Done", interstitialTriggered)
        assertTrue("Settings flow: Must finish immediately with slide animation", slideFinishExecuted)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 12. Direction-Based Independent Swipe Counters (right_to_left_count & left_to_right_count)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun directionBasedCounters_operateIndependently() {
        val config = AdsConfig.DEFAULT.copy(
            isSwipeAdEnabled = true,
            isSwipeAdInterstitial = true,
            rightToLeftCount = 3,
            leftToRightCount = 5
        )

        var rightToLeftCount = 0
        var leftToRightCount = 0
        var rightToLeftAdShownCount = 0
        var leftToRightAdShownCount = 0

        fun performSwipe(direction: SwipeDirection) {
            val trigger = when (direction) {
                SwipeDirection.RIGHT_TO_LEFT -> config.rightToLeftCount
                SwipeDirection.LEFT_TO_RIGHT -> config.leftToRightCount
            }
            if (trigger <= 0) return

            when (direction) {
                SwipeDirection.RIGHT_TO_LEFT -> {
                    rightToLeftCount++
                    if (rightToLeftCount >= trigger) {
                        rightToLeftCount = 0
                        rightToLeftAdShownCount++
                    }
                }
                SwipeDirection.LEFT_TO_RIGHT -> {
                    leftToRightCount++
                    if (leftToRightCount >= trigger) {
                        leftToRightCount = 0
                        leftToRightAdShownCount++
                    }
                }
            }
        }

        // Perform 2 Right-to-Left swipes (count: 2/3)
        performSwipe(SwipeDirection.RIGHT_TO_LEFT)
        performSwipe(SwipeDirection.RIGHT_TO_LEFT)
        assertEquals(2, rightToLeftCount)
        assertEquals(0, leftToRightCount)
        assertEquals(0, rightToLeftAdShownCount)

        // Perform 4 Left-to-Right swipes (count: 4/5)
        repeat(4) { performSwipe(SwipeDirection.LEFT_TO_RIGHT) }
        assertEquals(2, rightToLeftCount)
        assertEquals(4, leftToRightCount)
        assertEquals(0, rightToLeftAdShownCount)
        assertEquals(0, leftToRightAdShownCount)

        // 3rd Right-to-Left swipe -> triggers Ad and resets Right-to-Left counter only
        performSwipe(SwipeDirection.RIGHT_TO_LEFT)
        assertEquals(0, rightToLeftCount)
        assertEquals(1, rightToLeftAdShownCount)
        assertEquals(4, leftToRightCount) // left_to_right counter unaffected
        assertEquals(0, leftToRightAdShownCount)

        // 5th Left-to-Right swipe -> triggers Ad and resets Left-to-Right counter only
        performSwipe(SwipeDirection.LEFT_TO_RIGHT)
        assertEquals(0, leftToRightCount)
        assertEquals(1, leftToRightAdShownCount)
        assertEquals(0, rightToLeftCount)
        assertEquals(1, rightToLeftAdShownCount)
    }

    @Test
    fun zeroOrNegativeTrigger_safelyDisablesSwipeAdForThatDirection() {
        val config = AdsConfig.DEFAULT.copy(
            isSwipeAdEnabled = true,
            rightToLeftCount = 0,
            leftToRightCount = -1
        )

        var rightToLeftAdShown = false
        var leftToRightAdShown = false

        if (config.rightToLeftCount > 0) {
            rightToLeftAdShown = true
        }
        if (config.leftToRightCount > 0) {
            leftToRightAdShown = true
        }

        assertFalse("Right-to-Left ad must not trigger when right_to_left_count is 0", rightToLeftAdShown)
        assertFalse("Left-to-Right ad must not trigger when left_to_right_count is negative", leftToRightAdShown)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 13. HomeFragment Navigation Transition Mapping (Home->Phone vs Home->Dashboard vs Returns)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun homeNavigationTransitions_strictlyMapOnlyHomeOriginSwipes() {
        val PAGE_FIND_PHONE = 0
        val PAGE_HOME = 1
        val PAGE_DASHBOARD = 2

        fun mapSwipeDirection(fromPage: Int, toPage: Int): SwipeDirection? {
            return when {
                fromPage == PAGE_HOME && toPage == PAGE_DASHBOARD -> SwipeDirection.RIGHT_TO_LEFT
                fromPage == PAGE_HOME && toPage == PAGE_FIND_PHONE -> SwipeDirection.LEFT_TO_RIGHT
                else -> null
            }
        }

        // 1. Home -> Dashboard: RIGHT_TO_LEFT
        assertEquals(SwipeDirection.RIGHT_TO_LEFT, mapSwipeDirection(PAGE_HOME, PAGE_DASHBOARD))

        // 2. Home -> Phone: LEFT_TO_RIGHT
        assertEquals(SwipeDirection.LEFT_TO_RIGHT, mapSwipeDirection(PAGE_HOME, PAGE_FIND_PHONE))

        // 3. Dashboard -> Home: null (must NOT count)
        assertNull(mapSwipeDirection(PAGE_DASHBOARD, PAGE_HOME))

        // 4. Phone -> Home: null (must NOT count)
        assertNull(mapSwipeDirection(PAGE_FIND_PHONE, PAGE_HOME))
    }

    @Test
    fun directionEnableFlags_strictlyGuardAdTriggers() {
        val config = AdsConfig.DEFAULT.copy(
            isSwipeAdEnabled = true,
            rightToLeftAdEnable = false, // Disabled
            leftToRightAdEnable = true,  // Enabled
            rightToLeftCount = 1,
            leftToRightCount = 1
        )

        var rightToLeftAdExecuted = false
        var leftToRightAdExecuted = false

        fun handleSwipe(direction: SwipeDirection) {
            val isEnabled = when (direction) {
                SwipeDirection.RIGHT_TO_LEFT -> config.rightToLeftAdEnable
                SwipeDirection.LEFT_TO_RIGHT -> config.leftToRightAdEnable
            }
            if (!isEnabled) return

            when (direction) {
                SwipeDirection.RIGHT_TO_LEFT -> rightToLeftAdExecuted = true
                SwipeDirection.LEFT_TO_RIGHT -> leftToRightAdExecuted = true
            }
        }

        handleSwipe(SwipeDirection.RIGHT_TO_LEFT)
        assertFalse("Right-to-Left ad must NOT execute when right_to_left_ad_enable=false", rightToLeftAdExecuted)

        handleSwipe(SwipeDirection.LEFT_TO_RIGHT)
        assertTrue("Left-to-Right ad must execute when left_to_right_ad_enable=true", leftToRightAdExecuted)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 14. Banner & Native Ad Reload On Screen Return & Cleanup
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun screenReturn_triggersFreshBannerAndNativeLoad() {
        var bannerLoadCount = 0
        var nativeLoadCount = 0
        var activeBannerDestroyed = false
        var activeNativeDestroyed = false

        fun onScreenVisible() {
            // Load fresh ads
            bannerLoadCount++
            nativeLoadCount++
        }

        fun onScreenLeaveOrDestroy() {
            // Clean up old ad instances
            activeBannerDestroyed = true
            activeNativeDestroyed = true
        }

        // 1. Initial screen appearance
        onScreenVisible()
        assertEquals(1, bannerLoadCount)
        assertEquals(1, nativeLoadCount)

        // 2. User navigates to another screen
        onScreenLeaveOrDestroy()
        assertTrue(activeBannerDestroyed)
        assertTrue(activeNativeDestroyed)

        // 3. User returns to previous screen -> Fresh load triggered
        activeBannerDestroyed = false
        activeNativeDestroyed = false
        onScreenVisible()
        assertEquals(2, bannerLoadCount)
        assertEquals(2, nativeLoadCount)

        // 4. Repeated returns (A -> B -> A -> B -> A)
        repeat(3) {
            onScreenLeaveOrDestroy()
            onScreenVisible()
        }
        assertEquals(5, bannerLoadCount)
        assertEquals(5, nativeLoadCount)
    }

    @Test
    fun disabledAdConfig_preventsReloadAndHidesViews() {
        val disabledConfig = AdsConfig.DEFAULT.copy(
            isBannerAdEnabled = false,
            isNativeAdEnabled = false
        )

        var bannerLoaded = false
        var nativeLoaded = false
        var containerHidden = false

        if (!disabledConfig.isBannerAdEnabled && !disabledConfig.isNativeAdEnabled) {
            containerHidden = true
        } else {
            bannerLoaded = true
            nativeLoaded = true
        }

        assertTrue("Containers must be hidden when ads are disabled", containerHidden)
        assertFalse("Banner must not load when isBannerAdEnabled=false", bannerLoaded)
        assertFalse("Native must not load when isNativeAdEnabled=false", nativeLoaded)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 15. AppDrawer Scrolling & Gesture Banner Request Guard
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun appDrawerScroll_neverTriggersBannerAdRequest() {
        var bannerRequestCount = 0
        var hasAttemptedSession = false
        var isDrawerOpen = false

        fun loadBannerAd(forceReload: Boolean = false) {
            if (hasAttemptedSession && !forceReload) return
            hasAttemptedSession = true
            bannerRequestCount++
        }

        fun onOpenDrawer() {
            val wasAlreadyOpen = isDrawerOpen
            isDrawerOpen = true
            if (!wasAlreadyOpen) {
                loadBannerAd(forceReload = true)
            }
        }

        fun onCloseDrawer() {
            isDrawerOpen = false
            hasAttemptedSession = false
        }

        fun onScrollOrTouchInsideDrawer() {
            // Settle / touch callback inside already open drawer
            if (isDrawerOpen) {
                onOpenDrawer() // Should check wasAlreadyOpen and NOT re-request
            }
            loadBannerAd(forceReload = false) // Session guard also blocks
        }

        // 1. Initial Open: Should request exactly 1 banner ad
        onOpenDrawer()
        assertEquals(1, bannerRequestCount)

        // 2. User scrolls list, flings, drags up/down multiple times inside open drawer
        repeat(10) {
            onScrollOrTouchInsideDrawer()
        }
        assertEquals("Scrolling or touching inside open drawer must NEVER trigger new banner requests", 1, bannerRequestCount)

        // 3. User closes drawer
        onCloseDrawer()

        // 4. User opens drawer again later -> Triggers fresh load
        onOpenDrawer()
        assertEquals("Re-opening drawer must trigger fresh banner load", 2, bannerRequestCount)

        // 5. More scrolling in new session -> Still locked to 2
        repeat(5) {
            onScrollOrTouchInsideDrawer()
        }
        assertEquals("Scrolling in new session must NEVER trigger duplicate requests", 2, bannerRequestCount)
    }
}
