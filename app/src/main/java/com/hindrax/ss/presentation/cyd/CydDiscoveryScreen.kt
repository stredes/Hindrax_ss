package com.hindrax.ss.presentation.cyd

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hindrax.ss.domain.cyd.CydDevice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CydDiscoveryScreen(
    onBack: () -> Unit,
    onDeviceConnected: () -> Unit,
    viewModel: CydDiscoveryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Discover CYD") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.startDiscovery() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (uiState.isScanning) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            uiState.errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }

            if (uiState.devices.isEmpty() && !uiState.isScanning) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No devices found. Tap refresh to scan.")
                }
            } else {
                LazyColumn {
                    items(uiState.devices) { device ->
                        CydDeviceItem(
                            device = device,
                            onClick = { viewModel.connectToDevice(device, onDeviceConnected) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CydDeviceItem(device: CydDevice, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(device.name) },
        supportingContent = { Text("${device.connectionType} - ${device.ipAddress ?: device.macAddress ?: "USB"}") },
        leadingContent = { Icon(Icons.Default.Devices, contentDescription = null) },
        modifier = Modifier.clickable { onClick() }
    )
}
