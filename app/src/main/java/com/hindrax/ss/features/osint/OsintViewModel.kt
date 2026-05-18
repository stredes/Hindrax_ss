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
import org.json.JSONArray
import java.util.concurrent.TimeUnit

data class OsintUiState(
    val query: String = "",
    val isRunning: Boolean = false,
    val logs: String = "",
    val results: List<String> = emptyList()
)

class OsintViewModel(
    private val auditRepository: AuditRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(OsintUiState())
    val uiState = _uiState.asStateFlow()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    fun onQueryChange(newQuery: String) {
        _uiState.value = _uiState.value.copy(query = newQuery)
    }

    fun startOsintSearch() {
        val query = _uiState.value.query.trim()
        if (query.isBlank()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRunning = true, logs = "Consultando crt.sh para subdominios de $query...\n", results = emptyList())
            
            val sessionId = auditRepository.startSession(
                AuditSessionEntity(
                    title = "OSINT Discovery: $query",
                    taskType = "OSINT",
                    target = query,
                    targetType = "DOMAIN",
                    authorizationMode = "OPEN_SOURCE",
                    status = "RUNNING",
                    startedAt = System.currentTimeMillis()
                )
            )

            try {
                // crt.sh API (JSON)
                val url = "https://crt.sh/?q=$query&output=json"
                val request = Request.Builder().url(url).build()
                
                val subdomains = withContext(Dispatchers.IO) {
                    val response = client.newCall(request).execute()
                    if (!response.isSuccessful) throw Exception("Error en la API de crt.sh: ${response.code}")
                    
                    val jsonData = response.body?.string() ?: "[]"
                    val jsonArray = JSONArray(jsonData)
                    val resultSet = mutableSetOf<String>()
                    
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val nameValue = obj.getString("name_value")
                        // Los nombres pueden tener saltos de línea y comodines
                        nameValue.split("\n").forEach { 
                            resultSet.add(it.trim().lowercase())
                        }
                    }
                    resultSet.toList().sorted()
                }

                _uiState.value = _uiState.value.copy(
                    isRunning = false,
                    logs = _uiState.value.logs + "Recolección completada. ${subdomains.size} registros únicos encontrados.\n",
                    results = subdomains
                )

                auditRepository.saveResult(
                    AuditResultEntity(
                        sessionId = sessionId,
                        severity = Severity.INFO.name,
                        category = "OSINT",
                        findingTitle = "Subdominios Detectados (crt.sh)",
                        findingBody = "Se encontraron ${subdomains.size} subdominios asociados a $query.",
                        evidence = subdomains.take(50).joinToString("\n")
                    )
                )

                auditRepository.getSessionById(sessionId)?.let { session ->
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
}
