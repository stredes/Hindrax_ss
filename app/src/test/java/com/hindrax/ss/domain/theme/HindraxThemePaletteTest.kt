package com.hindrax.ss.domain.theme

import org.junit.Assert.assertTrue
import org.junit.Test

class HindraxThemePaletteTest {
    @Test
    fun paletteContainsValidHexColors() {
        val allColors = HindraxThemePalette.all.map { it.hex }

        assertTrue(allColors.isNotEmpty())
        assertTrue(allColors.all { HindraxThemePresetCodec.normalizeHex(it, "") == it })
    }
}
