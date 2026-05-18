package com.hindrax.ss.features.reports

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hindrax.ss.HindraxApplication

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    sessionId: Long,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as HindraxApplication
    val viewModel: SessionDetailViewModel = viewModel(
        factory = SessionDetailViewModelFactory(sessionId, app.auditRepository)
    )
    val uiState by viewModel.uiState.collectAsState()

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri ->
        uri?.let { 
            saveFile(context, it, uiState.reportMarkdown)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.session?.title ?: "Session Detail") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        val fileName = "Audit_Report_${uiState.session?.id ?: "export"}.md"
                        exportLauncher.launch(fileName)
                    }) {
                        Text(text = "Export", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            uiState.session?.let { session ->
                Text(text = "Resumen de Sesión", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Objetivo: ${session.target}", fontWeight = FontWeight.Bold)
                        Text("Módulo: ${session.taskType}")
                        Text("Estado: ${session.status}")
                    }
                }
            }

            if (uiState.recommendations.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Recomendaciones de Seguridad", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                uiState.recommendations.forEach { recommendation ->
                    RecommendationItem(recommendation)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(text = "Vista Previa del Reporte (Markdown)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF5F5F5))
                    .padding(12.dp)
            ) {
                Text(
                    text = uiState.reportMarkdown,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Black
                )
            }
        }
    }
}

@Composable
fun RecommendationItem(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

private fun saveFile(context: Context, uri: Uri, content: String) {
    try {
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(content.toByteArray())
        }
        Toast.makeText(context, "Reporte exportado exitosamente", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Error al exportar reporte: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

class SessionDetailViewModelFactory(
    private val sessionId: Long,
    private val repository: com.hindrax.ss.data.repository.AuditRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return SessionDetailViewModel(sessionId, repository) as T
    }
}
