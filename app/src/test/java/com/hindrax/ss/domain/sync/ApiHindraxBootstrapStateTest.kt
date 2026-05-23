package com.hindrax.ss.domain.sync

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiHindraxBootstrapStateTest {
    @Test
    fun runsBootstrapWhenApiIsReadyAndBaseUrlWasNotCompleted() {
        assertTrue(
            ApiHindraxBootstrapState.shouldRunBootstrap(
                isReady = true,
                baseUrl = "https://api-hindrax.vercel.app/",
                completedBaseUrl = ""
            )
        )
    }

    @Test
    fun skipsBootstrapWhenSameBaseUrlWasCompleted() {
        assertFalse(
            ApiHindraxBootstrapState.shouldRunBootstrap(
                isReady = true,
                baseUrl = "https://api-hindrax.vercel.app/",
                completedBaseUrl = "https://api-hindrax.vercel.app"
            )
        )
    }

    @Test
    fun skipsBootstrapWhenApiIsNotReady() {
        assertFalse(
            ApiHindraxBootstrapState.shouldRunBootstrap(
                isReady = false,
                baseUrl = "https://api-hindrax.vercel.app",
                completedBaseUrl = ""
            )
        )
    }
}
