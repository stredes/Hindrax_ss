package com.hindrax.ss.domain.sync

object ApiHindraxEndpoint {
    const val DEFAULT_BASE_URL = "https://api-hindrax.vercel.app"

    fun normalizeBaseUrl(value: String): String {
        return value.trim().trimEnd('/')
    }

    fun isValid(value: String): Boolean {
        val normalized = normalizeBaseUrl(value)
        return normalized.startsWith("https://") || normalized.startsWith("http://")
    }
}

data class ApiHindraxReleaseDefaults(
    val enabled: Boolean,
    val baseUrl: String,
    val token: String
) {
    val normalizedBaseUrl: String = ApiHindraxEndpoint.normalizeBaseUrl(baseUrl)

    val isReady: Boolean
        get() = enabled &&
            normalizedBaseUrl.isNotBlank() &&
            token.isNotBlank() &&
            ApiHindraxEndpoint.isValid(normalizedBaseUrl)
}
