package com.hindrax.ss.domain.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class ApiHindraxStatusLabelTest {
    @Test
    fun disabledWhenSwitchIsOff() {
        assertEquals("DISABLED", ApiHindraxStatusLabel.status(enabled = false, baseUrl = "", token = ""))
    }

    @Test
    fun configPendingWhenEnabledButMissingToken() {
        assertEquals(
            "CONFIG_PENDING",
            ApiHindraxStatusLabel.status(enabled = true, baseUrl = "https://api-hindrax.vercel.app", token = "")
        )
    }

    @Test
    fun onlineWhenEnabledUrlAndTokenAreReady() {
        assertEquals(
            "ONLINE",
            ApiHindraxStatusLabel.status(enabled = true, baseUrl = "https://api-hindrax.vercel.app", token = "secret")
        )
    }
}
