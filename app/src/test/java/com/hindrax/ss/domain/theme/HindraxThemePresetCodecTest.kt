package com.hindrax.ss.domain.theme

import com.hindrax.ss.core.util.HindraxThemeStore
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
            info = "#00D8FF",
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

    @Test
    fun storesAndRestoresThemeLibrary() {
        val themes = listOf(
            HindraxThemePreset(name = "Default"),
            HindraxThemePreset(name = "Blue Ops", accent = "#2196F3")
        )

        val encoded = HindraxThemeLibraryCodec.encode(themes)
        val decoded = HindraxThemeLibraryCodec.decode(encoded)

        assertEquals(themes, decoded)
    }

    @Test
    fun nextThemeNameSkipsExistingProfileThemes() {
        val themes = listOf(
            HindraxThemePreset(name = "Hindrax Theme 1"),
            HindraxThemePreset(name = "Hindrax Theme 2")
        )

        assertEquals("Hindrax Theme 3", HindraxThemeStore.nextThemeName(themes))
    }
}
