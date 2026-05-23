package com.hindrax.ss.features.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hindrax.ss.BuildConfig
import com.hindrax.ss.core.util.HindraxThemeStore
import com.hindrax.ss.data.repository.OllamaEndpointDefaults
import com.hindrax.ss.data.repository.OllamaRepository
import com.hindrax.ss.data.remote.ApiHindraxConfigStore
import com.hindrax.ss.domain.ai.OpenAiResponsesPayloadBuilder
import com.hindrax.ss.domain.sync.ApiHindraxEndpoint
import com.hindrax.ss.domain.sync.ApiHindraxReleaseDefaults
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
    val apiHindraxEnabled: Boolean = false,
    val apiHindraxBaseUrl: String = ApiHindraxEndpoint.DEFAULT_BASE_URL,
    val apiHindraxToken: String = "",
    val isDarkTheme: Boolean = true,
    val saveReportsAutomatically: Boolean = false,
    val themePreset: HindraxThemePreset = HindraxThemePreset(),
    val savedThemes: List<HindraxThemePreset> = listOf(HindraxThemePreset()),
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
        val activeTheme = HindraxThemeStore.loadActiveTheme(context)
        val apiDefaults = ApiHindraxReleaseDefaults(
            enabled = BuildConfig.API_HINDRAX_DEFAULT_ENABLED,
            baseUrl = BuildConfig.API_HINDRAX_DEFAULT_BASE_URL,
            token = BuildConfig.API_HINDRAX_DEFAULT_TOKEN
        )
        _uiState.value = SettingsUiState(
            themePreset = activeTheme,
            savedThemes = HindraxThemeStore.loadLibrary(context),
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
            apiHindraxEnabled = prefs.getBoolean(
                ApiHindraxConfigStore.KEY_ENABLED,
                apiDefaults.isReady
            ),
            apiHindraxBaseUrl = prefs.getString(
                ApiHindraxConfigStore.KEY_BASE_URL,
                apiDefaults.normalizedBaseUrl
            ) ?: apiDefaults.normalizedBaseUrl,
            apiHindraxToken = prefs.getString(
                ApiHindraxConfigStore.KEY_TOKEN,
                apiDefaults.token
            ) ?: apiDefaults.token,
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

    fun toggleApiHindrax(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences("hindrax_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean(ApiHindraxConfigStore.KEY_ENABLED, enabled).apply()
        loadSettings(context)
    }

    fun updateApiHindraxBaseUrl(context: Context, value: String) {
        val normalized = ApiHindraxEndpoint.normalizeBaseUrl(value)
        val prefs = context.getSharedPreferences("hindrax_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString(ApiHindraxConfigStore.KEY_BASE_URL, normalized).apply()
        loadSettings(context)
    }

    fun updateApiHindraxToken(context: Context, value: String) {
        val prefs = context.getSharedPreferences("hindrax_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString(ApiHindraxConfigStore.KEY_TOKEN, value.trim()).apply()
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
            "info" -> current.copy(info = value)
            "warning" -> current.copy(warning = value)
            "danger" -> current.copy(danger = value)
            else -> current
        }
        saveTheme(context, normalizeTheme(next), "THEME_SAVED")
    }

    fun resetTheme(context: Context) {
        saveTheme(context, HindraxThemePreset(), "THEME_RESET")
    }

    fun createNewTheme(context: Context) {
        val name = HindraxThemeStore.nextThemeName(_uiState.value.savedThemes)
        saveTheme(context, HindraxThemePreset(name = name), "NEW_THEME_READY")
    }

    fun saveThemeToProfile(context: Context) {
        HindraxThemeStore.saveThemeToProfile(context, _uiState.value.themePreset)
        _uiState.value = _uiState.value.copy(themeStatus = "THEME_SAVED_IN_PROFILE")
        loadSettings(context)
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
        HindraxThemeStore.saveThemeToProfile(context, decoded)
        saveTheme(context, decoded, "THEME_IMPORTED")
    }

    private fun saveTheme(context: Context, preset: HindraxThemePreset, status: String = "THEME_SAVED") {
        HindraxThemeStore.saveActiveTheme(context, normalizeTheme(preset))
        _uiState.value = _uiState.value.copy(themeStatus = status)
        loadSettings(context)
    }

    private fun normalizeTheme(preset: HindraxThemePreset): HindraxThemePreset {
        return HindraxThemeStore.normalize(preset)
    }
}
