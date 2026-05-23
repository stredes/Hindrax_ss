package com.hindrax.ss.domain.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class AutoSyncPolicyTest {
    @Test
    fun refreshIntervalIsThirtySeconds() {
        assertEquals(30L, AutoSyncPolicy.REFRESH_INTERVAL_SECONDS)
        assertEquals(30_000L, AutoSyncPolicy.REFRESH_INTERVAL_MILLIS)
    }
}
