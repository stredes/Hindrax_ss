package com.hindrax.ss.termux

import com.hindrax.ss.core.model.Target
import com.hindrax.ss.core.model.TargetType
import kotlinx.serialization.json.Json
import java.io.File

object ScriptManifestValidator {
    private val json = Json { ignoreUnknownKeys = true }

    fun validate(manifestContent: String, target: Target): ValidationResult {
        return try {
            val manifest = json.decodeFromString<ScriptManifest>(manifestContent)
            
            // 1. Validar tipo de objetivo
            val targetTypeName = when (target.type) {
                is TargetType.PrivateIP -> "PRIVATE_IP"
                is TargetType.PublicIP -> "PUBLIC_IP"
                is TargetType.AuthorizedDomain -> "AUTHORIZED_DOMAIN"
                is TargetType.LocalFile -> "LOCAL_FILE"
                is TargetType.APK -> "APK"
                else -> "UNKNOWN"
            }

            if (!manifest.allowedTargetTypes.contains(targetTypeName)) {
                return ValidationResult.Invalid("El script '${manifest.name}' no está permitido para objetivos de tipo $targetTypeName.")
            }

            // 2. Validar nivel de riesgo vs autorización (Simulado)
            if (manifest.riskLevel == "HIGH" && target.isPublic()) {
                return ValidationResult.Invalid("Tareas de alto riesgo en objetivos públicos requieren una validación manual adicional.")
            }

            ValidationResult.Valid(manifest)
        } catch (e: Exception) {
            ValidationResult.Error("Error al procesar el manifiesto del script: ${e.message}")
        }
    }

    sealed class ValidationResult {
        data class Valid(val manifest: ScriptManifest) : ValidationResult()
        data class Invalid(val reason: String) : ValidationResult()
        data class Error(val message: String) : ValidationResult()
    }
}
