package com.hindrax.ss.features.termux

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.hindrax.ss.ui.theme.SurfaceGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermuxSetupScreen(onBack: () -> Unit) {
    val clipboardManager = LocalClipboardManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Termux Setup") },
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
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Para habilitar las funciones avanzadas en Termux, sigue estos pasos:",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            StepItem(
                number = "1",
                text = "Instala Termux desde F-Droid (versión recomendada)."
            )

            StepItem(
                number = "2",
                text = "Crea el directorio de scripts de Hindrax SS:",
                command = "mkdir -p ~/.hindrax_ss/scripts"
            )

            StepItem(
                number = "3",
                text = "Crea un script de prueba (ej. network_ping.sh):",
                command = "echo '#!/bin/bash\nping -c 4 \"$1\"' > ~/.hindrax_ss/scripts/network_ping.sh && chmod +x ~/.hindrax_ss/scripts/network_ping.sh"
            )

            StepItem(
                number = "4",
                text = "Asegúrate de conceder el permiso 'com.termux.permission.RUN_COMMAND' a Hindrax SS en los ajustes de tu Android."
            )

            Spacer(modifier = Modifier.height(24.dp))
            
            AlertCard()
        }
    }
}

@Composable
fun StepItem(number: String, text: String, command: String? = null) {
    val clipboardManager = LocalClipboardManager.current
    
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row {
            Text(text = "$number.", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = text)
        }
        if (command != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = command,
                    color = Color.Green,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { clipboardManager.setText(AnnotatedString(command)) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun AlertCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Importante",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = "Hindrax SS nunca ejecutará comandos sin tu supervisión o fuera de la carpeta configurada (~/.hindrax_ss/scripts/).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}
