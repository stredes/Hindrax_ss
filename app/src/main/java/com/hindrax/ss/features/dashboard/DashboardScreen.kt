package com.hindrax.ss.features.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hindrax.ss.presentation.tasks.AsciiBanners

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigateToNetwork: () -> Unit,
    onNavigateToPortScanner: () -> Unit,
    onNavigateToNetworkDiscovery: () -> Unit,
    onNavigateToBannerGrabbing: () -> Unit,
    onNavigateToDns: () -> Unit,
    onNavigateToApk: () -> Unit,
    onNavigateToWeb: () -> Unit,
    onNavigateToWebScanner: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToOsint: () -> Unit,
    onNavigateToWhois: () -> Unit,
    onNavigateToMetadata: () -> Unit,
    onNavigateToTargets: () -> Unit,
    onNavigateToTermuxSetup: () -> Unit,
    onNavigateToTermuxScripts: () -> Unit,
    onNavigateToAutomation: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToCydConnect: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToChat: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    val currentVersion = packageInfo.versionName ?: "1.0.0"

    LaunchedEffect(Unit) {
        viewModel.refreshStatus(context, currentVersion)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050505))
            .padding(16.dp)
    ) {
        // ASCII Header
        Text(
            text = AsciiBanners.HINDRAX_MAIN,
            color = Color.Green,
            fontFamily = FontFamily.Monospace,
            fontSize = 7.sp,
            lineHeight = 8.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            softWrap = false
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Update Notification
        if (uiState.updateAvailable) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF003300)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Green),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Update, contentDescription = null, tint = Color.Green)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("UPDATE_AVAILABLE: v${uiState.newVersion}", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("Security and core optimizations ready.", color = Color.Green.copy(0.7f), fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                    }
                    Button(
                        onClick = { viewModel.installUpdate() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Green, contentColor = Color.Black),
                        shape = MaterialTheme.shapes.extraSmall,
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Text("INSTALL", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "HINDRAX_CORE",
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = Color.Green
            )
            Row {
                IconButton(onClick = onNavigateToSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.Green)
                }
                IconButton(onClick = onNavigateToTermuxSetup) {
                    Icon(Icons.Default.Build, contentDescription = "Termux Setup", tint = Color.Green)
                }
                IconButton(onClick = onNavigateToTargets) {
                    Icon(Icons.Default.Lock, contentDescription = "Authorized Targets", tint = Color.Green)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))

        // Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.DarkGray)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                StatusItem(label = "NET_ADDR", value = uiState.localIp, icon = Icons.Default.Info)
                StatusItem(
                    label = "TERMUX", 
                    value = if (uiState.isTermuxInstalled) "READY" else "OFFLINE",
                    icon = Icons.Default.Terminal,
                    color = if (uiState.isTermuxInstalled) Color.Green else Color.Red
                )
                StatusItem(label = "VERSION", value = "v$currentVersion", icon = Icons.Default.Fingerprint, color = Color.Cyan)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "--- CORE_MODULES ---",
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = Color.Cyan
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            item {
                ModuleCard("Automation", Icons.Default.Bolt, onNavigateToAutomation, accentColor = Color.Green)
            }
            item {
                ModuleCard("Missions", Icons.Default.Assignment, onNavigateToTasks, accentColor = Color.Yellow)
            }
            item {
                ModuleCard("Logistics", Icons.Default.Inventory, onNavigateToInventory, accentColor = Color.Cyan)
            }
            item {
                ModuleCard("CYD_Link", Icons.Default.DeveloperBoard, onNavigateToCydConnect, accentColor = Color.Magenta)
            }
            item {
                ModuleCard("Mesh Chat", Icons.Default.Chat, onNavigateToChat, accentColor = Color.White)
            }
            item {
                ModuleCard("Net_Disc", Icons.Default.CellTower, onNavigateToNetworkDiscovery)
            }
            item {
                ModuleCard("Terminal", Icons.Default.Terminal, onNavigateToTermuxScripts)
            }
            item {
                ModuleCard("Scanner", Icons.Default.Lan, onNavigateToPortScanner)
            }
            item {
                ModuleCard("Web_Scan", Icons.Default.ManageSearch, onNavigateToWebScanner)
            }
            item {
                ModuleCard("Logs", Icons.Default.History, onNavigateToReports)
            }
        }
    }
}

@Composable
fun StatusItem(label: String, value: String, icon: ImageVector, color: Color = Color.Green) {
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = color)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "$label: ", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = Color.Gray)
        Text(text = value, color = color, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleCard(
    name: String, 
    icon: ImageVector, 
    onClick: () -> Unit,
    accentColor: Color = Color.White
) {
    Card(
        onClick = onClick,
        modifier = Modifier.height(80.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.DarkGray),
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = accentColor)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = name.uppercase(), 
                fontWeight = FontWeight.Bold, 
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = Color.White
            )
        }
    }
}
