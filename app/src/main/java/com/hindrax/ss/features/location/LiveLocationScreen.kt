package com.hindrax.ss.features.location

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hindrax.ss.data.entity.PeerEntity
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveLocationScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: LiveLocationViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            viewModel.startTracking(context)
        } else {
            viewModel.refreshPermission(context)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshPermission(context)
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.stopTracking() }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("LIVE_GEOLOCATION", fontFamily = FontFamily.Monospace) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Green)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.startTracking(context) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh GPS", tint = Color.Green)
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            GeoPanel(title = "--- GPS_STATUS ---", accent = if (uiState.isTracking) Color.Green else Color.DarkGray) {
                GeoLine("STATE", uiState.status, if (uiState.error == null) Color.Green else Color.Red)
                GeoLine("PROVIDER", uiState.provider.uppercase(), Color.Cyan)
                GeoLine("PERMISSION", if (uiState.permissionGranted) "GRANTED" else "REQUIRED", if (uiState.permissionGranted) Color.Green else Color.Yellow)
                if (uiState.error != null) {
                    GeoLine("ERROR", uiState.error.orEmpty(), Color.Red)
                }
            }

            GeoPanel(title = "--- LIVE_COORDINATES ---", accent = Color.Green) {
                GeoLine("LAT", uiState.latitude?.format(6) ?: "--")
                GeoLine("LON", uiState.longitude?.format(6) ?: "--")
                GeoLine("ACCURACY", uiState.accuracyMeters?.let { "${it.format(1)} m" } ?: "--")
                GeoLine("ALTITUDE", uiState.altitudeMeters?.let { "${it.format(1)} m" } ?: "--")
                GeoLine("SPEED", uiState.speedMetersPerSecond?.let { "${it.format(2)} m/s" } ?: "--")
                GeoLine("BEARING", uiState.bearingDegrees?.let { "${it.format(1)} deg" } ?: "--")
                GeoLine("UPDATED", LiveLocationViewModel.formatTimestamp(uiState.updatedAt), Color.Cyan)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        if (LiveLocationViewModel.hasLocationPermission(context)) {
                            viewModel.startTracking(context)
                        } else {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    modifier = Modifier.weight(1f).height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Green, contentColor = Color.Black),
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("START", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { viewModel.stopTracking() },
                    modifier = Modifier.weight(1f).height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray, contentColor = Color.White),
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Icon(Icons.Default.Pause, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("STOP", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { copyCoordinates(context, uiState) },
                    enabled = uiState.hasFix,
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A0A0A), contentColor = Color.Green),
                    border = BorderStroke(1.dp, Color.Green),
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("COPY", fontFamily = FontFamily.Monospace)
                }

                Button(
                    onClick = { openMaps(context, uiState) },
                    enabled = uiState.hasFix,
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A0A0A), contentColor = Color.Cyan),
                    border = BorderStroke(1.dp, Color.Cyan),
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Icon(Icons.Default.Map, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("MAP", fontFamily = FontFamily.Monospace)
                }
            }

            GeoPanel(title = "--- DEVICE_MESH_LOCATIONS ---", accent = Color.Cyan) {
                GeoLine("KNOWN_DEVICES", uiState.peers.size.toString(), Color.Cyan)
                uiState.peerLocationStatus?.let { GeoLine("SYNC", it, if (it.contains("ERROR")) Color.Red else Color.Green) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.requestAllPeerLocations() },
                        modifier = Modifier.weight(1f).height(46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Cyan, contentColor = Color.Black),
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Icon(Icons.Default.Radar, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("LOCATE_ALL", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Button(
                        onClick = { viewModel.shareMyLocationWithAllDevices() },
                        modifier = Modifier.weight(1f).height(46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A0A0A), contentColor = Color.Green),
                        border = BorderStroke(1.dp, Color.Green),
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SHARE_ME", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (uiState.peers.isEmpty()) {
                    Text(
                        text = "NO_PAIRED_DEVICES. Usa Network Discovery para emparejar otros Hindrax.",
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 14.sp
                    )
                } else {
                    uiState.peers.forEach { peer ->
                        PeerLocationCard(
                            peer = peer,
                            onRequest = { viewModel.requestPeerLocation(peer.id) },
                            onCopy = { copyPeerCoordinates(context, peer) },
                            onMap = { openPeerMaps(context, peer) }
                        )
                    }
                }
            }

            GeoPanel(title = "--- STREAM ---", accent = Color.DarkGray) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MyLocation, contentDescription = null, tint = Color.Green)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (uiState.isTracking) "UPDATES_EVERY_1S_WHILE_SCREEN_ACTIVE" else "PRESS_START_TO_ENABLE_REAL_TIME_GPS",
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun PeerLocationCard(
    peer: PeerEntity,
    onRequest: () -> Unit,
    onCopy: () -> Unit,
    onMap: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF050505)),
        border = BorderStroke(1.dp, if (peer.hasLocation) Color.Cyan.copy(alpha = 0.7f) else Color.DarkGray),
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = if (peer.hasLocation) Color.Cyan else Color.Gray)
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(peer.displayName, color = Color.White, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(peer.id, color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                }
                Text(
                    text = if (peer.isOnline) "ONLINE" else "SEEN",
                    color = if (peer.isOnline) Color.Green else Color.Yellow,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            GeoLine("IP", peer.lastKnownIp, Color.Cyan)
            GeoLine("GPS", peer.locationLabel, if (peer.hasLocation) Color.Green else Color.Gray)
            GeoLine("ACCURACY", peer.locationAccuracy?.let { "${it.format(1)} m" } ?: "--")
            GeoLine("UPDATED", LiveLocationViewModel.formatTimestamp(peer.locationUpdatedAt), Color.Cyan)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onRequest,
                    modifier = Modifier.weight(1f).height(38.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Cyan, contentColor = Color.Black),
                    shape = MaterialTheme.shapes.extraSmall,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp)
                ) {
                    Text("REQUEST", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                }
                Button(
                    onClick = onCopy,
                    enabled = peer.hasLocation,
                    modifier = Modifier.weight(1f).height(38.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A0A0A), contentColor = Color.Green),
                    border = BorderStroke(1.dp, Color.Green),
                    shape = MaterialTheme.shapes.extraSmall,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp)
                ) {
                    Text("COPY", fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                }
                Button(
                    onClick = onMap,
                    enabled = peer.hasLocation,
                    modifier = Modifier.weight(1f).height(38.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A0A0A), contentColor = Color.Cyan),
                    border = BorderStroke(1.dp, Color.Cyan),
                    shape = MaterialTheme.shapes.extraSmall,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp)
                ) {
                    Text("MAP", fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun GeoPanel(
    title: String,
    accent: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
        border = BorderStroke(1.dp, accent),
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = accent)
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, color = accent, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            content()
        }
    }
}

