@file:OptIn(ExperimentalMaterial3Api::class)
package com.hindrax.ss.presentation.chat

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.hindrax.ss.data.entity.PeerEntity
import com.hindrax.ss.data.entity.ChatMessageEntity
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatScreen(
    onBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current
    var pendingLocationAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) pendingLocationAction?.invoke()
        pendingLocationAction = null
    }

    fun runWithLocationPermission(action: () -> Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            action()
        } else {
            pendingLocationAction = action
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = if (uiState.selectedPeer != null) "LINKED_TO: ${uiState.selectedPeer?.displayName}" else "SECURE_MESH_CHAT",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 16.sp
                    ) 
                 },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.selectedPeer != null) {
                            viewModel.selectPeer(null)
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.Green
                        )
                    }
                },
                actions = {
                    if (uiState.selectedPeer != null) {
                        IconButton(onClick = { runWithLocationPermission { viewModel.shareLocationWithSelected() } }) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Share GPS Location",
                                tint = Color.Green
                            )
                        }
                        IconButton(onClick = { viewModel.syncFamilyData() }) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Sync Family Data",
                                tint = Color.Cyan
                            )
                        }
                    } else if (uiState.peers.isNotEmpty()) {
                        IconButton(onClick = { runWithLocationPermission { viewModel.shareLocationWithAllDevices() } }) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Share GPS With All Devices",
                                tint = Color.Green
                            )
                        }
                        IconButton(onClick = { viewModel.syncAllDevices() }) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Sync All Devices",
                                tint = Color.Cyan
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF050505),
                    titleContentColor = Color.Green
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFF050505))
        ) {
            if (uiState.selectedPeer == null) {
                PeerList(uiState.peers, scrollBehavior) { viewModel.selectPeer(it) }
            } else {
                ChatWindow(
                    selectedPeer = uiState.selectedPeer,
                    messages = uiState.messages,
                    currentMessage = uiState.currentMessage,
                    nicknameDraft = uiState.nicknameDraft,
                    locationStatus = uiState.locationStatus,
                    onMessageChange = viewModel::onMessageChange,
                    onNicknameChange = viewModel::onNicknameChange,
                    onSaveNickname = viewModel::saveNickname,
                    onShareLocation = { runWithLocationPermission { viewModel.shareLocationWithSelected() } },
                    onSend = viewModel::sendMessage,
                    scrollBehavior = scrollBehavior
                )
            }
        }
    }
}

