package com.hindrax.ss.domain.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class HindraxThemeRolePaletteTest {
    @Test
    fun rolePaletteNormalizesPresetColorsForGlobalUi() {
        val palette = HindraxThemeRolePalette.fromPreset(
            HindraxThemePreset(
                background = "#111111",
                surface = "#222222",
                text = "#EEEEEE",
                accent = "#00AA00",
                info = "#00D8FF",
                warning = "bad",
                danger = "#AA0000"
            )
        )

        assertEquals("#111111", palette.background)
        assertEquals("#222222", palette.surface)
        assertEquals("#EEEEEE", palette.text)
        assertEquals("#00AA00", palette.accent)
        assertEquals("#00D8FF", palette.info)
        assertEquals(HindraxThemePreset().warning, palette.warning)
        assertEquals("#AA0000", palette.danger)
    }
}
