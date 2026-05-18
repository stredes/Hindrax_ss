package com.hindrax.ss.features.dns

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
import java.net.InetAddress

data class DnsUiState(
    val domain: String = "",
    val isRunning: Boolean = false,
    val logs: String = "",
    val records: List<String> = emptyList()
)

class DnsViewModel(
    private val auditRepository: AuditRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(DnsUiState())
    val uiState = _uiState.asStateFlow()

    fun onDomainChange(newDomain: String) {
        _uiState.value = _uiState.value.copy(domain = newDomain)
    }

    fun resolveDns() {
        val domain = _uiState.value.domain
        if (domain.isBlank()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRunning = true, logs = "Resolviendo DNS para $domain...\n")
            
            val sessionId = auditRepository.startSession(
                AuditSessionEntity(
                    title = "DNS Lookup",
                    taskType = "DNS",
                    target = domain,
                    targetType = "DOMAIN",
                    authorizationMode = "MANUAL",
                    status = "RUNNING",
                    startedAt = System.currentTimeMillis()
                )
            )

            val results = withContext(Dispatchers.IO) {
                try {
                    val addresses = InetAddress.getAllByName(domain)
                    addresses.map { "A Record: ${it.hostAddress}" }
                } catch (e: Exception) {
                    listOf("Error: ${e.message}")
                }
            }

            _uiState.value = _uiState.value.copy(
                isRunning = false,
                logs = _uiState.value.logs + "Encontrados ${results.size} registros.\n",
                records = results
            )

            results.forEach { record ->
                auditRepository.saveResult(
                    AuditResultEntity(
                        sessionId = sessionId,
                        severity = Severity.INFO.name,
                        category = "DNS",
                        findingTitle = "Registro DNS",
                        findingBody = record
                    )
                )
            }

            val session = auditRepository.getSessionById(sessionId)
            if (session != null) {
                auditRepository.updateSession(session.copy(status = "FINISHED", finishedAt = System.currentTimeMillis()))
            }
        }
    }
}