@Composable
fun PeerList(
    peers: List<PeerEntity>,
    scrollBehavior: TopAppBarScrollBehavior,
    onPeerClick: (PeerEntity) -> Unit
) {
    Column {
        Text(
            text = "--- █ ACTIVE_IDENTITIES █ ---",
            color = Color.Cyan,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            modifier = Modifier.padding(16.dp)
        )
        if (peers.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("WAITING_FOR_PAIRING...", color = Color.Gray, fontFamily = FontFamily.Monospace)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(peers) { peer ->
                    ListItem(
                        headlineContent = { Text(peer.displayName, color = Color.White, fontFamily = FontFamily.Monospace) },
                        supportingContent = { 
                            Column {
                                Text("HASH: ${peer.id} | LAST_IP: ${peer.lastKnownIp}", color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                                if (peer.hasLocation) {
                                    Text("GPS: ${peer.locationLabel}", color = Color.Cyan, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                                }
                            }
                        },
                        trailingContent = {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(if (peer.isOnline) Color.Green else Color.Red)
                            )
                        },
                        modifier = Modifier
                            .clickable { onPeerClick(peer) }
                            .background(Color(0xFF0A0A0A)),
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    HorizontalDivider(color = Color.DarkGray, thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
fun ChatWindow(
    selectedPeer: PeerEntity?,
    messages: List<ChatMessageEntity>,
    currentMessage: String,
    nicknameDraft: String,
    locationStatus: String?,
    onMessageChange: (String) -> Unit,
    onNicknameChange: (String) -> Unit,
    onSaveNickname: () -> Unit,
    onShareLocation: () -> Unit,
    onSend: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior
) {
    val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    Column(modifier = Modifier.fillMaxSize()) {
        DeviceNicknamePanel(
            peer = selectedPeer,
            nicknameDraft = nicknameDraft,
            locationStatus = locationStatus,
            onNicknameChange = onNicknameChange,
            onSaveNickname = onSaveNickname,
            onShareLocation = onShareLocation
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            reverseLayout = false
        ) {
            items(messages) { msg ->
                val alignment = if (msg.isFromMe) Alignment.End else Alignment.Start
                val bgColor = if (msg.isFromMe) Color(0xFF003300) else Color(0xFF1A1A1A)
                val textColor = if (msg.isFromMe) Color.Green else Color.White
                
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
                    Surface(
                        color = bgColor,
                        shape = MaterialTheme.shapes.extraSmall,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if(msg.isFromMe) Color.Green.copy(0.3f) else Color.DarkGray),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(msg.message, color = textColor, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                            Text(
                                text = dateFormat.format(Date(msg.timestamp)),
                                color = Color.Gray,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }
                }
            }
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .background(Color.Black),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = currentMessage,
                onValueChange = onMessageChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("TYPE_MSG", color = Color.DarkGray, fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, color = Color.Green, fontSize = 14.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.DarkGray,
                    focusedBorderColor = Color.Green
                ),
                shape = MaterialTheme.shapes.extraSmall
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onSend,
                modifier = Modifier.background(Color(0xFF002200), MaterialTheme.shapes.extraSmall)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = Color.Green
                )
            }
        }
    }
}

@Composable
fun DeviceNicknamePanel(
    peer: PeerEntity?,
    nicknameDraft: String,
    locationStatus: String?,
    onNicknameChange: (String) -> Unit,
    onSaveNickname: () -> Unit,
    onShareLocation: () -> Unit
) {
    if (peer == null) return
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.DarkGray),
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = "--- DEVICE_NICKNAME_CONFIG ---",
                color = Color.Cyan,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = nicknameDraft,
                    onValueChange = onNicknameChange,
                    modifier = Modifier.weight(1f),
                    label = { Text("NICKNAME", fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                    placeholder = { Text(peer.name, color = Color.DarkGray, fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, color = Color.Green, fontSize = 13.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color.DarkGray,
                        focusedBorderColor = Color.Green,
                        cursorColor = Color.Green
                    ),
                    shape = MaterialTheme.shapes.extraSmall
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onSaveNickname,
                    modifier = Modifier.background(Color(0xFF002200), MaterialTheme.shapes.extraSmall)
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Save nickname", tint = Color.Green)
                }
            }
            Text(
                text = "HASH_ID: ${peer.id}",
                color = Color.Gray,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "--- DEVICE_LOCATION_GPS ---",
                color = Color.Cyan,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (peer.hasLocation) {
                    "LAST_FIX: ${peer.locationLabel} | ACC: ${peer.locationAccuracy ?: "?"}m"
                } else {
                    "LAST_FIX: NO_GPS_DATA"
                },
                color = if (peer.hasLocation) Color.Green else Color.Gray,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
            )
            if (locationStatus != null) {
                Text(
                    text = "STATUS: $locationStatus",
                    color = if (locationStatus.startsWith("GPS_ERROR")) Color.Red else Color.Cyan,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onShareLocation,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Green, contentColor = Color.Black),
                    shape = MaterialTheme.shapes.extraSmall,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("SHARE_MY_GPS", fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                }
                OutlinedButton(
                    onClick = {
                        if (peer.hasLocation) {
                            val uri = Uri.parse("geo:${peer.latitude},${peer.longitude}?q=${peer.latitude},${peer.longitude}(${Uri.encode(peer.displayName)})")
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        }
                    },
                    enabled = peer.hasLocation,
                    shape = MaterialTheme.shapes.extraSmall,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("OPEN_MAP", fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                }
            }
        }
    }
}
