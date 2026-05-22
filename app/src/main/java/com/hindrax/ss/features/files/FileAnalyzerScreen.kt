package com.hindrax.ss.features.files

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hindrax.ss.domain.files.HindraxFileAnalysis

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileAnalyzerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel: FileAnalyzerViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.analyzeUri(context, it) }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("FILE_ANALYZER", fontFamily = FontFamily.Monospace) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Green)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF050505),
                    titleContentColor = Color.Green
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFF050505))
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = FILE_ANALYZER_ASCII,
                color = Color.Green,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                lineHeight = 11.sp,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { launcher.launch(arrayOf("*/*")) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !uiState.isRunning,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Green, contentColor = Color.Black),
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("QUICK_SELECT_FILE", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }

            when {
                uiState.isRunning -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .border(BorderStroke(1.dp, Color.DarkGray)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.Green)
                    }
                }
                uiState.error != null -> {
                    TerminalPanel(title = "--- ERROR ---", accent = Color.Red) {
                        Text(
                            text = uiState.error.orEmpty(),
                            color = Color.Red,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                }
                uiState.analysis != null -> {
                    FileAnalysisReport(uiState.analysis!!)
                }
                else -> {
                    TerminalPanel(title = "--- WAITING_FOR_FILE ---", accent = Color.DarkGray) {
                        Text(
                            text = "SELECT_ANY_FILE_TO_ANALYZE_HASH_MAGIC_MIME_AND_PREVIEW",
                            color = Color.Gray,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FileAnalysisReport(analysis: HindraxFileAnalysis) {
    TerminalPanel(title = "--- FILE_SUMMARY ---", accent = Color.Green) {
        TerminalRow("NAME", analysis.fileName)
        TerminalRow("TYPE", "${analysis.category} / ${analysis.signature}")
        TerminalRow("MIME", analysis.mimeType)
        TerminalRow("SIZE", formatBytes(analysis.sizeBytes))
        TerminalRow("EXT", analysis.extension)
    }

    TerminalPanel(title = "--- HASH_MAGIC ---", accent = Color.Cyan) {
        TerminalRow("SHA256", analysis.sha256)
        TerminalRow("MAGIC", analysis.magicHex.ifBlank { "EMPTY" })
        TerminalRow("TEXT", if (analysis.isLikelyText) "LIKELY_TEXT" else "BINARY_OR_MIXED")
    }

    TerminalPanel(title = "--- FLAGS ---", accent = if (analysis.flags.isEmpty()) Color.Green else Color.Yellow) {
        val flags = analysis.flags.joinToString(" / ").ifBlank { "NO_FLAGS" }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Security, contentDescription = null, tint = if (analysis.flags.isEmpty()) Color.Green else Color.Yellow, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(flags, color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))
        TerminalRow("ACTION", analysis.recommendation)
    }

    TerminalPanel(title = "--- PREVIEW ---", accent = Color.DarkGray) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.InsertDriveFile, contentDescription = null, tint = Color.Green, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = analysis.preview,
                color = if (analysis.isLikelyText) Color.Green else Color.Gray,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                lineHeight = 12.sp
            )
        }
    }
}

@Composable
private fun TerminalPanel(
    title: String,
    accent: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
        border = BorderStroke(1.dp, accent),
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Memory, contentDescription = null, tint = accent, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, color = accent, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            content()
        }
    }
}

@Composable
private fun TerminalRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(value, color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 13.sp)
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 0) return "UNKNOWN"
    val units = listOf("B", "KB", "MB", "GB")
    var value = bytes.toDouble()
    var unitIndex = 0
    while (value >= 1024 && unitIndex < units.lastIndex) {
        value /= 1024
        unitIndex++
    }
    return if (unitIndex == 0) "${bytes}B" else "%.2f%s".format(value, units[unitIndex])
}

private const val FILE_ANALYZER_ASCII = """
+--[ HINDRAX FILE ANALYZER ]--+
| FAST_PICK  HASH  MAGIC  MIME |
| STATIC_SCAN ASCII_NEON_MODE  |
+------------------------------+
"""
