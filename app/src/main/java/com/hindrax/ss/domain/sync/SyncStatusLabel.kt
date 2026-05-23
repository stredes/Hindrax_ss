package com.hindrax.ss.domain.sync

enum class NodeSyncState {
    IDLE,
    PAIRING,
    SYNCING_TASKS,
    SYNCING_INVENTORY,
    SYNCED,
    ERROR
}

object SyncStatusLabel {
    fun forDevice(isPaired: Boolean, state: NodeSyncState): String {
        if (!isPaired && state != NodeSyncState.PAIRING) return "PAIRING_REQUIRED"

        return when (state) {
            NodeSyncState.IDLE -> "LINKED_READY: tareas+inventario"
            NodeSyncState.PAIRING -> "PAIRING_NODE..."
            NodeSyncState.SYNCING_TASKS -> "SYNCING_TASKS..."
            NodeSyncState.SYNCING_INVENTORY -> "SYNCING_INVENTORY..."
            NodeSyncState.SYNCED -> "SYNC_OK: tareas+inventario"
            NodeSyncState.ERROR -> "SYNC_ERROR"
        }
    }
}
