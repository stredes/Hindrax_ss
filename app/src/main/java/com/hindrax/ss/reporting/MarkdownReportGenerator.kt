package com.hindrax.ss.reporting

import com.hindrax.ss.data.entity.AuditResultEntity
import com.hindrax.ss.data.entity.AuditSessionEntity
import java.text.SimpleDateFormat
import java.util.*

object MarkdownReportGenerator {
    fun generate(session: AuditSessionEntity, results: List<AuditResultEntity>): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val dateStr = sdf.format(Date(session.startedAt))

        return buildString {
            append("# Informe de Auditoría: ${session.title}\n\n")
            append("## Resumen Ejecutivo\n")
            append("- **Fecha:** $dateStr\n")
            append("- **Objetivo:** ${session.target} (${session.targetType})\n")
            append("- **Tipo de Tarea:** ${session.taskType}\n")
            append("- **Estado:** ${session.status}\n\n")

            if (!session.summary.isNullOrBlank()) {
                append("### Nota del Auditor\n")
                append("${session.summary}\n\n")
            }

            append("## Hallazgos\n\n")
            if (results.isEmpty()) {
                append("No se encontraron hallazgos significativos.\n\n")
            } else {
                results.forEach { result ->
                    append("### [${result.severity}] ${result.findingTitle}\n")
                    append("- **Categoría:** ${result.category}\n")
                    append("- **Descripción:** ${result.findingBody}\n")
                    if (!result.evidence.isNullOrBlank()) {
                        append("- **Evidencia:**\n```\n${result.evidence}\n```\n")
                    }
                    if (!result.recommendation.isNullOrBlank()) {
                        append("- **Recomendación:** ${result.recommendation}\n")
                    }
                    append("\n---\n\n")
                }
            }

            append("\n\n---\n")
            append("> **Descargo de Responsabilidad:** Este informe fue generado por `hindrax_ss` para propósitos de auditoría autorizada. El uso de esta información para fines ilícitos es responsabilidad exclusiva del usuario.\n")
        }
    }
}
