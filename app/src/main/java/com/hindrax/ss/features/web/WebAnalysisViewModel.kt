package com.hindrax.ss.features.web

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hindrax.ss.core.model.Severity
import com.hindrax.ss.data.entity.AuditResultEntity
import com.hindrax.ss.data.entity.AuditSessionEntity
import com.hindrax.ss.data.repository.AuditRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class WebUiState(
    val url: String = "https://",
    val isRunning: Boolean = false,
    val logs: String = "",
    val headers: Map<String, String> = emptyMap()
)

class WebAnalysisViewModel(
    private val auditRepository: AuditRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(WebUiState())
    val uiState = _uiState.asStateFlow()

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    fun onUrlChange(newUrl: String) {
        _uiState.value = _uiState.value.copy(url = newUrl)
    }

    fun analyzeWeb() {
        val url = _uiState.value.url
        if (!url.startsWith("http")) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRunning = true, logs = "Analizando headers de $url...\n")
            
            val sessionId = auditRepository.startSession(
                AuditSessionEntity(
                    title = "Web Header Analysis",
                    taskType = "WEB_HEADERS",
                    target = url,
                    targetType = "URL",
                    authorizationMode = "MANUAL",
                    status = "RUNNING",
                    startedAt = System.currentTimeMillis()
                )
            )

            try {
                val request = Request.Builder().url(url).build()
                val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
                
                val headersMap = mutableMapOf<String, String>()
                response.headers.forEach { pair ->
                    headersMap[pair.first] = pair.second
                }

                _uiState.value = _uiState.value.copy(headers = headersMap)

                // Check security headers
                checkSecurityHeader(sessionId, "Content-Security-Policy", headersMap)
                checkSecurityHeader(sessionId, "X-Frame-Options", headersMap)
                checkSecurityHeader(sessionId, "X-Content-Type-Options", headersMap)
                checkSecurityHeader(sessionId, "Strict-Transport-Security", headersMap)

                _uiState.value = _uiState.value.copy(
                    isRunning = false,
                    logs = _uiState.value.logs + "Análisis completado. ${headersMap.size} headers encontrados.\n"
                )

                val session = auditRepository.getSessionById(sessionId)
                if (session != null) {
                    auditRepository.updateSession(session.copy(status = "FINISHED", finishedAt = System.currentTimeMillis()))
                }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRunning = false,
                    logs = _uiState.value.logs + "Error: ${e.message}\n"
                )
            }
        }
    }

    private suspend fun checkSecurityHeader(sessionId: Long, header: String, headers: Map<String, String>) {
        if (!headers.containsKey(header)) {
            auditRepository.saveResult(
                AuditResultEntity(
                    sessionId = sessionId,
                    severity = Severity.LOW.name,
                    category = "Web Security",
                    findingTitle = "Falta header $header",
                    findingBody = "La cabecera de seguridad $header no está presente en la respuesta.",
                    recommendation = "Implementar la cabecera $header para mejorar la postura de seguridad."
                )
            )
        }
    }
}
