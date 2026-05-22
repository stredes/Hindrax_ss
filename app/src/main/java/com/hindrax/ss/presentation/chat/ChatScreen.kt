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
                PeerList(uiState.peers, scrollBehavior) { viewModel.selectPeer(it) }
            } else {
                ChatWindow(
                    messages = uiState.messages,
                    currentMessage = uiState.currentMessage,
                    onMessageChange = viewModel::onMessageChange,
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
                        headlineContent = { Text(peer.id, color = Color.White, fontFamily = FontFamily.Monospace) },
                        supportingContent = { 
                            Text("LAST_IP: ${peer.lastKnownIp}", color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 10.sp) 
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
    messages: List<ChatMessageEntity>,
    currentMessage: String,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior
) {
    val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    Column(modifier = Modifier.fillMaxSize()) {
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
