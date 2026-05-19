package com.hindrax.ss.features.network

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkDiscoveryScreen(
    onBack: () -> Unit,
    onNavigateToCyd: () -> Unit,
    onNavigateToChat: () -> Unit = {}
) {
    val viewModel: NetworkDiscoveryViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    
    // Permission Handling
    val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    } else {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            viewModel.startDiscovery()
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("NODES_DISCOVERY", fontFamily = FontFamily.Monospace) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Green)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.checkAppUpdates() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Check Updates", tint = Color.Green)
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
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color(0xFF050505))
                .padding(16.dp)
        ) {
            // Self Identity Panel
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF151515)),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.DarkGray),
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Fingerprint, null, tint = Color.Cyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "MY_HASH: ${uiState.myDeviceId}",
                            color = Color.Cyan,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        text = "LOCAL_ADDR: ${uiState.localIp}",
                        color = Color.Green.copy(0.7f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Discovery Actions
            Button(
                onClick = { launcher.launch(permissionsToRequest) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isScanning,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Green, contentColor = Color.Black),
                shape = MaterialTheme.shapes.extraSmall
            ) {
                if (uiState.isScanning) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SCANNING_AIR_AND_WIRE...", fontFamily = FontFamily.Monospace)
                } else {
                    Icon(Icons.Default.Radar, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("INIT_HYBRID_SCAN", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }

            if (uiState.isScanning) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { uiState.progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.Green,
                    trackColor = Color.DarkGray
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("--- ACTIVE_NODES ---", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = Color.Cyan)

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.discoveredDevices) { device ->
                    DeviceItemComponent(
                        device = device,
                        onConnectCyd = { 
                            viewModel.connectCyd(device.ip ?: "0.0.0.0", device.cydName ?: "Bruce", onNavigateToCyd) 
                        },
                        onConnectHindrax = {
                            viewModel.connectHindraxNode(device) {
                                onNavigateToChat()
                            }
                        },
                        onSync = {
                            viewModel.syncFamilyData(device.deviceHash ?: "")
                        }
                    )
                }
            }

            // System Console
            Text("SYSTEM_OUTPUT:", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = Color.Gray)
            Box(
                modifier = Modifier.fillMaxWidth().height(80.dp).background(Color(0xFF0A0A0A)).border(1.dp, Color.DarkGray).padding(8.dp)
            ) {
                Text(text = uiState.logs, color = Color.Green, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun DeviceItemComponent(
    device: DiscoveredDevice, 
    onConnectCyd: () -> Unit,
    onConnectHindrax: () -> Unit,
    onSync: () -> Unit
) {
    val nodeColor = if (device.isHindraxNode) Color.Yellow else if (device.isCyd) Color.Cyan else Color.Green
    
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F0F)),
        border = androidx.compose.foundation.BorderStroke(1.dp, nodeColor.copy(0.5f)),
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (device.isHindraxNode) Icons.Default.Smartphone else if (device.isCyd) Icons.Default.Memory else Icons.Default.Devices,
                contentDescription = null,
                tint = nodeColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.deviceHash ?: device.ip ?: "Unknown Node", 
                    fontWeight = FontWeight.Bold, 
                    fontFamily = FontFamily.Monospace, 
                    color = Color.White,
                    fontSize = 14.sp
                )
                Text(
                    text = if (device.isHindraxNode) "HINDRAX_MESH [${device.discoveryMethod}]" else "GUEST_DEVICE",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = nodeColor.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
            }
            if (device.isHindraxNode) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (device.isAlreadyPaired) {
                        IconButton(
                            onClick = onSync,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = "Sync Data", tint = Color.Cyan)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    
                    Button(
                        onClick = onConnectHindrax,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (device.isAlreadyPaired) Color.DarkGray else Color.Yellow, 
                            contentColor = Color.Black
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp),
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            text = if (device.isAlreadyPaired) "OPEN" else "PAIR", 
                            fontSize = 10.sp, 
                            fontFamily = FontFamily.Monospace, 
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else if (device.isCyd) {
                Button(
                    onClick = onConnectCyd,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Cyan, contentColor = Color.Black),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp),
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text("CONNECT", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
