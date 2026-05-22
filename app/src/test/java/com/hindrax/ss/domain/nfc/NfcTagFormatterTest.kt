package com.hindrax.ss.domain.nfc

import org.junit.Assert.assertEquals
import org.junit.Test

class NfcTagFormatterTest {
    @Test
    fun formatsTagIdAsUppercaseHexPairs() {
        val id = byteArrayOf(0x04, 0x2A, 0x0F, 0x7B)

        assertEquals("04:2A:0F:7B", NfcTagFormatter.formatId(id))
    }

    @Test
    fun masksTagIdForSafeDisplay() {
        assertEquals("04:2A:...:0F:7B", NfcTagFormatter.maskId("04:2A:9C:01:0F:7B"))
    }
}
