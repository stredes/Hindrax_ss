package com.hindrax.ss.domain.usecase

import com.hindrax.ss.core.model.AuditTask
import com.hindrax.ss.core.security.SafetyGate
import com.hindrax.ss.core.security.TargetParser
import com.hindrax.ss.core.security.ValidationResult
import com.hindrax.ss.data.repository.TargetRepository

class ValidateTargetUseCase(private val targetRepository: TargetRepository) {
    suspend operator fun invoke(targetValue: String, task: AuditTask): ValidationResult {
        val target = TargetParser.parse(targetValue) 
            ?: return ValidationResult.Blocked("Formato de objetivo inválido.")
        
        val authorization = targetRepository.getTargetByValue(target.value)
        
        return SafetyGate.validateTask(target, task, authorization)
    }
}
