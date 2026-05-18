package com.hindrax.ss.features.termux

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
fun TermuxScriptsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as HindraxApplication
    val viewModel: TermuxScriptsViewModel = viewModel(
        factory = TermuxScriptsViewModelFactory(app.auditRepository)
    )
    val uiState by viewModel.uiState.collectAsState()
    var selectedScript by remember { mutableStateOf<TermuxScriptItem?>(null) }
    var targetHost by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.checkTermux(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Termux Scripts") },
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
            if (!uiState.isTermuxInstalled) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Termux no detectado. Instala Termux y sigue la guía de configuración para usar este módulo.",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text(text = "Ejecutar Script Controlado", style = MaterialTheme.typography.titleMedium)
            
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = targetHost,
                onValueChange = { targetHost = it },
                label = { Text("Target Host / IP") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("192.168.1.1") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(uiState.availableScripts) { script ->
                    ScriptListItem(
                        script = script,
                        isSelected = selectedScript == script,
                        onClick = { selectedScript = script }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { 
                    selectedScript?.let { viewModel.executeScript(context, it.fileName, targetHost) }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.isTermuxInstalled && selectedScript != null && targetHost.isNotBlank()
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Run in Termux")
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

@Composable
fun ScriptListItem(script: TermuxScriptItem, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = script.displayName, fontWeight = FontWeight.Bold)
            Text(text = script.description, style = MaterialTheme.typography.bodySmall)
            Text(text = "File: ${script.fileName}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
    }
}
