package com.hindrax.ss.features.network

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hindrax.ss.core.model.Severity
import com.hindrax.ss.core.security.TargetParser
import com.hindrax.ss.core.security.ValidationResult
import com.hindrax.ss.domain.usecase.ValidateTargetUseCase
import com.hindrax.ss.data.entity.AuditResultEntity
import com.hindrax.ss.data.entity.AuditSessionEntity
import com.hindrax.ss.data.repository.AuditRepository
import com.hindrax.ss.core.model.AuditTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress

data class NetworkUiState(
    val target: String = "",
    val isRunning: Boolean = false,
    val logs: String = "",
    val lastResult: String? = null,
    val validationMessage: String? = null
)

class NetworkViewModel(
    private val auditRepository: AuditRepository,
    private val validateTargetUseCase: ValidateTargetUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(NetworkUiState())
    val uiState = _uiState.asStateFlow()

    private val pingTask = AuditTask(
        id = "ping",
        name = "Ping",
        category = "Network",
        localOnly = false,
        requiresTermux = false,
        riskLevel = Severity.LOW
    )

    fun onTargetChange(newTarget: String) {
        _uiState.value = _uiState.value.copy(target = newTarget, validationMessage = null)
    }

    fun runPing() {
        val targetValue = _uiState.value.target
        if (targetValue.isBlank()) return

        viewModelScope.launch {
            // Safety Gate Check
            val validation = validateTargetUseCase(targetValue, pingTask)
            if (validation is ValidationResult.Blocked) {
                _uiState.value = _uiState.value.copy(validationMessage = validation.reason)
                return@launch
            } else if (validation is ValidationResult.RequiresAuthorization) {
                _uiState.value = _uiState.value.copy(validationMessage = validation.message)
                return@launch
            }

            _uiState.value = _uiState.value.copy(isRunning = true, logs = "Iniciando Ping a $targetValue...\n", validationMessage = null)
            
            val sessionId = auditRepository.startSession(
                AuditSessionEntity(
                    title = "Ping Audit",
                    taskType = "PING",
                    target = targetValue,
                    targetType = "IP/Domain",
                    authorizationMode = "VERIFIED",
                    status = "RUNNING",
                    startedAt = System.currentTimeMillis()
                )
            )

            val result = withContext(Dispatchers.IO) {
                try {
                    val address = InetAddress.getByName(targetValue)
                    val reachable = address.isReachable(5000)
                    if (reachable) {
                        "Host $targetValue es alcanzable (IP: ${address.hostAddress})"
                    } else {
                        "Host $targetValue no responde al ping (timeout)."
                    }
                } catch (e: Exception) {
                    "Error al ejecutar ping: ${e.message}"
                }
            }

            _uiState.value = _uiState.value.copy(
                isRunning = false,
                logs = _uiState.value.logs + result + "\n",
                lastResult = result
            )

            auditRepository.saveResult(
                AuditResultEntity(
                    sessionId = sessionId,
                    severity = if (result.contains("Error") || result.contains("no responde")) Severity.MEDIUM.name else Severity.INFO.name,
                    category = "Reconocimiento",
                    findingTitle = "Resultado de Ping",
                    findingBody = result
                )
            )

            auditRepository.getSessionById(sessionId)?.let { session ->
                auditRepository.updateSession(session.copy(status = "FINISHED", finishedAt = System.currentTimeMillis()))
            }
        }
    }
}
