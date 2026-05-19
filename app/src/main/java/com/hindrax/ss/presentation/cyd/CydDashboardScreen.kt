package com.hindrax.ss.presentation.cyd

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = uiState.device?.name?.uppercase() ?: "CYD_DASHBOARD",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 16.sp
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Green)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.reboot() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reboot", tint = Color.Green)
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
                .background(Color(0xFF050505)),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                DeviceStatusCard(uiState.device, uiState.status)
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                Text(
                    text = "--- REMOTE_MODULES ---", 
                    style = MaterialTheme.typography.labelSmall, 
                    fontFamily = FontFamily.Monospace, 
                    color = Color.Cyan
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Grid simulation in LazyColumn
            item {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.weight(1f)) {
                        ModuleActionButton("CC1101 SCAN", Icons.Default.Radar) { viewModel.runModuleAction("cc1101", "scan") }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        ModuleActionButton("NRF24 SCAN", Icons.Default.Wifi) { viewModel.runModuleAction("nrf24", "scan") }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.weight(1f)) {
                        ModuleActionButton("NFC READ", Icons.Default.Nfc) { viewModel.runModuleAction("pn532", "read") }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        ModuleActionButton("IR CAPTURE", Icons.Default.SettingsRemote) { viewModel.runModuleAction("ir", "capture") }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.weight(1f)) {
                        ModuleActionButton("FILES", Icons.Default.Folder, onNavigateToFileManager)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        ModuleActionButton("TERMINAL", Icons.Default.Terminal, onNavigateToTerminal)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                Text(
                    text = "--- REALTIME_LOGS ---", 
                    style = MaterialTheme.typography.labelSmall, 
                    fontFamily = FontFamily.Monospace, 
                    color = Color.Cyan
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(uiState.logs) { log ->
                Text(
                    text = log,
                    color = Color.Green,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0A0A0A))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun DeviceStatusCard(device: CydDevice?, status: DeviceStatus) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.DarkGray),
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DeveloperBoard, contentDescription = null, tint = Color.Cyan, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "FIRMWARE: BRUCE ${device?.firmwareVersion ?: "UNKNOWN"}", 
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White,
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatusInfoItem("BATT", "${status.batteryPercent ?: "--"}%", Icons.Default.BatteryChargingFull, if((status.batteryPercent ?: 100) < 20) Color.Red else Color.Green)
                StatusInfoItem("HEAP", "${(status.freeHeap ?: 0) / 1024}KB", Icons.Default.Memory, Color.Cyan)
                StatusInfoItem("RSSI", "${status.wifiSignal ?: "--"}dBm", Icons.Default.Wifi, Color.Yellow)
            }
        }
    }
}

@Composable
fun StatusInfoItem(label: String, value: String, icon: ImageVector, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = color)
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
            Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = color, fontFamily = FontFamily.Monospace)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleActionButton(name: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.height(80.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.DarkGray),
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = name, 
                style = MaterialTheme.typography.labelSmall, 
                color = Color.Green,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
            )
        }
    }
}
