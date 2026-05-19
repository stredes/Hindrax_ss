package com.hindrax.ss.features.reports

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
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
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri ->
        uri?.let { 
            saveFile(context, it, uiState.reportMarkdown)
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = uiState.session?.title?.uppercase() ?: "SESSION_DETAIL",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 16.sp
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Green)
                    }
                },
                actions = {
                    TextButton(onClick = { 
                        val fileName = "Audit_Report_${uiState.session?.id ?: "export"}.md"
                        exportLauncher.launch(fileName)
                    }) {
                        Text(
                            text = "EXPORT", 
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color.Green
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF050505),
                    titleContentColor = Color.Green
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color(0xFF050505))
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            uiState.session?.let { session ->
                Text(
                    text = "--- SESSION_SUMMARY ---", 
                    style = MaterialTheme.typography.labelSmall, 
                    fontFamily = FontFamily.Monospace, 
                    color = Color.Cyan
                )
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.DarkGray),
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        DetailItem("TARGET", session.target)
                        DetailItem("MODULE", session.taskType)
                        DetailItem("STATUS", session.status, if (session.status == "FINISHED") Color.Green else Color.Yellow)
                    }
                }
            }

            if (uiState.recommendations.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "--- SECURITY_ADVISORIES ---", 
                    style = MaterialTheme.typography.labelSmall, 
                    fontFamily = FontFamily.Monospace, 
                    color = Color.Cyan
                )
                Spacer(modifier = Modifier.height(8.dp))
                uiState.recommendations.forEach { recommendation ->
                    RecommendationItem(recommendation)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "--- REPORT_PREVIEW (MARKDOWN) ---", 
                style = MaterialTheme.typography.labelSmall, 
                fontFamily = FontFamily.Monospace, 
                color = Color.Cyan
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0A0A0A))
                    .border(1.dp, Color.DarkGray)
                    .padding(12.dp)
            ) {
                Text(
                    text = uiState.reportMarkdown,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Green,
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun DetailItem(label: String, value: String, valueColor: Color = Color.White) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            text = "$label: ", 
            fontWeight = FontWeight.Bold, 
            fontFamily = FontFamily.Monospace, 
            fontSize = 12.sp,
            color = Color.Gray
        )
        Text(
            text = value, 
            fontFamily = FontFamily.Monospace, 
            fontSize = 12.sp,
            color = valueColor
        )
    }
}

@Composable
fun RecommendationItem(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF001100)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Green.copy(alpha = 0.3f)),
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color.Yellow, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = Color.White
            )
        }
    }
}

private fun saveFile(context: Context, uri: Uri, content: String) {
    try {
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(content.toByteArray())
        }
        Toast.makeText(context, "Report exported successfully", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
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
