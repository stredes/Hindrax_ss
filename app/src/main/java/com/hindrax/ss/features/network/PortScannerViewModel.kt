package com.hindrax.ss.features.network

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.hindrax.ss.core.work.AuditWorker
import com.hindrax.ss.data.entity.AuditSessionEntity
import com.hindrax.ss.data.repository.AuditRepository
import com.hindrax.ss.domain.tools.NetworkToolSuggestions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class PortScannerUiState(
    val target: String = "",
    val selectedProfileId: String = NetworkToolSuggestions.defaultPortProfile.id,
    val ports: String = NetworkToolSuggestions.portCsv(NetworkToolSuggestions.defaultPortProfile.id),
    val isRunning: Boolean = false,
    val logs: String = "",
    val workId: UUID? = null
)

class PortScannerViewModel(
    private val auditRepository: AuditRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(PortScannerUiState())
    val uiState = _uiState.asStateFlow()

    fun onTargetChange(newTarget: String) {
        _uiState.value = _uiState.value.copy(target = newTarget)
    }

    fun onPortsChange(newPorts: String) {
        _uiState.value = _uiState.value.copy(ports = newPorts, selectedProfileId = "custom")
    }

    fun selectPortProfile(profileId: String) {
        _uiState.value = _uiState.value.copy(
            selectedProfileId = profileId,
            ports = NetworkToolSuggestions.portCsv(profileId)
        )
    }

    fun startScan(context: Context) {
        val target = _uiState.value.target
        val ports = NetworkToolSuggestions.parsePorts(_uiState.value.ports)
        if (target.isBlank()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isRunning = true,
                logs = "Iniciando escaneo en segundo plano...\nPuertos: ${ports.joinToString(", ")}\n"
            )
            
            val sessionId = auditRepository.startSession(
                AuditSessionEntity(
                    title = "Background Port Scan",
                    taskType = "PORT_SCAN",
                    target = target,
                    targetType = "IP/DOMAIN",
                    authorizationMode = "VERIFIED",
                    status = "RUNNING",
                    startedAt = System.currentTimeMillis()
                )
            )

            val workRequest = OneTimeWorkRequestBuilder<AuditWorker>()
                .setInputData(workDataOf(
                    "SESSION_ID" to sessionId,
                    "TARGET" to target,
                    "TASK_TYPE" to "PORT_SCAN",
                    "PORTS" to ports.joinToString(",")
                ))
                .setConstraints(Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build())
                .build()

            WorkManager.getInstance(context).enqueue(workRequest)

            _uiState.value = _uiState.value.copy(
                workId = workRequest.id,
                logs = _uiState.value.logs + "Tarea encolada (ID: ${workRequest.id}). Puedes salir de esta pantalla y el escaneo continuará.\n"
            )
            
            // In a real app, we would observe the work status here or via the database
        }
    }
}
