package com.hindrax.ss.features.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hindrax.ss.data.repository.OllamaRepository
import com.hindrax.ss.domain.ai.OpenAiResponsesPayloadBuilder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val apiKeys: Map<String, String> = emptyMap(),
    val openAiModel: String = OpenAiResponsesPayloadBuilder.DEFAULT_MODEL,
    val ollamaFallbackEnabled: Boolean = false,
    val ollamaBaseUrl: String = "http://10.0.2.2:11434",
    val ollamaModel: String = "gemma3:1b",
    val ollamaPullStatus: String? = null,
    val isPullingOllamaModel: Boolean = false,
    val isDarkTheme: Boolean = true,
    val saveReportsAutomatically: Boolean = false
)

class SettingsViewModel(
    private val ollamaRepository: OllamaRepository = OllamaRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    fun loadSettings(context: Context) {
        val prefs = context.getSharedPreferences("hindrax_prefs", Context.MODE_PRIVATE)
        _uiState.value = SettingsUiState(
            apiKeys = mapOf(
                "OpenAI" to (prefs.getString("api_key_openai", "") ?: ""),
                "Shodan" to (prefs.getString("api_key_shodan", "") ?: ""),
                "Hunter.io" to (prefs.getString("api_key_hunter", "") ?: "")
            ),
            openAiModel = prefs.getString("openai_model", OpenAiResponsesPayloadBuilder.DEFAULT_MODEL)
                ?: OpenAiResponsesPayloadBuilder.DEFAULT_MODEL,
            ollamaFallbackEnabled = prefs.getBoolean("ollama_fallback_enabled", false),
            ollamaBaseUrl = prefs.getString("ollama_base_url", "http://10.0.2.2:11434")
                ?: "http://10.0.2.2:11434",
            ollamaModel = prefs.getString("ollama_model", "gemma3:1b") ?: "gemma3:1b",
            ollamaPullStatus = _uiState.value.ollamaPullStatus,
            isPullingOllamaModel = _uiState.value.isPullingOllamaModel,
            isDarkTheme = prefs.getBoolean("dark_theme", true),
            saveReportsAutomatically = prefs.getBoolean("auto_save", false)
        )
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
        val normalized = value.trim().ifBlank { "http://10.0.2.2:11434" }
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
}
