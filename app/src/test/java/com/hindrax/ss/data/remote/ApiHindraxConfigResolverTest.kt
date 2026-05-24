package com.hindrax.ss.data.remote

import com.hindrax.ss.domain.sync.ApiHindraxReleaseDefaults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiHindraxConfigResolverTest {
    @Test
    fun readyReleaseDefaultsRepairOldDisabledEmptyPrefs() {
        val config = ApiHindraxConfigResolver.resolve(
            stored = ApiHindraxStoredConfig(
                hasEnabled = true,
                enabled = false,
                baseUrl = null,
                token = null
            ),
            releaseDefaults = ApiHindraxReleaseDefaults(
                enabled = true,
                baseUrl = "https://api-hindrax.vercel.app/",
                token = "release-token"
            )
        )

        assertTrue(config.enabled)
        assertEquals("https://api-hindrax.vercel.app", config.baseUrl)
        assertEquals("release-token", config.token)
        assertTrue(config.isReady)
    }

    @Test
    fun explicitDisabledWithCustomTokenIsRespected() {
        val config = ApiHindraxConfigResolver.resolve(
            stored = ApiHindraxStoredConfig(
                hasEnabled = true,
                enabled = false,
                baseUrl = "https://custom.example",
                token = "custom-token"
            ),
            releaseDefaults = ApiHindraxReleaseDefaults(
                enabled = true,
                baseUrl = "https://api-hindrax.vercel.app",
                token = "release-token"
            )
        )

        assertEquals(false, config.enabled)
        assertEquals("https://custom.example", config.baseUrl)
        assertEquals("custom-token", config.token)
    }
}
