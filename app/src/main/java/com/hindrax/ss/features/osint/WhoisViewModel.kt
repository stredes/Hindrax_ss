package com.hindrax.ss.features.osint

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
import org.json.JSONObject

data class WhoisUiState(
    val domain: String = "",
    val isRunning: Boolean = false,
    val logs: String = "",
    val rawData: String = ""
)

class WhoisViewModel(
    private val auditRepository: AuditRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(WhoisUiState())
    val uiState = _uiState.asStateFlow()

    private val client = OkHttpClient()

    fun onDomainChange(newDomain: String) {
        _uiState.value = _uiState.value.copy(domain = newDomain)
    }

    fun startWhoisLookup() {
        val domain = _uiState.value.domain.trim()
        if (domain.isBlank()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRunning = true, logs = "Consultando WHOIS para $domain...\n")
            
            val sessionId = auditRepository.startSession(
                AuditSessionEntity(
                    title = "WHOIS Lookup: $domain",
                    taskType = "WHOIS",
                    target = domain,
                    targetType = "DOMAIN",
                    authorizationMode = "OPEN_SOURCE",
                    status = "RUNNING",
                    startedAt = System.currentTimeMillis()
                )
            )

            try {
                // Usando rdap.org como proxy gratuito de RDAP (sucesor de WHOIS)
                val url = "https://rdap.org/domain/$domain"
                val request = Request.Builder().url(url).build()
                
                val result = withContext(Dispatchers.IO) {
                    val response = client.newCall(request).execute()
                    if (response.code == 404) return@withContext "Dominio no encontrado."
                    response.body?.string() ?: "Sin respuesta."
                }

                _uiState.value = _uiState.value.copy(
                    isRunning = false,
                    rawData = result,
                    logs = _uiState.value.logs + "Consulta completada.\n"
                )

                auditRepository.saveResult(
                    AuditResultEntity(
                        sessionId = sessionId,
                        severity = Severity.INFO.name,
                        category = "OSINT",
                        findingTitle = "Datos de Registro (WHOIS/RDAP)",
                        findingBody = "Información obtenida satisfactoriamente.",
                        evidence = result.take(2000)
                    )
                )

                auditRepository.getSessionById(sessionId)?.let { session ->
                    auditRepository.updateSession(session.copy(status = "FINISHED", finishedAt = System.currentTimeMillis()))
                }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isRunning = false, logs = "Error: ${e.message}\n")
            }
        }
    }
}

class WhoisViewModelFactory(private val repository: AuditRepository) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return WhoisViewModel(repository) as T
    }
}
