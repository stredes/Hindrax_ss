package com.hindrax.ss.features.ai

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hindrax.ss.data.repository.OllamaEndpointDefaults
import com.hindrax.ss.data.repository.OllamaFallbackConfig
import com.hindrax.ss.data.repository.OpenAiRepository
import com.hindrax.ss.domain.ai.OpenAiResponsesPayloadBuilder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HindraxAiUiState(
    val prompt: String = "",
    val response: String = "",
    val apiKeyConfigured: Boolean = false,
    val model: String = OpenAiResponsesPayloadBuilder.DEFAULT_MODEL,
    val ollamaFallbackEnabled: Boolean = false,
    val ollamaModel: String = "gemma3:1b",
    val isRunning: Boolean = false,
    val error: String? = null
)

class HindraxAiViewModel(
    private val repository: OpenAiRepository = OpenAiRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(HindraxAiUiState())
    val uiState = _uiState.asStateFlow()

    fun load(context: Context) {
        val prefs = context.getSharedPreferences("hindrax_prefs", Context.MODE_PRIVATE)
        val apiKey = prefs.getString("api_key_openai", "").orEmpty()
        val model = prefs.getString("openai_model", OpenAiResponsesPayloadBuilder.DEFAULT_MODEL)
            ?: OpenAiResponsesPayloadBuilder.DEFAULT_MODEL
        val ollamaFallbackEnabled = prefs.getBoolean("ollama_fallback_enabled", false)
        val ollamaModel = prefs.getString("ollama_model", "gemma3:1b") ?: "gemma3:1b"
        _uiState.value = _uiState.value.copy(
            apiKeyConfigured = apiKey.isNotBlank(),
            model = model,
            ollamaFallbackEnabled = ollamaFallbackEnabled,
            ollamaModel = ollamaModel
        )
    }

    fun onPromptChange(value: String) {
        _uiState.value = _uiState.value.copy(prompt = value, error = null)
    }

    fun ask(context: Context, appContext: String = "MODULE=HINDRAX_CORE") {
        val prompt = _uiState.value.prompt.trim()
        if (prompt.isBlank()) return

        val prefs = context.getSharedPreferences("hindrax_prefs", Context.MODE_PRIVATE)
        val apiKey = prefs.getString("api_key_openai", "").orEmpty()
        val model = prefs.getString("openai_model", OpenAiResponsesPayloadBuilder.DEFAULT_MODEL)
            ?: OpenAiResponsesPayloadBuilder.DEFAULT_MODEL
        val ollamaFallback = OllamaFallbackConfig(
            enabled = prefs.getBoolean("ollama_fallback_enabled", false),
            baseUrl = prefs.getString("ollama_base_url", OllamaEndpointDefaults.defaultBaseUrl()).orEmpty(),
            model = prefs.getString("ollama_model", "gemma3:1b") ?: "gemma3:1b"
        )

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRunning = true, error = null, response = "")
            val result = runCatching {
                repository.ask(
                    apiKey = apiKey,
                    model = model,
                    prompt = prompt,
                    appContext = appContext,
                    toolExecutor = HindraxAiToolExecutor(context.applicationContext)::execute,
                    ollamaFallback = ollamaFallback
                )
            }
            _uiState.value = result.fold(
                onSuccess = {
                    _uiState.value.copy(
                        isRunning = false,
                        response = it,
                        apiKeyConfigured = apiKey.isNotBlank(),
                        model = model,
                        ollamaFallbackEnabled = ollamaFallback.enabled,
                        ollamaModel = ollamaFallback.model
                    )
                },
                onFailure = {
                    _uiState.value.copy(
                        isRunning = false,
                        error = it.message ?: "OPENAI_REQUEST_FAILED",
                        apiKeyConfigured = apiKey.isNotBlank(),
                        model = model,
                        ollamaFallbackEnabled = ollamaFallback.enabled,
                        ollamaModel = ollamaFallback.model
                    )
                }
            )
        }
    }
}
