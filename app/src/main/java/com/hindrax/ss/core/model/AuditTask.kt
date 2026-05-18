package com.hindrax.ss.core.model

data class AuditTask(
    val id: String,
    val name: String,
    val category: String,
    val localOnly: Boolean,
    val requiresTermux: Boolean,
    val riskLevel: Severity,
    val scriptName: String? = null
)
