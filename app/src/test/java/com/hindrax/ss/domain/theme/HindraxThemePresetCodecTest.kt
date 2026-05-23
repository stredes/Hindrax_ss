package com.hindrax.ss.domain.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class HindraxThemePresetCodecTest {
    @Test
    fun exportsAndImportsThemePreset() {
        val preset = HindraxThemePreset(
            name = "Matrix Cyan",
            background = "#050505",
            surface = "#101010",
            text = "#E8FFF2",
            accent = "#00E5FF",
            warning = "#FFD54F",
            danger = "#FF5252"
        )

        val encoded = HindraxThemePresetCodec.encode(preset)
        val decoded = HindraxThemePresetCodec.decode(encoded)

        assertEquals(preset, decoded)
    }

    @Test
    fun normalizesInvalidHexToDefault() {
        val normalized = HindraxThemePresetCodec.normalizeHex("verde", "#00FF00")

        assertEquals("#00FF00", normalized)
    }
}
