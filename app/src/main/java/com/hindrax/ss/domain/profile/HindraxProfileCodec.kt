package com.hindrax.ss.domain.profile

data class PairingIdentity(
    val deviceId: String,
    val nickname: String?
)

object HindraxProfileCodec {
    fun encodePairingIdentity(deviceId: String, nickname: String?): String {
        val normalized = nickname?.trim()?.takeIf { it.isNotBlank() }
        return if (normalized == null) deviceId else "$deviceId|$normalized"
    }

    fun decodePairingIdentity(payload: String): PairingIdentity {
        val parts = payload.split("|", limit = 2)
        return PairingIdentity(
            deviceId = parts.firstOrNull().orEmpty(),
            nickname = parts.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }
        )
    }
}
