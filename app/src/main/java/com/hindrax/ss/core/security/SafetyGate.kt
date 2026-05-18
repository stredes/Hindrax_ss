package com.hindrax.ss.core.security

import com.hindrax.ss.core.model.AuditTask
import com.hindrax.ss.core.model.Target
import com.hindrax.ss.data.entity.AllowedTargetEntity

sealed class ValidationResult {
    object Allowed : ValidationResult()
    data class Blocked(val reason: String) : ValidationResult()
    data class RequiresAuthorization(val message: String) : ValidationResult()
}

object SafetyGate {
    fun validateTask(
        target: Target,
        task: AuditTask,
        authorization: AllowedTargetEntity?
    ): ValidationResult {
        
        // 1. Check if task is local-only but target is public
        if (task.localOnly && target.isPublic()) {
            return ValidationResult.Blocked("Esta tarea solo permite objetivos locales o privados.")
        }

        // 2. Check if target is public and has no authorization
        if (target.isPublic() && authorization == null) {
            return ValidationResult.RequiresAuthorization("El objetivo público requiere autorización explícita.")
        }

        // 3. Check if authorization is expired
        if (authorization != null && authorization.expiresAt != null) {
            if (System.currentTimeMillis() > authorization.expiresAt) {
                return ValidationResult.Blocked("La autorización para este objetivo ha expirado.")
            }
        }

        // 4. Specific policy checks (extensible)
        if (!isTaskCompatibleWithTarget(task, target)) {
            return ValidationResult.Blocked("La tarea no es compatible con el tipo de objetivo.")
        }

        return ValidationResult.Allowed
    }

    private fun isTaskCompatibleWithTarget(task: AuditTask, target: Target): Boolean {
        // Simple logic for MVP, can be expanded
        return true 
    }
}
