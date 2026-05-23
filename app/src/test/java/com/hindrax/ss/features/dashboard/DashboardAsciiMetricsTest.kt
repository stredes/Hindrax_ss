package com.hindrax.ss.features.dashboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardAsciiMetricsTest {
    @Test
    fun scalesBannerFontForSmallPhonesAndTablets() {
        assertEquals(5, DashboardAsciiMetrics.bannerFontSp(280, 360))
        assertEquals(7, DashboardAsciiMetrics.bannerFontSp(390, 360))
        assertEquals(12, DashboardAsciiMetrics.bannerFontSp(900, 800))
    }

    @Test
    fun stacksSystemSignalOnlyOnVeryNarrowPanels() {
        assertTrue(DashboardAsciiMetrics.shouldStackSystemSignal(320))
        assertFalse(DashboardAsciiMetrics.shouldStackSystemSignal(420))
    }
}
