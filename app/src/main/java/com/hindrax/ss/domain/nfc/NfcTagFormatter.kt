package com.hindrax.ss.domain.nfc

object NfcTagFormatter {
    fun formatId(id: ByteArray): String {
        return id.joinToString(":") { byte -> "%02X".format(byte) }
    }

    fun maskId(formattedId: String): String {
        val parts = formattedId.split(":")
        if (parts.size <= 4) return formattedId
        return "${parts.take(2).joinToString(":")}:...:${parts.takeLast(2).joinToString(":")}"
    }
}

data class NfcTagSnapshot(
    val tagId: String,
    val maskedTagId: String,
    val technologies: List<String>,
    val ndefType: String? = null,
    val maxSizeBytes: Int? = null,
    val isWritable: Boolean? = null
)
