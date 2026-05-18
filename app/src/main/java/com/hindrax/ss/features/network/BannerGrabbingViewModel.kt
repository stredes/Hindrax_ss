package com.hindrax.ss.features.network

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
import java.net.InetSocketAddress
import java.net.Socket

data class BannerUiState(
    val target: String = "",
    val port: String = "80",
    val isRunning: Boolean = false,
    val logs: String = "",
    val banner: String? = null
)

class BannerGrabbingViewModel(
    private val auditRepository: AuditRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(BannerUiState())
    val uiState = _uiState.asStateFlow()

    fun onTargetChange(newTarget: String) {
        _uiState.value = _uiState.value.copy(target = newTarget)
    }

    fun onPortChange(newPort: String) {
        _uiState.value = _uiState.value.copy(port = newPort)
    }

    fun grabBanner() {
        val target = _uiState.value.target
        val port = _uiState.value.port.toIntOrNull() ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRunning = true, logs = "Conectando a $target:$port para obtener banner...\n", banner = null)
            
            val sessionId = auditRepository.startSession(
                AuditSessionEntity(
                    title = "Banner Grabbing: $port",
                    taskType = "BANNER_GRAB",
                    target = "$target:$port",
                    targetType = "SERVICE",
                    authorizationMode = "VERIFIED",
                    status = "RUNNING",
                    startedAt = System.currentTimeMillis()
                )
            )

            val result = withContext(Dispatchers.IO) {
                try {
                    val socket = Socket()
                    socket.connect(InetSocketAddress(target, port), 3000)
                    socket.soTimeout = 3000
                    
                    // Enviamos un string genérico para provocar respuesta (ej. para HTTP)
                    val out = socket.getOutputStream()
                    out.write("HEAD / HTTP/1.0\r\n\r\n".toByteArray())
                    out.flush()
                    
                    val input = socket.getInputStream()
                    val buffer = ByteArray(1024)
                    val bytesRead = input.read(buffer)
                    socket.close()
                    
                    if (bytesRead != -1) String(buffer, 0, bytesRead).trim() else "No se recibió respuesta del servicio."
                } catch (e: Exception) {
                    "Error de conexión: ${e.message}"
                }
            }

            _uiState.value = _uiState.value.copy(
                isRunning = false,
                logs = _uiState.value.logs + "Respuesta recibida.\n",
                banner = result
            )

            auditRepository.saveResult(
                AuditResultEntity(
                    sessionId = sessionId,
                    severity = Severity.INFO.name,
                    category = "Fingerprinting",
                    findingTitle = "Banner del Servicio ($port)",
                    findingBody = "El servicio en el puerto $port respondió con: $result"
                )
            )

            auditRepository.getSessionById(sessionId)?.let { session ->
                auditRepository.updateSession(session.copy(status = "FINISHED", finishedAt = System.currentTimeMillis()))
            }
        }
    }
}

class BannerGrabbingViewModelFactory(private val repository: AuditRepository) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return BannerGrabbingViewModel(repository) as T
    }
}
