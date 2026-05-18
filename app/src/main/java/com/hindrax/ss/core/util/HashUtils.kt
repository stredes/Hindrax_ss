package com.hindrax.ss.core.util

import java.io.File
import java.security.MessageDigest

object HashUtils {
    fun calculateSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val inputStream = file.inputStream()
        val buffer = ByteArray(8192)
        var bytesRead: Int
        try {
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        } finally {
            inputStream.close()
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
