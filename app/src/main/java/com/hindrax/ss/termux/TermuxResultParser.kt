package com.hindrax.ss.termux

import com.hindrax.ss.core.model.Severity
import com.hindrax.ss.data.entity.AuditResultEntity
import org.json.JSONObject

object TermuxResultParser {
    /**
     * Parsea la salida JSON de un script de Termux.
     * El formato esperado del JSON es:
     * {
     *   "title": "Nombre del hallazgo",
     *   "body": "Descripción técnica",
     *   "severity": "LOW|MEDIUM|HIGH|CRITICAL",
     *   "category": "Network|Web|OSINT",
     *   "evidence": "Salida cruda opcional"
     * }
     */
    fun parseJsonResult(sessionId: Long, jsonString: String): AuditResultEntity? {
        return try {
            val json = JSONObject(jsonString)
            AuditResultEntity(
                sessionId = sessionId,
                findingTitle = json.optString("title", "Resultado de Termux"),
                findingBody = json.optString("body", ""),
                severity = json.optString("severity", Severity.INFO.name),
                category = json.optString("category", "General"),
                evidence = json.optString("evidence", null),
                recommendation = json.optString("recommendation", null)
            )
        } catch (e: Exception) {
            null
        }
    }
}
