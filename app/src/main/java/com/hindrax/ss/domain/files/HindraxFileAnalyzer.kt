package com.hindrax.ss.domain.files

enum class FileCategory {
    ANDROID_PACKAGE,
    ARCHIVE,
    DOCUMENT,
    IMAGE,
    SCRIPT,
    EXECUTABLE,
    DATABASE,
    TEXT,
    UNKNOWN
}

enum class FileRiskFlag {
    ANDROID_PACKAGE,
    ARCHIVE_CONTAINER,
    EXECUTABLE,
    SCRIPT,
    MIME_EXTENSION_MISMATCH,
    UNKNOWN_BINARY
}

data class HindraxFileAnalysis(
    val fileName: String,
    val extension: String,
    val mimeType: String,
    val sizeBytes: Long,
    val sha256: String,
    val magicHex: String,
    val signature: String,
    val category: FileCategory,
    val flags: List<FileRiskFlag>,
    val isLikelyText: Boolean,
    val preview: String,
    val recommendation: String
)

object HindraxFileAnalyzer {
    fun analyzeSnapshot(
        fileName: String,
        mimeType: String?,
        sizeBytes: Long,
        sha256: String,
        headBytes: ByteArray,
        sampleBytes: ByteArray
    ): HindraxFileAnalysis {
        val normalizedName = fileName.ifBlank { "unknown.bin" }
        val extension = normalizedName.substringAfterLast('.', "").lowercase()
        val signature = detectSignature(headBytes, extension)
        val isText = isLikelyText(sampleBytes)
        val category = categorize(extension, mimeType.orEmpty(), signature, isText)
        val flags = buildFlags(extension, mimeType.orEmpty(), signature, category, isText)

        return HindraxFileAnalysis(
            fileName = normalizedName,
            extension = extension.ifBlank { "none" },
            mimeType = mimeType?.takeIf { it.isNotBlank() } ?: "application/octet-stream",
            sizeBytes = sizeBytes,
            sha256 = sha256,
            magicHex = headBytes.take(16).joinToString(" ") { "%02X".format(it) },
            signature = signature,
            category = category,
            flags = flags,
            isLikelyText = isText,
            preview = buildPreview(sampleBytes, isText),
            recommendation = recommendation(category, flags)
        )
    }

    private fun detectSignature(headBytes: ByteArray, extension: String): String {
        return when {
            headBytes.startsWith(0x50, 0x4B, 0x03, 0x04) && extension == "apk" -> "APK/ZIP_CONTAINER"
            headBytes.startsWith(0x50, 0x4B, 0x03, 0x04) -> "ZIP_CONTAINER"
            headBytes.startsWith(0x7F, 0x45, 0x4C, 0x46) -> "ELF_BINARY"
            headBytes.startsWith(0x64, 0x65, 0x78, 0x0A) -> "DEX_BYTECODE"
            headBytes.startsWith(0x25, 0x50, 0x44, 0x46) -> "PDF_DOCUMENT"
            headBytes.startsWith(0x89, 0x50, 0x4E, 0x47) -> "PNG_IMAGE"
            headBytes.startsWith(0xFF, 0xD8, 0xFF) -> "JPEG_IMAGE"
            headBytes.startsWith(0x1F, 0x8B) -> "GZIP_STREAM"
            headBytes.startsWith(0x52, 0x61, 0x72, 0x21) -> "RAR_ARCHIVE"
            headBytes.startsWith(0x37, 0x7A, 0xBC, 0xAF, 0x27, 0x1C) -> "7Z_ARCHIVE"
            headBytes.decodePrefix().startsWith("SQLite format 3") -> "SQLITE_DATABASE"
            headBytes.decodePrefix().startsWith("#!") -> "SCRIPT_SHEBANG"
            else -> "UNKNOWN"
        }
    }

