package com.hindrax.ss.features.dashboard

object DashboardAsciiMetrics {
    fun bannerFontSp(availableWidthDp: Int, smallestScreenWidthDp: Int): Int {
        return when {
            availableWidthDp < 300 -> 5
            availableWidthDp < 360 -> 6
            availableWidthDp < 480 -> 7
            smallestScreenWidthDp >= 600 && availableWidthDp < 720 -> 10
            smallestScreenWidthDp >= 600 -> 12
            else -> 8
        }
    }

    fun bannerLineHeightSp(fontSp: Int): Int {
        return fontSp + 1
    }

    fun nodeLogoSizeDp(availableWidthDp: Int): Int {
        return when {
            availableWidthDp < 320 -> 58
            availableWidthDp < 420 -> 70
            availableWidthDp < 600 -> 82
            else -> 104
        }
    }

    fun nodeTextSp(availableWidthDp: Int, tablet: Boolean): Int {
        return when {
            availableWidthDp < 320 -> 9
            availableWidthDp < 420 -> 10
            tablet -> 13
            else -> 11
        }
    }

    fun shouldStackSystemSignal(availableWidthDp: Int): Boolean {
        return availableWidthDp < 360
    }
}
