package com.hindrax.ss.domain.nfc

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NfcHceEmulatorTest {
    @Test
    fun servesNdefType4TextPayloadOverApduFlow() {
        val emulator = NfcHceEmulator("hindrax://lab/demo")

        assertArrayEquals(NfcHceEmulator.STATUS_OK, emulator.process(hex("00A4040007D276000085010100")))
        assertArrayEquals(NfcHceEmulator.STATUS_OK, emulator.process(hex("00A4000C02E103")))

        val capabilityContainer = emulator.process(hex("00B000000F"))
        assertEquals("000F20003B00340406E10400FF00FF", capabilityContainer.dropStatus().toHex())
        assertEquals("9000", capabilityContainer.statusHex())

        assertArrayEquals(NfcHceEmulator.STATUS_OK, emulator.process(hex("00A4000C02E104")))

        val header = emulator.process(hex("00B0000002"))
        val ndefLength = ((header[0].toInt() and 0xFF) shl 8) or (header[1].toInt() and 0xFF)
        assertTrue(ndefLength > 0)
        assertEquals("9000", header.statusHex())

        val body = emulator.process(byteArrayOf(0x00, 0xB0.toByte(), 0x00, 0x02, ndefLength.toByte()))
        assertTrue(String(body.dropStatus(), Charsets.ISO_8859_1).contains("hindrax://lab/demo"))
        assertEquals("9000", body.statusHex())
    }

    @Test
    fun rejectsUnknownAidWithoutChangingSelection() {
        val emulator = NfcHceEmulator("hindrax://lab/demo")

        assertArrayEquals(NfcHceEmulator.STATUS_FILE_NOT_FOUND, emulator.process(hex("00A4040002F00100")))
    }

    @Test
    fun disabledProfileReturnsSecurityStatus() {
        val emulator = NfcHceEmulator("hindrax://lab/demo", enabled = false)

        assertArrayEquals(NfcHceEmulator.STATUS_SECURITY_NOT_SATISFIED, emulator.process(hex("00A4040007D276000085010100")))
    }

    private fun ByteArray.dropStatus(): ByteArray = copyOfRange(0, size - 2)

    private fun ByteArray.statusHex(): String = copyOfRange(size - 2, size).toHex()

    private fun ByteArray.toHex(): String = joinToString("") { "%02X".format(it) }

    private fun hex(value: String): ByteArray {
        return value.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }
}