    private fun categorize(extension: String, mimeType: String, signature: String, isText: Boolean): FileCategory {
        return when {
            extension == "apk" || signature == "APK/ZIP_CONTAINER" -> FileCategory.ANDROID_PACKAGE
            signature in setOf("ZIP_CONTAINER", "GZIP_STREAM", "RAR_ARCHIVE", "7Z_ARCHIVE") ||
                extension in setOf("zip", "gz", "tgz", "rar", "7z", "tar") -> FileCategory.ARCHIVE
            signature in setOf("ELF_BINARY", "DEX_BYTECODE") ||
                extension in setOf("so", "elf", "dex", "bin") -> FileCategory.EXECUTABLE
            signature == "SCRIPT_SHEBANG" ||
                extension in setOf("sh", "bash", "py", "rb", "pl", "js", "ps1") -> FileCategory.SCRIPT
            signature in setOf("PNG_IMAGE", "JPEG_IMAGE") || mimeType.startsWith("image/") -> FileCategory.IMAGE
            signature == "PDF_DOCUMENT" || mimeType.startsWith("application/pdf") ||
                extension in setOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx") -> FileCategory.DOCUMENT
            signature == "SQLITE_DATABASE" || extension in setOf("db", "sqlite", "sqlite3") -> FileCategory.DATABASE
            isText || mimeType.startsWith("text/") || extension in setOf("txt", "md", "json", "xml", "csv", "log") -> FileCategory.TEXT
            else -> FileCategory.UNKNOWN
        }
    }

    private fun buildFlags(
        extension: String,
        mimeType: String,
        signature: String,
        category: FileCategory,
        isText: Boolean
    ): List<FileRiskFlag> {
        val flags = mutableListOf<FileRiskFlag>()
        if (category == FileCategory.ANDROID_PACKAGE) flags += FileRiskFlag.ANDROID_PACKAGE
        if (category == FileCategory.ARCHIVE || signature.contains("ZIP") || signature.contains("ARCHIVE")) {
            flags += FileRiskFlag.ARCHIVE_CONTAINER
        }
        if (category == FileCategory.EXECUTABLE) flags += FileRiskFlag.EXECUTABLE
        if (category == FileCategory.SCRIPT) flags += FileRiskFlag.SCRIPT
        if (extension == "apk" && mimeType.isNotBlank() && !mimeType.contains("package-archive") && !mimeType.contains("zip")) {
            flags += FileRiskFlag.MIME_EXTENSION_MISMATCH
        }
        if (!isText && category == FileCategory.UNKNOWN) flags += FileRiskFlag.UNKNOWN_BINARY
        return flags.distinct()
    }

    private fun isLikelyText(bytes: ByteArray): Boolean {
        if (bytes.isEmpty()) return false
        val printable = bytes.count { byte ->
            val value = byte.toInt() and 0xFF
            value == 9 || value == 10 || value == 13 || value in 32..126
        }
        return printable.toDouble() / bytes.size.toDouble() >= 0.85
    }

    private fun buildPreview(bytes: ByteArray, isText: Boolean): String {
        if (!isText) return "<binary_preview_disabled>"
        return bytes.decodeToString()
            .replace("\u0000", "")
            .lines()
            .take(8)
            .joinToString("\n")
            .take(1200)
            .ifBlank { "<empty_text_preview>" }
    }

    private fun recommendation(category: FileCategory, flags: List<FileRiskFlag>): String {
        return when {
            flags.contains(FileRiskFlag.ANDROID_PACKAGE) -> "Route to APK_DEEP_ANALYSIS before installing or sharing."
            flags.contains(FileRiskFlag.EXECUTABLE) -> "Inspect in lab/Termux before execution."
            flags.contains(FileRiskFlag.SCRIPT) -> "Review commands before running."
            category == FileCategory.ARCHIVE -> "Extract only inside a controlled workspace."
            flags.contains(FileRiskFlag.UNKNOWN_BINARY) -> "Treat as unknown binary and verify origin."
            else -> "Low-risk static inspection completed."
        }
    }

    private fun ByteArray.startsWith(vararg prefix: Int): Boolean {
        if (size < prefix.size) return false
        return prefix.indices.all { index -> (this[index].toInt() and 0xFF) == prefix[index] }
    }

    private fun ByteArray.decodePrefix(): String {
        return take(32)
            .map { it.toInt().toChar() }
            .joinToString("")
    }
}
