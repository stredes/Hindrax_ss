package com.hindrax.ss.features.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hindrax.ss.data.repository.OllamaEndpointDefaults
import com.hindrax.ss.data.repository.OllamaRepository
import com.hindrax.ss.domain.ai.OpenAiResponsesPayloadBuilder
import com.hindrax.ss.domain.theme.HindraxThemePreset
import com.hindrax.ss.domain.theme.HindraxThemePresetCodec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val apiKeys: Map<String, String> = emptyMap(),
    val openAiModel: String = OpenAiResponsesPayloadBuilder.DEFAULT_MODEL,
    val ollamaFallbackEnabled: Boolean = false,
    val ollamaBaseUrl: String = OllamaEndpointDefaults.defaultBaseUrl(),
    val ollamaModel: String = "gemma3:1b",
    val ollamaPullStatus: String? = null,
    val isPullingOllamaModel: Boolean = false,
    val isDarkTheme: Boolean = true,
    val saveReportsAutomatically: Boolean = false,
    val themePreset: HindraxThemePreset = HindraxThemePreset(),
    val themeExport: String = HindraxThemePresetCodec.encode(HindraxThemePreset()),
    val themeImportDraft: String = "",
    val themeStatus: String? = null
)

class SettingsViewModel(
    private val ollamaRepository: OllamaRepository = OllamaRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    fun loadSettings(context: Context) {
        val prefs = context.getSharedPreferences("hindrax_prefs", Context.MODE_PRIVATE)
        _uiState.value = SettingsUiState(
            themePreset = prefs.getString("theme_preset", null)
                ?.let { runCatching { HindraxThemePresetCodec.decode(it) }.getOrNull() }
                ?: HindraxThemePreset(),
            apiKeys = mapOf(
                "OpenAI" to (prefs.getString("api_key_openai", "") ?: ""),
                "Shodan" to (prefs.getString("api_key_shodan", "") ?: ""),
                "Hunter.io" to (prefs.getString("api_key_hunter", "") ?: "")
            ),
            openAiModel = prefs.getString("openai_model", OpenAiResponsesPayloadBuilder.DEFAULT_MODEL)
                ?: OpenAiResponsesPayloadBuilder.DEFAULT_MODEL,
            ollamaFallbackEnabled = prefs.getBoolean("ollama_fallback_enabled", false),
            ollamaBaseUrl = prefs.getString("ollama_base_url", OllamaEndpointDefaults.defaultBaseUrl())
                ?: OllamaEndpointDefaults.defaultBaseUrl(),
            ollamaModel = prefs.getString("ollama_model", "gemma3:1b") ?: "gemma3:1b",
            ollamaPullStatus = _uiState.value.ollamaPullStatus,
            isPullingOllamaModel = _uiState.value.isPullingOllamaModel,
            isDarkTheme = prefs.getBoolean("dark_theme", true),
            saveReportsAutomatically = prefs.getBoolean("auto_save", false),
            themeImportDraft = _uiState.value.themeImportDraft,
            themeStatus = _uiState.value.themeStatus
        ).let { state ->
            state.copy(themeExport = HindraxThemePresetCodec.encode(state.themePreset))
        }
    }

    fun updateApiKey(context: Context, key: String, value: String) {
        val prefs = context.getSharedPreferences("hindrax_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("api_key_${key.lowercase()}", value).apply()
        loadSettings(context)
    }

    fun updateOpenAiModel(context: Context, value: String) {
        val prefs = context.getSharedPreferences("hindrax_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("openai_model", value.trim().ifBlank { OpenAiResponsesPayloadBuilder.DEFAULT_MODEL }).apply()
        loadSettings(context)
    }

    fun toggleOllamaFallback(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences("hindrax_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("ollama_fallback_enabled", enabled).apply()
        loadSettings(context)
    }

    fun updateOllamaBaseUrl(context: Context, value: String) {
        val normalized = OllamaEndpointDefaults.normalizeBaseUrl(value)
        val prefs = context.getSharedPreferences("hindrax_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("ollama_base_url", normalized).apply()
        loadSettings(context)
    }

    fun updateOllamaModel(context: Context, value: String) {
        val normalized = value.trim().ifBlank { "gemma3:1b" }
        val prefs = context.getSharedPreferences("hindrax_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("ollama_model", normalized).apply()
        loadSettings(context)
    }

    fun pullOllamaModel(context: Context) {
        val state = _uiState.value
        if (state.isPullingOllamaModel) return

        viewModelScope.launch {
            _uiState.value = state.copy(
                isPullingOllamaModel = true,
                ollamaPullStatus = "PULLING ${state.ollamaModel}..."
            )
            val result = runCatching {
                OllamaEndpointDefaults.validateReachableFromThisDevice(state.ollamaBaseUrl)
                ollamaRepository.pullModel(
                    baseUrl = state.ollamaBaseUrl,
                    model = state.ollamaModel
                )
            }
            _uiState.value = _uiState.value.copy(
                isPullingOllamaModel = false,
                ollamaPullStatus = result.fold(
                    onSuccess = { "OLLAMA_PULL_OK: $it" },
                    onFailure = { "OLLAMA_PULL_ERROR: ${it.message}" }
                )
            )
            loadSettings(context)
        }
    }

    fun toggleAutoSave(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences("hindrax_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("auto_save", enabled).apply()
        loadSettings(context)
    }

    fun updateThemeName(context: Context, value: String) {
        saveTheme(context, _uiState.value.themePreset.copy(name = value.ifBlank { "Hindrax Custom" }))
    }

    fun updateThemeColor(context: Context, key: String, value: String) {
        val current = _uiState.value.themePreset
        val next = when (key) {
            "background" -> current.copy(background = value)
            "surface" -> current.copy(surface = value)
            "text" -> current.copy(text = value)
            "accent" -> current.copy(accent = value)
            "warning" -> current.copy(warning = value)
            "danger" -> current.copy(danger = value)
            else -> current
        }
        saveTheme(context, normalizeTheme(next), "THEME_SAVED")
    }

    fun resetTheme(context: Context) {
        saveTheme(context, HindraxThemePreset(), "THEME_RESET")
    }

    fun updateThemeImportDraft(value: String) {
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
        saveTheme(context, decoded, "THEME_IMPORTED")
    }

    private fun saveTheme(context: Context, preset: HindraxThemePreset, status: String = "THEME_SAVED") {
        val normalized = normalizeTheme(preset)
        val prefs = context.getSharedPreferences("hindrax_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("theme_preset", HindraxThemePresetCodec.encode(normalized)).apply()
        _uiState.value = _uiState.value.copy(themeStatus = status)
        loadSettings(context)
    }

    private fun normalizeTheme(preset: HindraxThemePreset): HindraxThemePreset {
        val defaults = HindraxThemePreset()
        return preset.copy(
            background = HindraxThemePresetCodec.normalizeHex(preset.background, defaults.background),
            surface = HindraxThemePresetCodec.normalizeHex(preset.surface, defaults.surface),
            text = HindraxThemePresetCodec.normalizeHex(preset.text, defaults.text),
            accent = HindraxThemePresetCodec.normalizeHex(preset.accent, defaults.accent),
            warning = HindraxThemePresetCodec.normalizeHex(preset.warning, defaults.warning),
            danger = HindraxThemePresetCodec.normalizeHex(preset.danger, defaults.danger)
        )
    }
}
