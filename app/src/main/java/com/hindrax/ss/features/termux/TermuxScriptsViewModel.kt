package com.hindrax.ss.features.termux

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hindrax.ss.core.model.Severity
import com.hindrax.ss.data.entity.AuditSessionEntity
import com.hindrax.ss.data.repository.AuditRepository
import com.hindrax.ss.termux.TermuxBridge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TermuxScriptsUiState(
    val isTermuxInstalled: Boolean = false,
    val logs: String = "",
    val availableScripts: List<TermuxScriptItem> = listOf(
        TermuxScriptItem("network_ping.sh", "Advanced Ping", "Ejecuta un ping avanzado con parámetros de red."),
        TermuxScriptItem("nmap_scan.sh", "Nmap Scan", "Escaneo de puertos detallado usando Nmap en Termux."),
        TermuxScriptItem("banner_grab.sh", "Banner Grabbing", "Obtiene banners de servicios para identificar versiones.")
    )
)

data class TermuxScriptItem(
    val fileName: String,
    val displayName: String,
    val description: String
)

class TermuxScriptsViewModel(
    private val auditRepository: AuditRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(TermuxScriptsUiState())
    val uiState = _uiState.asStateFlow()

    fun checkTermux(context: Context) {
        _uiState.value = _uiState.value.copy(
            isTermuxInstalled = TermuxBridge.isTermuxInstalled(context)
        )
    }

    fun executeScript(context: Context, scriptName: String, target: String) {
        if (target.isBlank()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(logs = _uiState.value.logs + "Enviando comando a Termux: $scriptName sobre $target...\n")
            
            // Create a session for history
            auditRepository.startSession(
                AuditSessionEntity(
                    title = "Termux: $scriptName",
                    taskType = "TERMUX_EXEC",
                    target = target,
                    targetType = "REMOTE_HOST",
                    authorizationMode = "TERMUX_BRIDGE",
                    status = "SENT_TO_TERMUX",
                    startedAt = System.currentTimeMillis()
                )
            )

            TermuxBridge.executeScript(context, scriptName, arrayOf(target))
            
            _uiState.value = _uiState.value.copy(
                logs = _uiState.value.logs + "Comando enviado. Revisa la terminal de Termux para ver la salida en tiempo real.\n"
            )
        }
    }
}

class TermuxScriptsViewModelFactory(private val repository: AuditRepository) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return TermuxScriptsViewModel(repository) as T
    }
}
