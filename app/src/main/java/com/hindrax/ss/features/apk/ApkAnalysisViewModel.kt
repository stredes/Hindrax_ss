package com.hindrax.ss.features.apk

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hindrax.ss.core.model.Severity
import com.hindrax.ss.core.util.HashUtils
import com.hindrax.ss.data.entity.AuditResultEntity
import com.hindrax.ss.data.entity.AuditSessionEntity
import com.hindrax.ss.data.repository.AuditRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class ApkUiState(
    val isRunning: Boolean = false,
    val logs: String = "",
    val apkInfo: ApkInfo? = null
)

data class ApkInfo(
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val sha256: String,
    val permissions: List<String> = emptyList()
)

class ApkAnalysisViewModel(
    private val auditRepository: AuditRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ApkUiState())
    val uiState = _uiState.asStateFlow()

    fun analyzeApk(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRunning = true, logs = "Iniciando análisis estático profundo...\n")
            
            val sessionId = auditRepository.startSession(
                AuditSessionEntity(
                    title = "APK Deep Analysis",
                    taskType = "APK_ANALYSIS",
                    target = uri.toString(),
                    targetType = "APK",
                    authorizationMode = "LOCAL",
                    status = "RUNNING",
                    startedAt = System.currentTimeMillis()
                )
            )

            try {
                val info = withContext(Dispatchers.IO) {
                    val tempFile = File(context.cacheDir, "temp_analysis.apk")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                        }
                    }

                    val pm = context.packageManager
                    // Solicitamos permisos también
                    val packageInfo = pm.getPackageArchiveInfo(tempFile.absolutePath, PackageManager.GET_PERMISSIONS)
                        ?: throw Exception("No se pudo leer el manifiesto del APK")
                    
                    val sha256 = HashUtils.calculateSha256(tempFile)
                    val permissions = packageInfo.requestedPermissions?.toList() ?: emptyList()
                    
                    ApkInfo(
                        packageName = packageInfo.packageName,
                        versionName = packageInfo.versionName ?: "N/A",
                        versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                            packageInfo.longVersionCode
                        } else {
                            packageInfo.versionCode.toLong()
                        },
                        sha256 = sha256,
                        permissions = permissions
                    )
                }

                _uiState.value = _uiState.value.copy(
                    isRunning = false,
                    apkInfo = info,
                    logs = _uiState.value.logs + "Análisis completado. Se detectaron ${info.permissions.size} permisos.\n"
                )

                // Guardar hallazgo de seguridad si hay permisos peligrosos
                if (info.permissions.any { it.contains("SMS") || it.contains("LOCATION") || it.contains("CAMERA") }) {
                    auditRepository.saveResult(
                        AuditResultEntity(
                            sessionId = sessionId,
                            severity = Severity.MEDIUM.name,
                            category = "Privacy",
                            findingTitle = "Permisos Sensibles Detectados",
                            findingBody = "El APK solicita acceso a: " + info.permissions.filter { it.contains("SMS") || it.contains("LOCATION") || it.contains("CAMERA") }.joinToString(", ")
                        )
                    )
                }

                auditRepository.saveResult(
                    AuditResultEntity(
                        sessionId = sessionId,
                        severity = Severity.INFO.name,
                        category = "APK Info",
                        findingTitle = "Metadatos y Hash",
                        findingBody = "Package: ${info.packageName}\nSHA-256: ${info.sha256}"
                    )
                )

                auditRepository.getSessionById(sessionId)?.let { session ->
                    auditRepository.updateSession(session.copy(status = "FINISHED", finishedAt = System.currentTimeMillis()))
                }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isRunning = false, logs = _uiState.value.logs + "Error: ${e.message}\n")
            }
        }
    }
}
