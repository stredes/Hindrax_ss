package com.hindrax.ss.domain.inventory

import java.util.Locale

object ProductNameNormalizer {
    fun key(value: String): String {
        return value.trim()
            .replace(Regex("\\s+"), " ")
            .lowercase(Locale.ROOT)
    }

    fun displayName(value: String): String {
        return value.trim()
            .replace(Regex("\\s+"), " ")
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                word.lowercase(Locale.ROOT).replaceFirstChar { first ->
                    if (first.isLowerCase()) first.titlecase(Locale.ROOT) else first.toString()
                }
            }
    }
}
