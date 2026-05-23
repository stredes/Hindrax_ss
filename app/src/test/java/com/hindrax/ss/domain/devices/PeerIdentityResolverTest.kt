package com.hindrax.ss.domain.devices

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PeerIdentityResolverTest {
    @Test
    fun newHashReusesNicknameFromSameKnownAddress() {
        val knownPeers = listOf(
            KnownPeerIdentity(
                id = "HNDX-OLDHASH",
                nickname = "Tablet Cocina",
                lastKnownAddress = "192.168.100.92"
            )
        )

        val match = PeerIdentityResolver.findReusableIdentity(
            deviceId = "HNDX-NEWHASH",
            address = "192.168.100.92",
            knownPeers = knownPeers
        )

        assertEquals("Tablet Cocina", match?.nickname)
        assertEquals("HNDX-OLDHASH", match?.previousId)
    }

    @Test
    fun sameHashIsNotTreatedAsReplacedDevice() {
        val knownPeers = listOf(
            KnownPeerIdentity(
                id = "HNDX-SAME",
                nickname = "Tablet Cocina",
                lastKnownAddress = "192.168.100.92"
            )
        )

        val match = PeerIdentityResolver.findReusableIdentity(
            deviceId = "HNDX-SAME",
            address = "192.168.100.92",
            knownPeers = knownPeers
        )

        assertNull(match)
    }
}
