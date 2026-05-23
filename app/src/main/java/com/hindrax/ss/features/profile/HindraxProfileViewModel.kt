package com.hindrax.ss.features.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import com.hindrax.ss.core.util.DeviceIdManager
import com.hindrax.ss.core.util.HindraxThemeStore
import com.hindrax.ss.domain.theme.HindraxThemePreset
import com.hindrax.ss.domain.theme.HindraxThemePresetCodec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class HindraxProfileUiState(
    val deviceId: String = "",
    val nickname: String = "",
    val saved: Boolean = false,
    val activeTheme: HindraxThemePreset = HindraxThemePreset(),
    val savedThemes: List<HindraxThemePreset> = listOf(HindraxThemePreset()),
    val themeImportDraft: String = "",
    val themeStatus: String? = null
)

class HindraxProfileViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(HindraxProfileUiState())
    val uiState = _uiState.asStateFlow()

    fun load(context: Context) {
        val manager = DeviceIdManager(context.applicationContext)
        _uiState.value = HindraxProfileUiState(
            deviceId = manager.getDeviceId(),
            nickname = manager.getNickname(),
            activeTheme = HindraxThemeStore.loadActiveTheme(context),
            savedThemes = HindraxThemeStore.loadLibrary(context),
            themeImportDraft = _uiState.value.themeImportDraft,
            themeStatus = _uiState.value.themeStatus
        )
    }

    fun onNicknameChange(value: String) {
        _uiState.value = _uiState.value.copy(nickname = value, saved = false)
    }

    fun save(context: Context) {
        val manager = DeviceIdManager(context.applicationContext)
        manager.setNickname(_uiState.value.nickname)
        _uiState.value = _uiState.value.copy(
            nickname = manager.getNickname(),
            saved = true
        )
    }

    fun applyTheme(context: Context, preset: HindraxThemePreset) {
        val active = HindraxThemeStore.applyTheme(context, preset)
        _uiState.value = _uiState.value.copy(
            activeTheme = active,
            savedThemes = HindraxThemeStore.loadLibrary(context),
            themeStatus = "THEME_APPLIED: ${active.name}"
        )
    }

    fun deleteTheme(context: Context, preset: HindraxThemePreset) {
        val themes = HindraxThemeStore.deleteTheme(context, preset.name)
        _uiState.value = _uiState.value.copy(
            savedThemes = themes,
            themeStatus = "THEME_DELETED: ${preset.name}"
        )
    }

    fun themeExport(preset: HindraxThemePreset): String {
        return HindraxThemePresetCodec.encode(preset)
    }

    fun onThemeImportDraftChange(value: String) {
        _uiState.value = _uiState.value.copy(themeImportDraft = value)
    }

    fun importTheme(context: Context) {
        val decoded = runCatching {
            HindraxThemePresetCodec.decode(_uiState.value.themeImportDraft)
        }.getOrNull()
        if (decoded == null) {
            _uiState.value = _uiState.value.copy(themeStatus = "THEME_IMPORT_ERROR")
            return
        }
        val active = HindraxThemeStore.applyTheme(context, decoded)
        _uiState.value = _uiState.value.copy(
            activeTheme = active,
            savedThemes = HindraxThemeStore.loadLibrary(context),
            themeImportDraft = "",
            themeStatus = "THEME_IMPORTED_TO_PROFILE"
        )
    }
}
