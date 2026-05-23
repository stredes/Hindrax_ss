@file:OptIn(ExperimentalMaterial3Api::class)
package com.hindrax.ss.presentation.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = if (uiState.selectedPeer != null) "LINKED_TO: ${uiState.selectedPeer?.id}" else "SECURE_MESH_CHAT",
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
                        IconButton(onClick = { viewModel.syncFamilyData() }) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Sync Family Data",
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
                PeerList(
                    peers = uiState.peers,
                    scrollBehavior = scrollBehavior,
                    onPeerClick = { viewModel.selectPeer(it) },
                    onDeletePeer = { viewModel.deletePeer(it, deleteMessages = false) }
                )
            } else {
                ChatWindow(
                    peer = uiState.selectedPeer!!,
                    messages = uiState.messages,
                    currentMessage = uiState.currentMessage,
                    nicknameDraft = uiState.nicknameDraft,
                    onMessageChange = viewModel::onMessageChange,
                    onNicknameChange = viewModel::onNicknameChange,
                    onSaveNickname = viewModel::saveNickname,
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
    onPeerClick: (PeerEntity) -> Unit,
    onDeletePeer: (PeerEntity) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text(
                text = "--- █ ACTIVE_IDENTITIES █ ---",
                color = Color.Cyan,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                modifier = Modifier.padding(16.dp)
            )
        }
        if (peers.isEmpty()) {
            item {
                Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                    Text("WAITING_FOR_PAIRING...", color = Color.Gray, fontFamily = FontFamily.Monospace)
                }
            }
        } else {
            items(peers) { peer ->
                var confirmDelete by remember(peer.id) { mutableStateOf(false) }
                ListItem(
                    headlineContent = {
                        Text(peer.displayName, color = Color.White, fontFamily = FontFamily.Monospace)
                    },
                    supportingContent = {
                        Column {
                            Text("HASH: ${peer.id}", color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                            Text("LAST_ADDR: ${peer.lastKnownIp}", color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                        }
                    },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(if (peer.isOnline) Color.Green else Color.Red)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = { confirmDelete = true }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Device", tint = Color.Red)
                            }
                        }
                    },
                    modifier = Modifier
                        .clickable { onPeerClick(peer) }
                        .background(Color(0xFF0A0A0A)),
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                if (confirmDelete) {
                    AlertDialog(
                        onDismissRequest = { confirmDelete = false },
                        confirmButton = {
                            TextButton(onClick = {
                                confirmDelete = false
                                onDeletePeer(peer)
                            }) {
                                Text("DELETE", color = Color.Red, fontFamily = FontFamily.Monospace)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { confirmDelete = false }) {
                                Text("CANCEL", fontFamily = FontFamily.Monospace)
                            }
                        },
                        title = {
                            Text("DELETE_DEVICE", fontFamily = FontFamily.Monospace)
                        },
                        text = {
                            Text(
                                "Remove ${peer.displayName} from linked devices. Messages are kept unless the database is cleaned manually.",
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    )
                }
                HorizontalDivider(color = Color.DarkGray, thickness = 0.5.dp)
            }
        }
    }
}

@Composable
fun ChatWindow(
    peer: PeerEntity,
    messages: List<ChatMessageEntity>,
    currentMessage: String,
    nicknameDraft: String,
    onMessageChange: (String) -> Unit,
    onNicknameChange: (String) -> Unit,
    onSaveNickname: () -> Unit,
    onSend: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior
) {
    val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    Column(modifier = Modifier.fillMaxSize()) {
        DeviceIdentityEditor(
            peer = peer,
            nicknameDraft = nicknameDraft,
            onNicknameChange = onNicknameChange,
            onSaveNickname = onSaveNickname
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
private fun DeviceIdentityEditor(
    peer: PeerEntity,
    nicknameDraft: String,
    onNicknameChange: (String) -> Unit,
    onSaveNickname: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF090909))
            .padding(12.dp)
    ) {
        Text(
            text = "LINKED_DEVICE: ${peer.displayName}",
            color = Color.Cyan,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp
        )
        Text(
            text = "HASH: ${peer.id}  |  LAST_ADDR: ${peer.lastKnownIp}",
            color = Color.Gray,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = nicknameDraft,
                onValueChange = onNicknameChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("DEVICE_NAME", fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, color = Color.Green, fontSize = 12.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.DarkGray,
                    focusedBorderColor = Color.Green
                ),
                shape = MaterialTheme.shapes.extraSmall
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onSaveNickname,
                shape = MaterialTheme.shapes.extraSmall,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Green, contentColor = Color.Black),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text("SAVE", fontFamily = FontFamily.Monospace, fontSize = 10.sp)
            }
        }
    }
}
