package com.hindrax.ss.domain.nfc

/**
 * Minimal NFC Forum Type 4 Tag APDU processor for a lab-only NDEF text profile.
 *
 * It serves the public NFC Forum NDEF tag application and never exposes or
 * clones a card UID, payment AID, transit AID, or access-control credential.
 */
class NfcHceEmulator(
    payloadText: String,
    private val enabled: Boolean = true
) {
    private var selectedFile: SelectedFile? = null
    private val ndefFile = buildNdefFile(payloadText)

    fun process(command: ByteArray?): ByteArray {
        if (!enabled) return STATUS_SECURITY_NOT_SATISFIED
        if (command == null || command.size < MIN_APDU_SIZE) return STATUS_WRONG_LENGTH

        val instruction = command[1].unsigned()
        return when (instruction) {
            INS_SELECT -> processSelect(command)
            INS_READ_BINARY -> processReadBinary(command)
            else -> STATUS_INS_NOT_SUPPORTED
        }
    }

    private fun processSelect(command: ByteArray): ByteArray {
        if (command.size < 5) return STATUS_WRONG_LENGTH

        val p1 = command[2].unsigned()
        val p2 = command[3].unsigned()
        val lc = command[4].unsigned()
        if (command.size < 5 + lc) return STATUS_WRONG_LENGTH
        val data = command.copyOfRange(5, 5 + lc)

        return when {
            p1 == SELECT_BY_NAME && data.contentEquals(NDEF_TAG_APPLICATION) -> {
                selectedFile = null
                STATUS_OK
            }
            p1 == SELECT_BY_FILE_ID && p2 == SELECT_FIRST_OR_ONLY && data.contentEquals(CC_FILE_ID) -> {
                selectedFile = SelectedFile.CAPABILITY_CONTAINER
                STATUS_OK
            }
            p1 == SELECT_BY_FILE_ID && p2 == SELECT_FIRST_OR_ONLY && data.contentEquals(NDEF_FILE_ID) -> {
                selectedFile = SelectedFile.NDEF
                STATUS_OK
            }
            else -> STATUS_FILE_NOT_FOUND
        }
    }

    private fun processReadBinary(command: ByteArray): ByteArray {
        if (command.size < 5) return STATUS_WRONG_LENGTH
        val file = when (selectedFile) {
            SelectedFile.CAPABILITY_CONTAINER -> CAPABILITY_CONTAINER
            SelectedFile.NDEF -> ndefFile
            null -> return STATUS_FILE_NOT_FOUND
        }
        val offset = (command[2].unsigned() shl 8) or command[3].unsigned()
        val requestedLength = command[4].unsigned().let { if (it == 0) 256 else it }
        if (offset > file.size) return STATUS_WRONG_PARAMETERS

        val end = minOf(file.size, offset + requestedLength)
        return file.copyOfRange(offset, end) + STATUS_OK
    }

    private enum class SelectedFile {
        CAPABILITY_CONTAINER,
        NDEF
    }

    companion object {
        val STATUS_OK = byteArrayOf(0x90.toByte(), 0x00)
        val STATUS_FILE_NOT_FOUND = byteArrayOf(0x6A, 0x82.toByte())
        val STATUS_WRONG_PARAMETERS = byteArrayOf(0x6A, 0x86.toByte())
        val STATUS_WRONG_LENGTH = byteArrayOf(0x67, 0x00)
        val STATUS_SECURITY_NOT_SATISFIED = byteArrayOf(0x69, 0x85.toByte())
        val STATUS_INS_NOT_SUPPORTED = byteArrayOf(0x6D, 0x00)

        const val DEFAULT_PAYLOAD = "hindrax://lab/demo"
        const val MAX_PAYLOAD_BYTES = 220

        private const val MIN_APDU_SIZE = 4
        private const val INS_SELECT = 0xA4
        private const val INS_READ_BINARY = 0xB0
        private const val SELECT_BY_NAME = 0x04
        private const val SELECT_BY_FILE_ID = 0x00
        private const val SELECT_FIRST_OR_ONLY = 0x0C

        private val NDEF_TAG_APPLICATION = hex("D2760000850101")
        private val CC_FILE_ID = hex("E103")
        private val NDEF_FILE_ID = hex("E104")
        private val CAPABILITY_CONTAINER = hex("000F20003B00340406E10400FF00FF")

        fun normalizePayload(value: String): String {
            return value.trim()
                .takeIf { it.isNotBlank() }
                ?.byteTrim(MAX_PAYLOAD_BYTES)
                ?: DEFAULT_PAYLOAD
        }

        private fun buildNdefFile(payloadText: String): ByteArray {
            val textBytes = normalizePayload(payloadText).toByteArray(Charsets.UTF_8)
            val languageBytes = "en".toByteArray(Charsets.US_ASCII)
            val payload = byteArrayOf(languageBytes.size.toByte()) + languageBytes + textBytes
            val record = byteArrayOf(
                0xD1.toByte(),
                0x01,
                payload.size.toByte(),
                0x54
            ) + payload
            return byteArrayOf(
                ((record.size ushr 8) and 0xFF).toByte(),
                (record.size and 0xFF).toByte()
            ) + record
        }

        private fun String.byteTrim(maxBytes: Int): String {
            var candidate = this
            while (candidate.toByteArray(Charsets.UTF_8).size > maxBytes) {
                candidate = candidate.dropLast(1)
            }
            return candidate
        }

        private fun hex(value: String): ByteArray {
            return value.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        }

        private fun Byte.unsigned(): Int = toInt() and 0xFF
    }
}
