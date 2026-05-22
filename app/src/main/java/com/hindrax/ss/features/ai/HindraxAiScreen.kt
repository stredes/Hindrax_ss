package com.hindrax.ss.features.ai

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
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HindraxAiScreen(
    onBack: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: HindraxAiViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LaunchedEffect(Unit) {
        viewModel.load(context)
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("HINDRAX_AI_ASSIST", fontFamily = FontFamily.Monospace) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Green)
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Open Settings", tint = Color.Green)
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
                text = AI_ASCII,
                color = Color.Green,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                lineHeight = 11.sp
            )

            AiStatusPanel(
                model = uiState.model,
                apiKeyConfigured = uiState.apiKeyConfigured,
                ollamaFallbackEnabled = uiState.ollamaFallbackEnabled,
                ollamaModel = uiState.ollamaModel,
                onOpenSettings = onOpenSettings
            )

            OutlinedTextField(
                value = uiState.prompt,
                onValueChange = viewModel::onPromptChange,
                label = { Text("ASK_HINDRAX_AI", fontFamily = FontFamily.Monospace) },
                placeholder = { Text("Ej: prepara checklist para analizar un APK propio", fontFamily = FontFamily.Monospace) },
                modifier = Modifier.fillMaxWidth().height(150.dp),
                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, color = Color.Green),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.DarkGray,
                    focusedBorderColor = Color.Green,
                    cursorColor = Color.Green
                ),
                shape = MaterialTheme.shapes.extraSmall
            )

            Button(
                onClick = { viewModel.ask(context) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !uiState.isRunning && uiState.prompt.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Green, contentColor = Color.Black),
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Icon(Icons.Default.Send, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("RUN_AI_ASSIST", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }

            when {
                uiState.isRunning -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(180.dp).border(BorderStroke(1.dp, Color.DarkGray)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.Green)
                    }
                }
                uiState.error != null -> {
                    AiTerminalPanel(title = "--- OPENAI_ERROR ---", accent = Color.Red) {
                        Text(uiState.error.orEmpty(), color = Color.Red, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    }
                }
                uiState.response.isNotBlank() -> {
                    AiTerminalPanel(title = "--- AI_RESPONSE ---", accent = Color.Green) {
                        Text(uiState.response, color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 15.sp)
                    }
                }
                else -> {
                    AiTerminalPanel(title = "--- READY ---", accent = Color.DarkGray) {
                        Text(
                            "CONFIGURA_OPENAI_API_KEY_EN_SETTINGS_Y_SOLICITA_GUIA_OPERACIONAL",
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
private fun AiStatusPanel(
    model: String,
    apiKeyConfigured: Boolean,
    ollamaFallbackEnabled: Boolean,
    ollamaModel: String,
    onOpenSettings: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
        border = BorderStroke(1.dp, if (apiKeyConfigured) Color.Green else Color.Yellow),
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Psychology, contentDescription = null, tint = Color.Green, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("MODEL: $model", color = Color.Green, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (apiKeyConfigured) "OPENAI_KEY: CONFIGURED" else "OPENAI_KEY: REQUIRED",
                    color = if (apiKeyConfigured) Color.Green else Color.Yellow,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
                Text(
                    if (ollamaFallbackEnabled) "OLLAMA_FALLBACK: $ollamaModel" else "OLLAMA_FALLBACK: OFF",
                    color = if (ollamaFallbackEnabled) Color.Cyan else Color.Gray,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
            }
            if (!apiKeyConfigured) {
                Button(
                    onClick = onOpenSettings,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Yellow, contentColor = Color.Black),
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text("SET", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AiTerminalPanel(
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
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = accent, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            content()
        }
    }
}

private const val AI_ASCII = """
+--[ HINDRAX AI ]--+
| RESPONSES_API    |
| SAFETY_GATE ON   |
| ASCII OPS MODE   |
+------------------+
"""
