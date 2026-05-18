package com.hindrax.ss.presentation.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskSettingsScreen(
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TASK_SYSTEM_CONFIG", fontFamily = FontFamily.Monospace) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Green)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0A0A0A), titleContentColor = Color.Green)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFF0A0A0A))
                .padding(16.dp)
        ) {
            Text("--- SYSTEM_MAINTENANCE ---", color = Color.Cyan, fontFamily = FontFamily.Monospace)
            
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { /* Lógica para limpiar todo el historial */ },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("PURGE_ALL_HISTORY", fontFamily = FontFamily.Monospace)
            }
            
            Text(
                "Warning: This action will permanently delete all event logs for all tasks.",
                color = Color.DarkGray,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
