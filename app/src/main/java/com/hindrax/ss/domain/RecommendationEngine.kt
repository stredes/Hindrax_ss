package com.hindrax.ss.domain

import com.hindrax.ss.data.entity.AuditResultEntity

object RecommendationEngine {
    fun getRecommendations(results: List<AuditResultEntity>): List<String> {
        val recommendations = mutableListOf<String>()

        results.forEach { result ->
            when {
                result.findingTitle.contains("Puerto 22") -> {
                    recommendations.add("SSH (22) detectado: Asegúrese de usar autenticación por llaves y desactivar el acceso root por contraseña.")
                }
                result.findingTitle.contains("Puerto 80") -> {
                    recommendations.add("HTTP (80) detectado: Se recomienda migrar a HTTPS (443) para cifrar el tráfico en tránsito.")
                }
                result.findingTitle.contains("Content-Security-Policy") -> {
                    recommendations.add("Falta CSP: Implemente una política de seguridad de contenido para prevenir ataques XSS y de inyección.")
                }
                result.findingTitle.contains("HSTS") || result.findingTitle.contains("Strict-Transport-Security") -> {
                    recommendations.add("Falta HSTS: Habilite HSTS para forzar conexiones seguras y prevenir ataques de degradación (SSL Strip).")
                }
                result.findingTitle.contains("Permisos Sensibles") -> {
                    recommendations.add("Análisis de Privacidad: El APK solicita permisos críticos. Verifique si realmente son necesarios para su funcionalidad mínima.")
                }
            }
        }

        if (recommendations.isEmpty() && results.isNotEmpty()) {
            recommendations.add("No se detectaron riesgos críticos inmediatos, pero se recomienda seguir el principio de mínimo privilegio.")
        }

        return recommendations.distinct()
    }
}
