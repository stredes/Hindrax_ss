package com.hindrax.ss.domain.devices

import com.hindrax.ss.data.entity.PeerEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceLocationTest {
    @Test
    fun peerEntityStoresLastGpsLocationForAntiTheftLookup() {
        val peer = PeerEntity(
            id = "HNDX-GPS1",
            name = "Node_GPS1",
            nickname = "Telefono Principal",
            lastKnownIp = "192.168.1.22",
            lastSeen = 10L,
            latitude = -33.4489,
            longitude = -70.6693,
            locationAccuracy = 8.5f,
            locationUpdatedAt = 20L
        )

        assertTrue(peer.hasLocation)
        assertEquals("-33.448900,-70.669300", peer.locationLabel)
    }
}
