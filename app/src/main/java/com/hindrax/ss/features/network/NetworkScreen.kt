package com.hindrax.ss.features.network

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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hindrax.ss.HindraxApplication
import com.hindrax.ss.domain.usecase.ValidateTargetUseCase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as HindraxApplication
    val viewModel: NetworkViewModel = viewModel(
        factory = NetworkViewModelFactory(
            app.auditRepository,
            ValidateTargetUseCase(app.targetRepository)
        )
    )
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Network Recon (Ping)") },
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
                value = uiState.target,
                onValueChange = { viewModel.onTargetChange(it) },
                label = { Text("Target IP or Domain") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !uiState.isRunning,
                isError = uiState.validationMessage != null,
                supportingText = {
                    uiState.validationMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.runPing() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isRunning && uiState.target.isNotBlank()
            ) {
                if (uiState.isRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Running...")
                } else {
                    Text("Run Ping")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Console Output", style = MaterialTheme.typography.titleMedium)
            
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxSize()
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

class NetworkViewModelFactory(
    private val repository: com.hindrax.ss.data.repository.AuditRepository,
    private val validateTargetUseCase: ValidateTargetUseCase
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return NetworkViewModel(repository, validateTargetUseCase) as T
    }
}
