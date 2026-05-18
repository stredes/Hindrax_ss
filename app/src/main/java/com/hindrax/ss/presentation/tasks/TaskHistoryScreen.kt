package com.hindrax.ss.presentation.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskHistoryScreen(
    taskId: Long,
    onBack: () -> Unit,
    viewModel: TaskHistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(taskId) {
        viewModel.loadHistory(taskId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MISSION_LOG_ACCESS", fontFamily = FontFamily.Monospace) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Green)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0A0A0A),
                    titleContentColor = Color.Green
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFF0A0A0A))
        ) {
            // ASCII Banner
            Text(
                text = AsciiBanners.HISTORY_HEADER,
                color = Color.Yellow,
                fontFamily = FontFamily.Monospace,
                fontSize = 8.sp,
                lineHeight = 9.sp,
                modifier = Modifier.padding(16.dp).fillMaxWidth()
            )

            Box(modifier = Modifier.weight(1f)) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color.Green)
                } else if (uiState.history.isEmpty()) {
                    Text(
                        text = "> [!] NO_LOG_ENTRIES_RECOVERED",
                        color = Color.Red,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.history) { event ->
                            HistoryLogItem(event = event)
                        }
                        item {
                            Text(
                                text = ">> [ EOF_LOG ] <<",
                                color = Color.Green,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(top = 16.dp).align(Alignment.Center)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryLogItem(event: com.hindrax.ss.domain.tasks.model.TaskHistory) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val dateStr = dateFormat.format(Date(event.createdAt))

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "[$dateStr]",
                color = Color.Gray,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = event.action.uppercase(),
                color = when (event.action) {
                    "CREACION" -> Color.Green
                    "ELIMINACION" -> Color.Red
                    "INVENTARIO_SYNC" -> Color.Magenta
                    "CAMBIO_ESTADO" -> Color.Yellow
                    else -> Color.Cyan
                },
                fontFamily = FontFamily.Monospace,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                fontSize = 12.sp
            )
        }
        Text(
            text = "  INFO: ${event.detail}",
            color = Color.White,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
        )
        Text(
            text = "  " + "─".repeat(20),
            color = Color.DarkGray,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp
        )
    }
}