@Composable
private fun GeoLine(label: String, value: String, color: Color = Color.White) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$label: ",
            color = Color.Gray,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier.width(96.dp)
        )
        Text(
            text = value,
            color = color,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            lineHeight = 15.sp
        )
    }
}

private fun copyCoordinates(context: Context, state: LiveLocationUiState) {
    val latitude = state.latitude ?: return
    val longitude = state.longitude ?: return
    val value = "${latitude.format(6)},${longitude.format(6)}"
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Hindrax GPS", value))
}

private fun openMaps(context: Context, state: LiveLocationUiState) {
    val latitude = state.latitude ?: return
    val longitude = state.longitude ?: return
    val uri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude")
    val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}

private fun copyPeerCoordinates(context: Context, peer: PeerEntity) {
    val latitude = peer.latitude ?: return
    val longitude = peer.longitude ?: return
    val value = "${latitude.format(6)},${longitude.format(6)}"
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Hindrax Peer GPS", value))
}

private fun openPeerMaps(context: Context, peer: PeerEntity) {
    val latitude = peer.latitude ?: return
    val longitude = peer.longitude ?: return
    val uri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude(${Uri.encode(peer.displayName)})")
    val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}

private fun Double.format(decimals: Int): String = String.format(Locale.US, "%.${decimals}f", this)
private fun Float.format(decimals: Int): String = String.format(Locale.US, "%.${decimals}f", this)
