package com.hindrax.ss.domain.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class SyncStatusLabelTest {
    @Test
    fun pairedIdleNodeIsReadyToSync() {
        val label = SyncStatusLabel.forDevice(isPaired = true, state = NodeSyncState.IDLE)

        assertEquals("LINKED_READY: tareas+inventario", label)
    }

    @Test
    fun inventorySyncShowsDedicatedState() {
        val label = SyncStatusLabel.forDevice(isPaired = true, state = NodeSyncState.SYNCING_INVENTORY)

        assertEquals("SYNCING_INVENTORY...", label)
    }

    @Test
    fun unpairedNodeShowsPairingNeeded() {
        val label = SyncStatusLabel.forDevice(isPaired = false, state = NodeSyncState.IDLE)

        assertEquals("PAIRING_REQUIRED", label)
    }
}
