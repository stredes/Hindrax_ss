package com.hindrax.ss.features.osint

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hindrax.ss.core.model.Severity
import com.hindrax.ss.data.entity.AuditResultEntity
import com.hindrax.ss.data.entity.AuditSessionEntity
import com.hindrax.ss.data.repository.AuditRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MetadataUiState(
    val isRunning: Boolean = false,
    val metadata: Map<String, String> = emptyMap(),
    val logs: String = ""
)

class MetadataViewModel(
    private val auditRepository: AuditRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(MetadataUiState())
    val uiState = _uiState.asStateFlow()

    fun analyzeImageMetadata(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRunning = true, logs = "Extrayendo metadatos EXIF...\n")
            
            val sessionId = auditRepository.startSession(
                AuditSessionEntity(
                    title = "Image Metadata Analysis",
                    taskType = "OSINT_METADATA",
                    target = uri.toString(),
                    targetType = "FILE",
                    authorizationMode = "LOCAL",
                    status = "RUNNING",
                    startedAt = System.currentTimeMillis()
                )
            )

            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val exif = ExifInterface(input)
                    val tags = mutableMapOf<String, String>()
                    
                    tags["Model"] = exif.getAttribute(ExifInterface.TAG_MODEL) ?: "N/A"
                    tags["Make"] = exif.getAttribute(ExifInterface.TAG_MAKE) ?: "N/A"
                    tags["Software"] = exif.getAttribute(ExifInterface.TAG_SOFTWARE) ?: "N/A"
                    tags["Date/Time"] = exif.getAttribute(ExifInterface.TAG_DATETIME) ?: "N/A"
                    
                    val latLong = exif.latLong
                    tags["GPS"] = if (latLong != null) "${latLong[0]}, ${latLong[1]}" else "None"

                    _uiState.value = _uiState.value.copy(
                        isRunning = false,
                        metadata = tags,
                        logs = _uiState.value.logs + "Metadatos extraídos con éxito.\n"
                    )

                    auditRepository.saveResult(
                        AuditResultEntity(
                            sessionId = sessionId,
                            severity = if (tags["GPS"] != "None") Severity.MEDIUM.name else Severity.INFO.name,
                            category = "OSINT",
                            findingTitle = "Metadatos EXIF Detectados",
                            findingBody = tags.entries.joinToString("\n") { "${it.key}: ${it.value}" }
                        )
                    )
                }
                
                auditRepository.getSessionById(sessionId)?.let { session ->
                    auditRepository.updateSession(session.copy(status = "FINISHED", finishedAt = System.currentTimeMillis()))
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isRunning = false, logs = _uiState.value.logs + "Error: ${e.message}\n")
            }
        }
    }
}
