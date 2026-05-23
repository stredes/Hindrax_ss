package com.hindrax.ss.domain.theme

data class HindraxThemePreset(
    val name: String = "Hindrax Default",
    val background: String = "#050505",
    val surface: String = "#101010",
    val text: String = "#E8FFF2",
    val accent: String = "#00FF66",
    val warning: String = "#FFD54F",
    val danger: String = "#FF5252"
)

object HindraxThemePresetCodec {
    private val hexRegex = Regex("^#[0-9A-Fa-f]{6}$")

    fun encode(preset: HindraxThemePreset): String {
        val defaults = HindraxThemePreset()
        return listOf(
            "HINDRAX_THEME_V1",
            preset.name.escapeField(),
            normalizeHex(preset.background, defaults.background),
            normalizeHex(preset.surface, defaults.surface),
            normalizeHex(preset.text, defaults.text),
            normalizeHex(preset.accent, defaults.accent),
            normalizeHex(preset.warning, defaults.warning),
            normalizeHex(preset.danger, defaults.danger)
        ).joinToString("|")
    }

    fun decode(payload: String): HindraxThemePreset {
        val parts = payload.split("|")
        val defaults = HindraxThemePreset()
        if (parts.firstOrNull() != "HINDRAX_THEME_V1") return defaults
        return HindraxThemePreset(
            name = parts.getOrNull(1)?.unescapeField()?.ifBlank { defaults.name } ?: defaults.name,
            background = normalizeHex(parts.getOrNull(2).orEmpty(), defaults.background),
            surface = normalizeHex(parts.getOrNull(3).orEmpty(), defaults.surface),
            text = normalizeHex(parts.getOrNull(4).orEmpty(), defaults.text),
            accent = normalizeHex(parts.getOrNull(5).orEmpty(), defaults.accent),
            warning = normalizeHex(parts.getOrNull(6).orEmpty(), defaults.warning),
            danger = normalizeHex(parts.getOrNull(7).orEmpty(), defaults.danger)
        )
    }

    fun normalizeHex(value: String, fallback: String): String {
        val normalized = value.trim()
        return if (hexRegex.matches(normalized)) normalized.uppercase() else fallback
    }

    private fun String.escapeField(): String = replace("%", "%25").replace("|", "%7C")
    private fun String.unescapeField(): String = replace("%7C", "|").replace("%25", "%")
}
