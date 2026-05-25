package com.hindrax.ss.domain.root

import java.security.MessageDigest

object RootAccessPolicy {
    const val CONFIRM_LOCAL_RESET = "RESET_LOCAL"
    const val CONFIRM_FIREBASE_RESET = "RESET_FIREBASE"

    private const val ROOT_KEY_SHA256 =
        "5b8460946a2343a4cc2ef15b058b4d7964c7163863eb809af3f8a1be1c9cfb6f"

    fun isValid(candidate: String): Boolean {
        val normalized = candidate.trim()
        if (normalized.isBlank()) return false
        return sha256(normalized).equals(ROOT_KEY_SHA256, ignoreCase = true)
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return digest.joinToString(separator = "") { byte -> "%02x".format(byte) }
    }
}
