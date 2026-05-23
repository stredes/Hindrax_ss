package com.hindrax.ss.domain.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiHindraxEndpointTest {
    @Test
    fun normalizesBaseUrlWithoutTrailingSlash() {
        assertEquals(
            "https://api-hindrax.vercel.app",
            ApiHindraxEndpoint.normalizeBaseUrl(" https://api-hindrax.vercel.app/ ")
        )
    }

    @Test
    fun validatesHttpsEndpoint() {
        assertTrue(ApiHindraxEndpoint.isValid("https://api-hindrax.vercel.app"))
        assertFalse(ApiHindraxEndpoint.isValid("ftp://api-hindrax.vercel.app"))
    }
}
