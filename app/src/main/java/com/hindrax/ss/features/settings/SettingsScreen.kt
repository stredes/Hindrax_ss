package com.hindrax.ss.features.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: SettingsViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadSettings(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(text = "API Keys (OSINT)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                uiState.apiKeys.forEach { (name, value) ->
                    ApiKeyInput(
                        name = name,
                        value = value,
                        onValueChange = { viewModel.updateApiKey(context, name, it) }
                    )
                }
            }

            item {
                HorizontalDivider()
            }

            item {
                Text(text = "Preferences", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Auto-save reports to storage")
                    Switch(
                        checked = uiState.saveReportsAutomatically,
                        onCheckedChange = { viewModel.toggleAutoSave(context, it) }
                    )
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "Hindrax SS v1.0.0",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun ApiKeyInput(name: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("$name API Key") },
        modifier = Modifier.fillMaxWidth(),
        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
        singleLine = true,
        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
    )
}
