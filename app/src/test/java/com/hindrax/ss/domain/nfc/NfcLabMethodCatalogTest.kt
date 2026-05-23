package com.hindrax.ss.domain.nfc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NfcLabMethodCatalogTest {
    @Test
    fun exposesSafeLabMethodsInOrder() {
        assertEquals(
            listOf(
                NfcLabMethod.READ,
                NfcLabMethod.COPY,
                NfcLabMethod.WRITE,
                NfcLabMethod.EMUL
            ),
            NfcLabMethodCatalog.methods
        )
    }

    @Test
    fun copyRequiresReadableNdefPayload() {
        val emptySnapshot = snapshot(ndefText = null)
        val readableSnapshot = snapshot(ndefText = "hindrax://lab/demo")

        assertFalse(NfcLabMethodCatalog.canCopy(emptySnapshot))
        assertTrue(NfcLabMethodCatalog.canCopy(readableSnapshot))
    }

    @Test
    fun writeRequiresWritableNdefTagAndPayload() {
        val writableSnapshot = snapshot(isWritable = true)
        val readOnlySnapshot = snapshot(isWritable = false)

        assertTrue(NfcLabMethodCatalog.canWrite(writableSnapshot, "hindrax://lab/demo"))
        assertFalse(NfcLabMethodCatalog.canWrite(readOnlySnapshot, "hindrax://lab/demo"))
        assertFalse(NfcLabMethodCatalog.canWrite(writableSnapshot, " "))
    }

    private fun snapshot(
        ndefText: String? = "hindrax://lab/demo",
        isWritable: Boolean? = true
    ) = NfcTagSnapshot(
        tagId = "04:2A:0F:7B",
        maskedTagId = "04:2A:0F:7B",
        technologies = listOf("Ndef"),
        ndefType = "NFC Forum Type 2",
        maxSizeBytes = 256,
        isWritable = isWritable,
        ndefText = ndefText
    )
}
