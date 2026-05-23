package com.hindrax.ss.domain.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class AutoSyncPolicyTest {
    @Test
    fun refreshIntervalIsOneHundredThirtySeconds() {
        assertEquals(130L, AutoSyncPolicy.REFRESH_INTERVAL_SECONDS)
        assertEquals(130_000L, AutoSyncPolicy.REFRESH_INTERVAL_MILLIS)
    }
}
