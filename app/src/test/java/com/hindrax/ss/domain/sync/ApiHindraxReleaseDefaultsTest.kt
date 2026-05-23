package com.hindrax.ss.domain.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiHindraxReleaseDefaultsTest {
    @Test
    fun readyWhenReleaseProvidesEnabledUrlAndToken() {
        val defaults = ApiHindraxReleaseDefaults(
            enabled = true,
            baseUrl = "https://api-hindrax.vercel.app/",
            token = "token"
        )

        assertTrue(defaults.isReady)
        assertEquals("https://api-hindrax.vercel.app", defaults.normalizedBaseUrl)
    }

    @Test
    fun notReadyWithoutToken() {
        val defaults = ApiHindraxReleaseDefaults(
            enabled = true,
            baseUrl = "https://api-hindrax.vercel.app",
            token = ""
        )

        assertFalse(defaults.isReady)
    }
}
