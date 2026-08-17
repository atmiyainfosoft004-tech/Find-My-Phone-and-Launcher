package com.example.findmyphonebyclaplauncher

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.findmyphonebyclaplauncher.ads.config.AdsConfig
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented UI tests verifying ad visibility guards, screen layout conditions, and system UI configurations.
 */
@RunWith(AndroidJUnit4::class)
class ScreenAdVisibilityAndImmersionTest {

    @Test
    fun appContext_isCorrect() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertTrue(appContext.packageName.contains("findmyphonebyclaplauncher"))
    }

    @Test
    fun dashboardNativeDisabled_flagReflectsImmediateCollapseState() {
        val config = AdsConfig.DEFAULT.copy(
            isNativeAdEnabled = true,
            nativeAdEnableDashboard = false
        )

        assertFalse("canShowNativeDashboard must evaluate to false when disabled", config.canShowNativeDashboard)
    }

    @Test
    fun alertBannerDisabled_flagReflectsImmediateCollapseState() {
        val config = AdsConfig.DEFAULT.copy(
            isBannerAdEnabled = true,
            bannerAdEnableAlertScreen = false
        )

        assertFalse("canShowBannerAlertScreen must evaluate to false when disabled", config.canShowBannerAlertScreen)
    }

    @Test
    fun stickyImmersionFlag_isTrueByDefault() {
        val config = AdsConfig.DEFAULT
        assertTrue("system_hide_navigation_bar_auto must default to true", config.systemHideNavigationBarAuto)
    }
}
