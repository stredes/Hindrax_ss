package com.hindrax.ss.core.util

import android.content.Context
import com.hindrax.ss.domain.theme.HindraxThemeLibraryCodec
import com.hindrax.ss.domain.theme.HindraxThemePreset
import com.hindrax.ss.domain.theme.HindraxThemePresetCodec

object HindraxThemeStore {
    const val PREFS_NAME = "hindrax_prefs"
    const val KEY_ACTIVE_THEME = "theme_preset"
    const val KEY_THEME_LIBRARY = "theme_library"

    fun loadActiveTheme(context: Context): HindraxThemePreset {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_ACTIVE_THEME, null)
            ?.let { runCatching { HindraxThemePresetCodec.decode(it) }.getOrNull() }
            ?: HindraxThemePreset()
    }

    fun saveActiveTheme(context: Context, preset: HindraxThemePreset): HindraxThemePreset {
        val normalized = normalize(preset)
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ACTIVE_THEME, HindraxThemePresetCodec.encode(normalized))
            .apply()
        return normalized
    }

    fun loadLibrary(context: Context): List<HindraxThemePreset> {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stored = prefs.getString(KEY_THEME_LIBRARY, null)
            ?.let { runCatching { HindraxThemeLibraryCodec.decode(it) }.getOrDefault(emptyList()) }
            .orEmpty()
            .map(::normalize)
        val active = loadActiveTheme(context)
        return upsert(stored, active).sortedBy { it.name.lowercase() }
    }

    fun saveThemeToProfile(context: Context, preset: HindraxThemePreset): List<HindraxThemePreset> {
        val next = upsert(loadLibrary(context), normalize(preset))
        saveLibrary(context, next)
        return next
    }

    fun applyTheme(context: Context, preset: HindraxThemePreset): HindraxThemePreset {
        val normalized = saveActiveTheme(context, preset)
        saveThemeToProfile(context, normalized)
        return normalized
    }

    fun deleteTheme(context: Context, name: String): List<HindraxThemePreset> {
        val normalizedName = name.trim()
        val next = loadLibrary(context).filterNot { it.name.equals(normalizedName, ignoreCase = true) }
        saveLibrary(context, next)
        return next
    }

    fun normalize(preset: HindraxThemePreset): HindraxThemePreset {
        val defaults = HindraxThemePreset()
        return preset.copy(
            name = preset.name.trim().ifBlank { defaults.name },
            background = HindraxThemePresetCodec.normalizeHex(preset.background, defaults.background),
            surface = HindraxThemePresetCodec.normalizeHex(preset.surface, defaults.surface),
            text = HindraxThemePresetCodec.normalizeHex(preset.text, defaults.text),
            accent = HindraxThemePresetCodec.normalizeHex(preset.accent, defaults.accent),
            warning = HindraxThemePresetCodec.normalizeHex(preset.warning, defaults.warning),
            danger = HindraxThemePresetCodec.normalizeHex(preset.danger, defaults.danger)
        )
    }

    fun nextThemeName(themes: List<HindraxThemePreset>): String {
        val existing = themes.map { it.name.trim().lowercase() }.toSet()
        var index = 1
        while ("hindrax theme $index" in existing) {
            index += 1
        }
        return "Hindrax Theme $index"
    }

    private fun saveLibrary(context: Context, themes: List<HindraxThemePreset>) {
        val normalized = themes.map(::normalize).distinctBy { it.name.lowercase() }
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME_LIBRARY, HindraxThemeLibraryCodec.encode(normalized))
            .apply()
    }

    private fun upsert(
        current: List<HindraxThemePreset>,
        preset: HindraxThemePreset
    ): List<HindraxThemePreset> {
        val normalized = normalize(preset)
        return current
            .filterNot { it.name.equals(normalized.name, ignoreCase = true) }
            .plus(normalized)
    }
}
