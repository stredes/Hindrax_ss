package com.hindrax.ss.features.files

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hindrax.ss.domain.files.HindraxFileAnalysis
import com.hindrax.ss.domain.files.HindraxFileAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.security.MessageDigest

data class FileAnalyzerUiState(
    val isRunning: Boolean = false,
    val analysis: HindraxFileAnalysis? = null,
    val error: String? = null
)

class FileAnalyzerViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(FileAnalyzerUiState())
    val uiState = _uiState.asStateFlow()

    fun analyzeUri(context: Context, uri: Uri) {
        _uiState.value = FileAnalyzerUiState(isRunning = true)
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    analyzeContentUri(context.applicationContext, uri)
                }
            }

            _uiState.value = result.fold(
                onSuccess = { FileAnalyzerUiState(analysis = it) },
                onFailure = { FileAnalyzerUiState(error = it.message ?: "FILE_ANALYSIS_FAILED") }
            )
        }
    }

    private fun analyzeContentUri(context: Context, uri: Uri): HindraxFileAnalysis {
        val resolver = context.contentResolver
        val metadata = resolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                FileMetadata(
                    name = if (nameIndex >= 0) cursor.getString(nameIndex) else null,
                    size = if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex) else -1L
                )
            } else {
                FileMetadata()
            }
        } ?: FileMetadata()

        val digest = MessageDigest.getInstance("SHA-256")
        val head = ByteArrayOutputStream()
        val sample = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var totalBytes = 0L

        resolver.openInputStream(uri)?.use { input ->
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                digest.update(buffer, 0, read)
                totalBytes += read

                val remainingHead = HEAD_LIMIT - head.size()
                if (remainingHead > 0) {
                    head.write(buffer, 0, minOf(read, remainingHead))
                }

                val remainingSample = SAMPLE_LIMIT - sample.size()
                if (remainingSample > 0) {
                    sample.write(buffer, 0, minOf(read, remainingSample))
                }
            }
        } ?: error("UNABLE_TO_OPEN_FILE")

        val sha256 = digest.digest().joinToString("") { "%02x".format(it) }
        return HindraxFileAnalyzer.analyzeSnapshot(
            fileName = metadata.name ?: uri.lastPathSegment ?: "selected_file.bin",
            mimeType = resolver.getType(uri),
            sizeBytes = metadata.size.takeIf { it >= 0 } ?: totalBytes,
            sha256 = sha256,
            headBytes = head.toByteArray(),
            sampleBytes = sample.toByteArray()
        )
    }

    private data class FileMetadata(
        val name: String? = null,
        val size: Long = -1L
    )

    private companion object {
        const val HEAD_LIMIT = 64
        const val SAMPLE_LIMIT = 4096
    }
}
