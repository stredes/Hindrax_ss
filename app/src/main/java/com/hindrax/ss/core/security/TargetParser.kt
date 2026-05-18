package com.hindrax.ss.core.security

import com.hindrax.ss.core.model.Target
import com.hindrax.ss.core.model.TargetType
import java.util.regex.Pattern

object TargetParser {
    private val IPV4_PATTERN = Pattern.compile(
        "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$"
    )
    private val DOMAIN_PATTERN = Pattern.compile(
        "^([a-z0-9]+(-[a-z0-9]+)*\\.)+[a-z]{2,}$"
    )

    fun parse(targetValue: String): Target? {
        val trimmed = targetValue.trim()
        if (trimmed.isEmpty()) return null

        return when {
            isIpv4(trimmed) -> {
                if (isPrivateIp(trimmed)) {
                    Target(trimmed, TargetType.PrivateIP)
                } else {
                    Target(trimmed, TargetType.PublicIP)
                }
            }
            trimmed.startsWith("/") || trimmed.startsWith("content://") -> {
                if (trimmed.endsWith(".apk")) {
                    Target(trimmed, TargetType.APK)
                } else {
                    Target(trimmed, TargetType.LocalFile)
                }
            }
            isDomain(trimmed) -> {
                Target(trimmed, TargetType.AuthorizedDomain)
            }
            else -> Target(trimmed, TargetType.Unknown)
        }
    }

    private fun isIpv4(value: String): Boolean = IPV4_PATTERN.matcher(value).matches()

    private fun isDomain(value: String): Boolean = DOMAIN_PATTERN.matcher(value.lowercase()).matches()

    private fun isPrivateIp(ip: String): Boolean {
        val parts = ip.split(".").map { it.toInt() }
        return (parts[0] == 10) ||
                (parts[0] == 172 && parts[1] in 16..31) ||
                (parts[0] == 192 && parts[1] == 168) ||
                (parts[0] == 127)
    }
}
