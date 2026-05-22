package com.hindrax.ss.domain.tasks

import com.hindrax.ss.data.entity.PeerEntity
import com.hindrax.ss.data.entity.TaskEntity
import com.hindrax.ss.domain.tasks.model.TaskStatus
import com.hindrax.ss.domain.tasks.model.TaskType
import com.hindrax.ss.data.tasks.repository.toDomain
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskDeviceAssignmentTest {
    @Test
    fun taskEntityPreservesAssignedDeviceInDomainModel() {
        val entity = TaskEntity(
            title = "Sync inventory",
            description = "Keep stock aligned",
            status = TaskStatus.PENDIENTE,
            type = TaskType.INVENTORY,
            assignedPeerId = "HNDX-ABCD",
            createdAt = 1L,
            updatedAt = 2L
        )

        val task = entity.toDomain()

        assertEquals("HNDX-ABCD", task.assignedPeerId)
    }

    @Test
    fun peerDisplayNamePrefersUserNicknameAfterPairing() {
        val peer = PeerEntity(
            id = "HNDX-1234",
            name = "Node_1234",
            nickname = "Tablet Taller",
            lastKnownIp = "192.168.1.50",
            lastSeen = 1L
        )

        assertEquals("Tablet Taller", peer.displayName)
    }
}
