package com.hindrax.ss.features.automation

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
import java.net.InetAddress
import java.util.concurrent.TimeUnit

data class AutomationUiState(
    val target: String = "",
    val isRunning: Boolean = false,
    val currentTask: String = "",
    val logs: String = "",
    val progress: Float = 0f
)

class AutomationViewModel(
    private val auditRepository: AuditRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AutomationUiState())
    val uiState = _uiState.asStateFlow()

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .build()

    fun onTargetChange(newTarget: String) {
        _uiState.value = _uiState.value.copy(target = newTarget)
    }

    fun startQuickAudit() {
        val target = _uiState.value.target.trim()
        if (target.isBlank()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRunning = true, logs = "Iniciando Auditoría Rápida para $target...\n", progress = 0.1f)
            
            val sessionId = auditRepository.startSession(
                AuditSessionEntity(
                    title = "Quick Audit: $target",
                    taskType = "AUTOMATION",
                    target = target,
                    targetType = "DOMAIN/IP",
                    authorizationMode = "VERIFIED",
                    status = "RUNNING",
                    startedAt = System.currentTimeMillis()
                )
            )

            // Task 1: Ping
            runTask("Ping Recon", 0.3f) {
                try {
                    val address = InetAddress.getByName(target)
                    val reachable = address.isReachable(3000)
                    val msg = if (reachable) "Host alcanzable (IP: ${address.hostAddress})" else "Host no responde."
                    saveFinding(sessionId, "Ping", msg, if (reachable) Severity.INFO else Severity.LOW)
                    msg
                } catch (e: Exception) { "Error en Ping: ${e.message}" }
            }

            // Task 2: DNS
            runTask("DNS Resolution", 0.6f) {
                try {
                    val addresses = InetAddress.getAllByName(target)
                    val msg = "IPs encontradas: ${addresses.joinToString { it.hostAddress }}"
                    saveFinding(sessionId, "DNS", msg, Severity.INFO)
                    msg
                } catch (e: Exception) { "Error en DNS: ${e.message}" }
            }

            // Task 3: Web Headers (if looks like a domain or has http)
            if (!target.matches(Regex("^[0-9.]+$"))) {
                runTask("Web Headers", 0.9f) {
                    try {
                        val url = if (target.startsWith("http")) target else "https://$target"
                        val request = Request.Builder().url(url).build()
                        val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
                        val server = response.header("Server") ?: "No detectado"
                        val msg = "Servidor Web: $server. Protocolo: ${response.protocol}"
                        saveFinding(sessionId, "Web Analysis", msg, Severity.INFO)
                        msg
                    } catch (e: Exception) { "Error en Web: ${e.message}" }
                }
            }

            _uiState.value = _uiState.value.copy(
                isRunning = false, 
                progress = 1f, 
                currentTask = "Completado",
                logs = _uiState.value.logs + "\nAuditoría rápida finalizada. Resultados guardados en el historial."
            )

            auditRepository.getSessionById(sessionId)?.let { session ->
                auditRepository.updateSession(session.copy(status = "FINISHED", finishedAt = System.currentTimeMillis()))
            }
        }
    }

    private suspend fun runTask(name: String, progress: Float, block: suspend () -> String) {
        _uiState.value = _uiState.value.copy(currentTask = name, logs = _uiState.value.logs + "[*] Ejecutando $name...\n")
        val result = block()
        _uiState.value = _uiState.value.copy(progress = progress, logs = _uiState.value.logs + "    -> $result\n")
    }

    private suspend fun saveFinding(sessionId: Long, category: String, body: String, severity: Severity) {
        auditRepository.saveResult(
            AuditResultEntity(
                sessionId = sessionId,
                severity = severity.name,
                category = category,
                findingTitle = "Automatización: $category",
                findingBody = body
            )
        )
    }
}

class AutomationViewModelFactory(private val repository: AuditRepository) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return AutomationViewModel(repository) as T
    }
}
