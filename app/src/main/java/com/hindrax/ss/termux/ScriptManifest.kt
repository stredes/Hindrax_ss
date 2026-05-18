package com.hindrax.ss.termux

import kotlinx.serialization.Serializable

@Serializable
data class ScriptManifest(
    val id: String,
    val name: String,
    val category: String,
    val script: String,
    val requiresAuthorization: Boolean,
    val allowedTargetTypes: List<String>,
    val riskLevel: String,
    val outputFormat: String
)
