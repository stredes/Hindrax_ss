package com.hindrax.ss.features.web

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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

data class WebScannerUiState(
    val url: String = "https://",
    val isRunning: Boolean = false,
    val logs: String = "",
    val progress: Float = 0f,
    val findings: List<String> = emptyList()
)

class WebScannerViewModel(
    private val auditRepository: AuditRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(WebScannerUiState())
    val uiState = _uiState.asStateFlow()

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .followRedirects(false)
        .build()

    private val pathsToScan = listOf(
        ".git/config", ".env", "wp-config.php.bak", "phpinfo.php",
        "server-status", ".htaccess", "config.json", "admin/"
    )

    fun onUrlChange(newUrl: String) {
        _uiState.value = _uiState.value.copy(url = newUrl)
    }

    fun startScan() {
        val baseUrl = _uiState.value.url.removeSuffix("/")
        if (!baseUrl.startsWith("http")) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRunning = true, logs = "Iniciando escaneo en $baseUrl...\n", findings = emptyList())
            
            val sessionId = auditRepository.startSession(
                AuditSessionEntity(
                    title = "Web Discovery",
                    taskType = "WEB_SCAN",
                    target = baseUrl,
                    targetType = "URL",
                    authorizationMode = "VERIFIED",
                    status = "RUNNING",
                    startedAt = System.currentTimeMillis()
                )
            )

            pathsToScan.forEachIndexed { index, path ->
                val fullUrl = "$baseUrl/$path"
                try {
                    val request = Request.Builder().url(fullUrl).build()
                    val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
                    
                    if (response.isSuccessful || response.code == 403) {
                        val status = if (response.isSuccessful) "OK (200)" else "FORBIDDEN (403)"
                        val currentFindings = _uiState.value.findings.toMutableList()
                        currentFindings.add("$path - $status")
                        
                        _uiState.value = _uiState.value.copy(
                            logs = _uiState.value.logs + "[+] Encontrado: $path\n",
                            findings = currentFindings,
                            progress = (index + 1).toFloat() / pathsToScan.size
                        )

                        auditRepository.saveResult(
                            AuditResultEntity(
                                sessionId = sessionId,
                                severity = if (response.isSuccessful) Severity.HIGH.name else Severity.MEDIUM.name,
                                category = "Discovery",
                                findingTitle = "Ruta Sensible: $path",
                                findingBody = "Se detectó acceso al recurso: $fullUrl"
                            )
                        )
                    }
                    response.close()
                } catch (e: Exception) { }
            }

            _uiState.value = _uiState.value.copy(isRunning = false, progress = 1f)
            auditRepository.getSessionById(sessionId)?.let {
                auditRepository.updateSession(it.copy(status = "FINISHED", finishedAt = System.currentTimeMillis()))
            }
        }
    }
}

class WebScannerViewModelFactory(private val repository: AuditRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return WebScannerViewModel(repository) as T
    }
}
