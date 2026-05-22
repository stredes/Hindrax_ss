package com.hindrax.ss.domain.profile

import org.junit.Assert.assertEquals
import org.junit.Test

class HindraxProfileTest {
    @Test
    fun pairingIdentityEncodesDeviceIdAndNickname() {
        val encoded = HindraxProfileCodec.encodePairingIdentity("HNDX-ABCD", "Mi Telefono")

        assertEquals("HNDX-ABCD|Mi Telefono", encoded)
    }

    @Test
    fun pairingIdentityDecodesLegacyHashOnlyPayload() {
        val decoded = HindraxProfileCodec.decodePairingIdentity("HNDX-ABCD")

        assertEquals("HNDX-ABCD", decoded.deviceId)
        assertEquals(null, decoded.nickname)
    }
}
