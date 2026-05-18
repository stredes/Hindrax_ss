package com.hindrax.ss.features.network

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BannerGrabbingScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as HindraxApplication
    val viewModel: BannerGrabbingViewModel = viewModel(
        factory = BannerGrabbingViewModelFactory(app.auditRepository)
    )
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Service Fingerprinting") },
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
                text = "Identifica la versión del software de un servicio remoto (Banner Grabbing).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = uiState.target,
                onValueChange = { viewModel.onTargetChange(it) },
                label = { Text("Target IP/Domain") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !uiState.isRunning,
                placeholder = { Text("192.168.1.1") }
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = uiState.port,
                onValueChange = { viewModel.onPortChange(it) },
                label = { Text("Port") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !uiState.isRunning
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.grabBanner() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isRunning && uiState.target.isNotBlank()
            ) {
                if (uiState.isRunning) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Grab Service Banner")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Service Response", style = MaterialTheme.typography.titleMedium)
            
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(8.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Column {
                    if (uiState.banner != null) {
                        Text(
                            text = uiState.banner!!,
                            color = Color.Cyan,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (uiState.logs.isNotEmpty()) {
                        Text(
                            text = "\n" + uiState.logs,
                            color = Color.LightGray,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
