package com.hindrax.ss.presentation.cyd

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hindrax.ss.domain.cyd.CydDevice
import com.hindrax.ss.domain.cyd.DeviceStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CydDashboardScreen(
    onBack: () -> Unit,
    onNavigateToTerminal: () -> Unit,
    onNavigateToFileManager: () -> Unit,
    viewModel: CydDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.device?.name ?: "CYD Dashboard") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.reboot() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reboot")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Device Info Header
            DeviceStatusCard(uiState.device, uiState.status)

            Spacer(modifier = Modifier.height(24.dp))

            Text(text = "Remote Modules", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            
            Spacer(modifier = Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.height(250.dp)
            ) {
                item { ModuleActionButton("CC1101 Scan", Icons.Default.Radar) { viewModel.runModuleAction("cc1101", "scan") } }
                item { ModuleActionButton("nRF24 Scan", Icons.Default.Wifi) { viewModel.runModuleAction("nrf24", "scan") } }
                item { ModuleActionButton("NFC Read", Icons.Default.Nfc) { viewModel.runModuleAction("pn532", "read") } }
                item { ModuleActionButton("IR Capture", Icons.Default.SettingsRemote) { viewModel.runModuleAction("ir", "capture") } }
                item { ModuleActionButton("File Manager", Icons.Default.Folder, onNavigateToFileManager) }
                item { ModuleActionButton("Terminal", Icons.Default.Terminal, onNavigateToTerminal) }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(text = "Real-time Logs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(8.dp)
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.logs) { log ->
                        Text(
                            text = log,
                            color = Color.Green,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceStatusCard(device: CydDevice?, status: DeviceStatus) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DeveloperBoard, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Firmware: Bruce ${device?.firmwareVersion ?: "Unknown"}", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatusInfoItem("Battery", "${status.batteryPercent ?: "--"}%", Icons.Default.BatteryChargingFull)
                StatusInfoItem("Heap", "${(status.freeHeap ?: 0) / 1024} KB", Icons.Default.Memory)
                StatusInfoItem("WiFi", "${status.wifiSignal ?: "--"} dBm", Icons.Default.Wifi)
            }
        }
    }
}

@Composable
fun StatusInfoItem(label: String, value: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
        Spacer(modifier = Modifier.width(4.dp))
        Column {
            Text(text = label, style = MaterialTheme.typography.labelSmall)
            Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleActionButton(name: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.height(80.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = name, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}
