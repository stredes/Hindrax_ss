package com.hindrax.ss.domain.sync

object ApiHindraxStatusLabel {
    fun status(enabled: Boolean, baseUrl: String, token: String): String {
        if (!enabled) return "DISABLED"
        return if (ApiHindraxEndpoint.isValid(baseUrl) && token.isNotBlank()) {
            "ONLINE"
        } else {
            "CONFIG_PENDING"
        }
    }
}
