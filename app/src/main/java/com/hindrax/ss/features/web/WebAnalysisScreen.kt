package com.hindrax.ss.features.web

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
fun WebAnalysisScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as HindraxApplication
    val viewModel: WebAnalysisViewModel = viewModel(
        factory = WebAnalysisViewModelFactory(app.auditRepository)
    )
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Web Analysis") },
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
            OutlinedTextField(
                value = uiState.url,
                onValueChange = { viewModel.onUrlChange(it) },
                label = { Text("Target URL") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !uiState.isRunning
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.analyzeWeb() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isRunning && uiState.url.startsWith("http")
            ) {
                if (uiState.isRunning) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Analyze Headers")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (uiState.headers.isNotEmpty()) {
                Text("Headers Found:", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    uiState.headers.forEach { (key, value) ->
                        Row(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(text = "$key: ", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            Text(text = value, style = MaterialTheme.typography.bodySmall)
                        }
                        HorizontalDivider()
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Logs", style = MaterialTheme.typography.titleSmall)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(Color.Black)
                    .padding(8.dp)
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

class WebAnalysisViewModelFactory(private val repository: com.hindrax.ss.data.repository.AuditRepository) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return WebAnalysisViewModel(repository) as T
    }
}
