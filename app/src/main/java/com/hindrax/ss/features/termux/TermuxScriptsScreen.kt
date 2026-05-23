package com.hindrax.ss.features.termux

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LaunchedEffect(Unit) {
        viewModel.checkTermux(context)
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("TERMUX_SCRIPTS_ENGINE", fontFamily = FontFamily.Monospace) },
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
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color(0xFF050505))
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!uiState.isTermuxInstalled) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF330000)),
                        modifier = Modifier.fillMaxWidth(),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red),
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            text = "[!] TERMUX_NOT_DETECTED: Please install Termux and configure the bridge to enable this module.",
                            modifier = Modifier.padding(16.dp),
                            color = Color.Red,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            item {
                Text(
                    text = "--- CONTROLLED_SCRIPT_EXECUTION ---",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = Color.Cyan
                )
            }

            item {
                OutlinedTextField(
                    value = targetHost,
                    onValueChange = { targetHost = it },
                    label = { Text("TARGET_HOST_IP", fontFamily = FontFamily.Monospace) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("192.168.1.1", color = Color.DarkGray) },
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, color = Color.Green),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color.DarkGray,
                        focusedBorderColor = Color.Green,
                        cursorColor = Color.Green
                    ),
                    shape = MaterialTheme.shapes.extraSmall
                )
            }

            item {
                Text(
                    text = "AVAILABLE_SCRIPTS:",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = Color.Gray
                )
            }

            items(uiState.availableScripts) { script ->
                ScriptListItem(
                    script = script,
                    isSelected = selectedScript == script,
                    onClick = { selectedScript = script }
                )
            }

            item {
                Button(
                    onClick = {
                        selectedScript?.let { viewModel.executeScript(context, it.fileName, targetHost) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.isTermuxInstalled && selectedScript != null && targetHost.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Green, contentColor = Color.Black),
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("RUN_IN_TERMUX", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }

            item {
                Text("--- SYSTEM_LOGS ---", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = Color.Cyan)
            }
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(Color(0xFF0A0A0A))
                        .border(1.dp, Color.DarkGray)
                        .padding(8.dp)
                ) {
                    Text(
                        text = uiState.logs,
                        color = Color.Green,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                }
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
            containerColor = if (isSelected) Color(0xFF002200) else Color(0xFF111111)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, 
            if (isSelected) Color.Green else Color.DarkGray
        ),
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = script.displayName.uppercase(), 
                fontWeight = FontWeight.Bold, 
                fontFamily = FontFamily.Monospace,
                color = if (isSelected) Color.Green else Color.White
            )
            Text(
                text = script.description, 
                style = MaterialTheme.typography.bodySmall, 
                fontFamily = FontFamily.Monospace,
                color = Color.Gray
            )
            Text(
                text = "FILE: ${script.fileName}", 
                style = MaterialTheme.typography.labelSmall, 
                color = Color.Cyan,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
