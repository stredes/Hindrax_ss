package com.hindrax.ss.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

data class OllamaFallbackConfig(
    val enabled: Boolean,
    val baseUrl: String,
    val model: String
) {
    val isUsable: Boolean
        get() = enabled && baseUrl.isNotBlank() && model.isNotBlank()
}

class OllamaRepository(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()
) {
    suspend fun generate(
        config: OllamaFallbackConfig,
        prompt: String,
        appContext: String
    ): String = withContext(Dispatchers.IO) {
        require(config.isUsable) { "OLLAMA_FALLBACK_NOT_CONFIGURED" }
        require(prompt.isNotBlank()) { "EMPTY_PROMPT" }

        val fullPrompt = buildString {
            appendLine(HINDRAX_OLLAMA_INSTRUCTIONS)
            appendLine()
            appendLine(appContext.trim().ifBlank { "MODULE=HINDRAX_CORE" })
            appendLine()
            appendLine("USER_REQUEST:")
            append(prompt.trim())
        }

        val body = buildJsonObject {
            put("model", config.model.trim())
            put("prompt", fullPrompt)
            put("stream", false)
            putJsonObject("options") {
                put("temperature", 0.2)
                put("num_predict", 900)
            }
        }.toString()

        val responseBody = postJson(config.baseUrl, "/api/generate", body)
        val root = Json.parseToJsonElement(responseBody).jsonObject
        root["response"]?.jsonPrimitive?.content?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: "EMPTY_OLLAMA_RESPONSE"
    }

    suspend fun pullModel(baseUrl: String, model: String): String = withContext(Dispatchers.IO) {
        require(baseUrl.isNotBlank()) { "OLLAMA_BASE_URL_REQUIRED" }
        require(model.isNotBlank()) { "OLLAMA_MODEL_REQUIRED" }

        val body = buildJsonObject {
            put("model", model.trim())
            put("stream", false)
        }.toString()

        val responseBody = postJson(baseUrl, "/api/pull", body)
        val root = Json.parseToJsonElement(responseBody).jsonObject
        root["status"]?.jsonPrimitive?.content?.ifBlank { "success" } ?: "success"
    }

    private fun postJson(baseUrl: String, path: String, body: String): String {
        val url = "${baseUrl.trim().trimEnd('/')}$path"
        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        return httpClient.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                error("OLLAMA_HTTP_${response.code}: ${responseBody.take(180)}")
            }
            responseBody
        }
    }

    private companion object {
        const val HINDRAX_OLLAMA_INSTRUCTIONS = """
You are Hindrax AI running through a local Ollama fallback model.
Help the user operate Hindrax in a defensive, owner-authorized Android security lab.
Keep answers concise, practical, terminal-friendly, and avoid destructive or unauthorized instructions.
Ollama fallback mode cannot call Hindrax app tools directly; explain the safe module/action to run when execution is needed.
"""
    }
}
