package com.hindrax.ss.presentation.cyd

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CydTerminalScreen(
    onBack: () -> Unit,
    viewModel: CydTerminalViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var commandText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new logs arrive
    LaunchedEffect(uiState.consoleOutput.size) {
        if (uiState.consoleOutput.isNotEmpty()) {
            listState.animateScrollToItem(uiState.consoleOutput.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CYD Terminal") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearTerminal() }) {
                        Icon(Icons.Default.ClearAll, contentDescription = "Clear")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 2.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .imePadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = commandText,
                        onValueChange = { commandText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Enter command...") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (commandText.isNotBlank()) {
                                viewModel.sendCommand(commandText)
                                commandText = ""
                            }
                        })
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (commandText.isNotBlank()) {
                                viewModel.sendCommand(commandText)
                                commandText = ""
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send")
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color.Black)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                items(uiState.consoleOutput) { line ->
                    Text(
                        text = line,
                        color = if (line.startsWith(">")) Color.Yellow else Color.Green,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
