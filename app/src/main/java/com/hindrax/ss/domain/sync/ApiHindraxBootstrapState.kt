package com.hindrax.ss.domain.sync

object ApiHindraxBootstrapState {
    fun shouldRunBootstrap(
        isReady: Boolean,
        baseUrl: String,
        completedBaseUrl: String?
    ): Boolean {
        if (!isReady) return false
        val normalizedBaseUrl = ApiHindraxEndpoint.normalizeBaseUrl(baseUrl)
        val normalizedCompletedBaseUrl = ApiHindraxEndpoint.normalizeBaseUrl(completedBaseUrl.orEmpty())
        return normalizedBaseUrl.isNotBlank() && normalizedBaseUrl != normalizedCompletedBaseUrl
    }
}
