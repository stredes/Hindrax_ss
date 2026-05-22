package com.hindrax.ss.data.repository

import com.hindrax.ss.domain.ai.OpenAiResponseParser
import com.hindrax.ss.domain.ai.OpenAiResponsesPayloadBuilder
import com.hindrax.ss.domain.ai.OpenAiToolOutput
import com.hindrax.ss.domain.ai.OpenAiFunctionCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class OpenAiRepository(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
) {
    private val ollamaRepository = OllamaRepository()

    suspend fun ask(
        apiKey: String,
        model: String,
        prompt: String,
        appContext: String,
        toolExecutor: (suspend (OpenAiFunctionCall) -> OpenAiToolOutput)? = null,
        ollamaFallback: OllamaFallbackConfig = OllamaFallbackConfig(
            enabled = false,
            baseUrl = "",
            model = ""
        )
    ): String = withContext(Dispatchers.IO) {
        val normalizedKey = apiKey.trim()
        require(normalizedKey.startsWith("sk-")) { "OPENAI_API_KEY_MISSING_OR_INVALID" }
        require(prompt.isNotBlank()) { "EMPTY_PROMPT" }

        val body = if (toolExecutor == null) {
            OpenAiResponsesPayloadBuilder.build(
                model = model,
                userInput = prompt,
                appContext = appContext
            )
        } else {
            OpenAiResponsesPayloadBuilder.buildWithTools(
                model = model,
                userInput = prompt,
                appContext = appContext
            )
        }

        val responseBody = runCatching { executeRequest(normalizedKey, body) }
            .getOrElse { error ->
                if (ollamaFallback.isUsable && error.isOpenAiQuotaError()) {
                    return@withContext ollamaFallbackNotice(
                        ollamaRepository.generate(
                            config = ollamaFallback,
                            prompt = prompt,
                            appContext = appContext
                        )
                    )
                }
                throw error
            }
        if (toolExecutor == null) {
            return@withContext OpenAiResponseParser.extractText(responseBody)
        }

        val calls = OpenAiResponseParser.extractFunctionCalls(responseBody)
        if (calls.isEmpty()) {
            return@withContext OpenAiResponseParser.extractText(responseBody)
        }

        val responseId = OpenAiResponseParser.extractResponseId(responseBody)
            ?: return@withContext OpenAiResponseParser.extractText(responseBody)

        val outputs = calls.map { call -> toolExecutor(call) }
        val followUpBody = OpenAiResponsesPayloadBuilder.buildToolOutputFollowUp(
            model = model,
            previousResponseId = responseId,
            outputs = outputs
        )
        val finalBody = runCatching { executeRequest(normalizedKey, followUpBody) }
            .getOrElse { error ->
                if (ollamaFallback.isUsable && error.isOpenAiQuotaError()) {
                    return@withContext ollamaFallbackNotice(
                        ollamaRepository.generate(
                            config = ollamaFallback,
                            prompt = prompt,
                            appContext = appContext
                        )
                    )
                }
                throw error
            }
        val finalText = OpenAiResponseParser.extractText(finalBody)
        finalText.takeIf { it != "EMPTY_OPENAI_RESPONSE" }
            ?: outputs.joinToString("\n") { it.output }
    }

    private fun executeRequest(apiKey: String, body: String): String {
        val request = Request.Builder()
            .url(OPENAI_RESPONSES_URL)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        return httpClient.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val parsedError = runCatching { OpenAiResponseParser.extractText(responseBody) }.getOrNull()
                error(parsedError ?: "OPENAI_HTTP_${response.code}")
            }
            responseBody
        }
    }

    private fun Throwable.isOpenAiQuotaError(): Boolean {
        val normalized = message.orEmpty().lowercase()
        return normalized.contains("openai_http_429") ||
            normalized.contains("rate_limit") ||
            normalized.contains("rate limit") ||
            normalized.contains("insufficient_quota") ||
            normalized.contains("quota") ||
            normalized.contains("billing")
    }

    private fun ollamaFallbackNotice(response: String): String {
        return """
--- OLLAMA_FALLBACK_ACTIVE ---
OpenAI quota/rate-limit detected. Response generated with local Ollama fallback.

$response
""".trimIndent()
    }

    private companion object {
        const val OPENAI_RESPONSES_URL = "https://api.openai.com/v1/responses"
    }
}
