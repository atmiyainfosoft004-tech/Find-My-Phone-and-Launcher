package com.example.findmyphonebyclaplauncher.ads

import com.example.findmyphonebyclaplauncher.ads.config.AdsConfig
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests validating Remote Config defaults, key parsing, aliases, and edge case fallbacks.
 */
class RemoteConfigManagerTest {

    private val gson = Gson()

    // ─────────────────────────────────────────────────────────────────────────
    // 1. Default Values Verification
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun defaultValues_areCorrectlyInitialized() {
        val defaultConfig = AdsConfig.DEFAULT

        // System UI
        assertTrue("system_hide_navigation_bar_auto should default to true", defaultConfig.systemHideNavigationBarAuto)

        // Master toggles
        assertTrue("is_banner_ad_enabled should default to true", defaultConfig.isBannerAdEnabled)
        assertTrue("is_native_ad_enabled should default to true", defaultConfig.isNativeAdEnabled)
        assertTrue("is_inter_ad_enabled should default to true", defaultConfig.isInterAdEnabled)
        assertTrue("is_app_open_ad_enabled should default to true", defaultConfig.isAppOpenAdEnabled)

        // Action-level switches
        assertFalse("is_click_ad_enabled should default to false", defaultConfig.isClickAdEnabled)
        assertFalse("is_swipe_ad_enabled should default to false", defaultConfig.isSwipeAdEnabled)
        assertTrue("is_back_ad_enabled should default to true", defaultConfig.isBackAdEnabled)

        // Format selections
        assertTrue("is_click_ad_interstitial should default to true", defaultConfig.isClickAdInterstitial)
        assertFalse("is_swipe_ad_interstitial should default to false", defaultConfig.isSwipeAdInterstitial)

        // Counter triggers
        assertEquals(3, defaultConfig.interAdCounterTrigger)
        assertEquals(1, defaultConfig.interAdBackCounterTrigger)
        assertEquals(3, defaultConfig.clickAdCounterTrigger)

        // Banner screen toggles
        assertTrue(defaultConfig.bannerAdEnableSplash)
        assertTrue(defaultConfig.bannerAdEnableHome)
        assertTrue(defaultConfig.bannerAdEnableAppDrawer)
        assertTrue(defaultConfig.bannerAdEnableFindPhone)
        assertTrue(defaultConfig.bannerAdEnableAlertScreen)

        // Native screen toggles & interval
        assertTrue(defaultConfig.nativeAdEnableDashboard)
        assertTrue(defaultConfig.nativeAdEnableGoogleSearch)
        assertTrue(defaultConfig.nativeAdEnableLanguage)
        assertTrue(defaultConfig.nativeAdEnableAfterCall)
        assertEquals(2, defaultConfig.nativeAdGoogleSearchItemInterval)

        // Unit IDs
        assertTrue(defaultConfig.interAdId.isNotBlank())
        assertTrue(defaultConfig.appOpenAdId.isNotBlank())
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. JSON Deserialization & Alias Parsing
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun jsonParsing_handlesAllUpdatedBooleanSchemaKeys() {
        val json = """
            {
                "system_hide_navigation_bar_auto": true,
                "is_banner_ad_enabled": true,
                "is_native_ad_enabled": false,
                "is_inter_ad_enabled": true,
                "is_app_open_ad_enabled": false,
                "is_click_ad_enabled": true,
                "is_swipe_ad_enabled": true,
                "is_back_ad_enabled": false,
                "is_click_ad_interstitial": false,
                "is_swipe_ad_interstitial": true,
                "inter_ad_counter_trigger": 5,
                "inter_ad_back_counter_trigger": 2,
                "click_ad_counter_trigger": 4,
                "banner_ad_enable_splash": true,
                "banner_ad_enable_home_screen": false,
                "banner_ad_enable_app_drawer": true,
                "banner_ad_enable_find_phone": false,
                "banner_ad_enable_alert_screen": true,
                "native_ad_enable_dashboard": false,
                "native_ad_enable_google_search": true,
                "native_ad_google_search_item_interval": 3
            }
        """.trimIndent()

        val parsed = gson.fromJson(json, AdsConfig::class.java)

        assertTrue(parsed.systemHideNavigationBarAuto)
        assertTrue(parsed.isBannerAdEnabled)
        assertFalse(parsed.isNativeAdEnabled)
        assertTrue(parsed.isInterAdEnabled)
        assertFalse(parsed.isAppOpenAdEnabled)

        assertTrue(parsed.isClickAdEnabled)
        assertTrue(parsed.isSwipeAdEnabled)
        assertFalse(parsed.isBackAdEnabled)

        assertFalse(parsed.isClickAdInterstitial)
        assertTrue(parsed.isSwipeAdInterstitial)

        assertEquals(5, parsed.interAdCounterTrigger)
        assertEquals(2, parsed.interAdBackCounterTrigger)
        assertEquals(4, parsed.clickAdCounterTrigger)

        assertFalse(parsed.bannerAdEnableHome)
        assertFalse(parsed.bannerAdEnableFindPhone)
        assertTrue(parsed.bannerAdEnableAlertScreen)

        assertFalse(parsed.nativeAdEnableDashboard)
        assertTrue(parsed.nativeAdEnableGoogleSearch)
        assertEquals(3, parsed.nativeAdGoogleSearchItemInterval)
    }

    @Test
    fun jsonParsing_handlesLegacyAliasesGracefully() {
        val legacyJson = """
            {
                "banner_ad_enable_contact_home": true,
                "banner_ad_id_contact_home": "test_home_id",
                "banner_ad_enable_alert_activity": true,
                "banner_ad_id_alert_activity": "test_alert_id",
                "inter_count": 4,
                "inter_back_count": 2,
                "isAppClickInterOn": true,
                "isRightLeftSwipeInterOn": false
            }
        """.trimIndent()

        val parsed = gson.fromJson(legacyJson, AdsConfig::class.java)

        assertTrue(parsed.bannerAdEnableHome)
        assertEquals("test_home_id", parsed.bannerAdIdHome)
        assertTrue(parsed.bannerAdEnableAlertScreen)
        assertEquals("test_alert_id", parsed.bannerAdIdAlertScreen)
        assertEquals(4, parsed.interAdCounterTrigger)
        assertEquals(2, parsed.interAdBackCounterTrigger)
        assertTrue(parsed.isClickAdInterstitial)
        assertFalse(parsed.isSwipeAdInterstitial)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. Strict Master & Screen Level Visibility Guards
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun masterBannerSwitchDisabled_disablesAllScreenBanners() {
        val config = AdsConfig.DEFAULT.copy(
            isBannerAdEnabled = false,
            bannerAdEnableHome = true,
            bannerAdEnableFindPhone = true,
            bannerAdEnableAlertScreen = true
        )

        assertFalse("canShowBanner should be false when master switch is off", config.canShowBanner)
        assertFalse("canShowBannerHome should be false", config.canShowBannerHome)
        assertFalse("canShowBannerFindPhone should be false", config.canShowBannerFindPhone)
        assertFalse("canShowBannerAlertScreen should be false", config.canShowBannerAlertScreen)
    }

    @Test
    fun masterNativeSwitchDisabled_disablesAllScreenNatives() {
        val config = AdsConfig.DEFAULT.copy(
            isNativeAdEnabled = false,
            nativeAdEnableDashboard = true,
            nativeAdEnableGoogleSearch = true
        )

        assertFalse("canShowNative should be false when master switch is off", config.canShowNative)
        assertFalse("canShowNativeDashboard should be false", config.canShowNativeDashboard)
        assertFalse("canShowNativeGoogleSearch should be false", config.canShowNativeGoogleSearch)
    }

    @Test
    fun masterInterSwitchDisabled_disablesInterstitial() {
        val config = AdsConfig.DEFAULT.copy(
            isInterAdEnabled = false,
            interAdId = "valid_inter_id"
        )

        assertFalse("canShowInter should be false when master switch is off", config.canShowInter)
    }

    @Test
    fun masterAppOpenSwitchDisabled_disablesAppOpen() {
        val config = AdsConfig.DEFAULT.copy(
            isAppOpenAdEnabled = false,
            appOpenAdId = "valid_open_id"
        )

        assertFalse("canShowAppOpen should be false when master switch is off", config.canShowAppOpen)
    }

    @Test
    fun blankAdUnitId_disablesAdDisplay() {
        val config = AdsConfig.DEFAULT.copy(
            isBannerAdEnabled = true,
            bannerAdEnableFindPhone = true,
            bannerAdIdFindPhone = "",
            isNativeAdEnabled = true,
            nativeAdEnableDashboard = true,
            nativeAdIdDashboard = "   ",
            isInterAdEnabled = true,
            interAdId = ""
        )

        assertFalse(config.canShowBannerFindPhone)
        assertFalse(config.canShowNativeDashboard)
        assertFalse(config.canShowInter)
    }
}
