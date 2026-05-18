package com.hindrax.ss.features.automation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
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
fun AutomationScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as HindraxApplication
    val viewModel: AutomationViewModel = viewModel(
        factory = AutomationViewModelFactory(app.auditRepository)
    )
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quick Audit Automation") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Text(
                text = "Ejecuta múltiples tareas de reconocimiento automáticamente sobre un objetivo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = uiState.target,
                onValueChange = { viewModel.onTargetChange(it) },
                label = { Text("Target (Domain or IP)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !uiState.isRunning,
                placeholder = { Text("example.com") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.startQuickAudit() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isRunning && uiState.target.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (uiState.isRunning) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Automation")
                }
            }

            if (uiState.isRunning || uiState.progress > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Task: ${uiState.currentTask}", style = MaterialTheme.typography.labelLarge)
                LinearProgressIndicator(
                    progress = { uiState.progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Automation Logs", style = MaterialTheme.typography.titleSmall)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black)
                    .padding(8.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = uiState.logs,
                    color = Color.Green,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
