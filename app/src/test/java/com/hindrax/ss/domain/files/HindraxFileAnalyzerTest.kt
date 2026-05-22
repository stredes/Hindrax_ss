package com.hindrax.ss.domain.files

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HindraxFileAnalyzerTest {
    @Test
    fun detectsApkCompatibleZipPayloads() {
        val analysis = HindraxFileAnalyzer.analyzeSnapshot(
            fileName = "payload.apk",
            mimeType = "application/vnd.android.package-archive",
            sizeBytes = 4096,
            sha256 = "abc123",
            headBytes = byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0x14, 0x00),
            sampleBytes = byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0x14, 0x00)
        )

        assertEquals("APK/ZIP_CONTAINER", analysis.signature)
        assertEquals(FileCategory.ANDROID_PACKAGE, analysis.category)
        assertTrue(analysis.flags.contains(FileRiskFlag.ANDROID_PACKAGE))
    }

    @Test
    fun detectsReadableScriptFiles() {
        val sample = "#!/bin/sh\nwhoami\n".encodeToByteArray()

        val analysis = HindraxFileAnalyzer.analyzeSnapshot(
            fileName = "audit.sh",
            mimeType = "text/x-shellscript",
            sizeBytes = sample.size.toLong(),
            sha256 = "def456",
            headBytes = sample,
            sampleBytes = sample
        )

        assertEquals(FileCategory.SCRIPT, analysis.category)
        assertTrue(analysis.isLikelyText)
        assertTrue(analysis.flags.contains(FileRiskFlag.SCRIPT))
    }
}
