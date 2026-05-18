package com.hindrax.ss.presentation.tasks

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hindrax.ss.domain.tasks.model.ChecklistItem
import com.hindrax.ss.domain.tasks.model.TaskStatus
import com.hindrax.ss.domain.tasks.model.TaskType
import com.hindrax.ss.presentation.tasks.AsciiBanners
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StatusBadge(status: TaskStatus) {
    val color = when (status) {
        TaskStatus.COMPLETADA -> Color.Green
        TaskStatus.EN_PROGRESO -> Color.Cyan
        TaskStatus.PENDIENTE -> Color.Yellow
        TaskStatus.PAUSADA -> Color.Gray
        TaskStatus.CANCELADA -> Color.Red
    }
    Surface(
        color = color.copy(alpha = 0.1f),
        contentColor = color,
        shape = MaterialTheme.shapes.extraSmall,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = status.name,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp
        )
    }
}

@Composable
fun ChecklistItemRow(
    item: ChecklistItem,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = item.isChecked,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = Color.Green,
                uncheckedColor = Color.Gray,
                checkmarkColor = Color.Black
            )
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.text.uppercase(),
                color = if (item.isChecked) Color.Gray else Color.White,
                fontFamily = FontFamily.Monospace,
                textDecoration = if (item.isChecked) TextDecoration.LineThrough else null,
                fontSize = 13.sp
            )
            if (item.quantity != null) {
                Text(
                    text = "└─ ${item.quantity} ${item.unit ?: ""}",
                    color = Color.Cyan,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskDetailScreen(
    taskId: Long,
    onEdit: (Long) -> Unit,
    onShowHistory: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: TaskDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }

    LaunchedEffect(taskId) {
        viewModel.loadTask(taskId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MISSION_INTEL", fontFamily = FontFamily.Monospace) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Green)
                    }
                },
                actions = {
                    IconButton(onClick = { showShareDialog = true }) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.Green)
                    }
                    IconButton(onClick = { onShowHistory(taskId) }) {
                        Icon(Icons.Default.History, contentDescription = "History", tint = Color.Cyan)
                    }
                    IconButton(onClick = { onEdit(taskId) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Yellow)
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF050505),
                    titleContentColor = Color.Green
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFF050505))
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color.Green)
            } else if (uiState.task != null) {
                val task = uiState.task!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(modifier = Modifier.height(12.dp))

                    // ASCII Art
                    val art = if (task.status == TaskStatus.COMPLETADA) {
                        AsciiBanners.MISSION_COMPLETE
                    } else {
                        when(task.type) {
                            TaskType.SHOPPING -> AsciiBanners.SHOPPING_ART
                            TaskType.FERIA -> AsciiBanners.FERIA_ART
                            else -> ""
                        }
                    }

                    if (art.isNotBlank()) {
                        Text(
                            text = art,
                            color = if (task.status == TaskStatus.COMPLETADA) Color.Green else Color.Cyan,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 6.sp,
                            lineHeight = 7.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            softWrap = false
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Mission Header
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "[ ${task.type.name} ]",
                            color = Color.Cyan,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.height(1.dp).weight(1f).background(Color.DarkGray))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = task.title.uppercase(),
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 18.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "STATUS: ", color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        StatusBadge(status = task.status)
                    }

                    // Checklist section
                    if (task.type == TaskType.SHOPPING || task.type == TaskType.FERIA) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "┌ PROCUREMENT_CHECKLIST ──┐",
                            color = Color.Cyan,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                        
                        Box(modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0A0A0A))
                            .border(1.dp, Color.DarkGray.copy(alpha = 0.5f))
                            .padding(8.dp)
                        ) {
                            if (task.checklist.isEmpty()) {
                                Text(
                                    text = "[ NO_ITEMS_IN_LIST ]",
                                    color = Color.DarkGray,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                            } else {
                                Column {
                                    task.checklist.forEach { item ->
                                        ChecklistItemRow(
                                            item = item,
                                            onToggle = { viewModel.toggleChecklistItem(item.id) }
                                        )
                                    }
                                }
                            }
                        }
                        Text(
                            text = "└─────────────────────────┘",
                            color = Color.Cyan,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }

                    // Location section
                    if (!task.locationName.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "TARGET_COORD:", color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                                Text(text = task.locationName, color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                            }
                            IconButton(onClick = {
                                val query = Uri.encode(task.locationName)
                                val gmmIntentUri = if (task.latitude != null && task.longitude != null) {
                                    Uri.parse("geo:${task.latitude},${task.longitude}?q=$query")
                                } else {
                                    Uri.parse("geo:0,0?q=$query")
                                }
                                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                context.startActivity(mapIntent)
                            }) {
                                Icon(Icons.Default.Map, contentDescription = "Open Maps", tint = Color.Cyan)
                            }
                        }
                    }

                    // Description section
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(text = "DEBUG_INFO:", color = Color.DarkGray, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.DarkGray)
                        .padding(10.dp)
                    ) {
                        Text(
                            text = if (task.description.isBlank()) "[ DATA_EMPTY ]" else task.description,
                            color = Color.Green.copy(alpha = 0.8f),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    Text(text = "TIMESTAMP_R: ${dateFormat.format(Date(task.createdAt))}", color = Color.DarkGray, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                    Text(text = "TIMESTAMP_U: ${dateFormat.format(Date(task.updatedAt))}", color = Color.DarkGray, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                    
                    Spacer(modifier = Modifier.height(32.dp))
                }
            } else if (uiState.error != null) {
                Text(
                    text = "FATAL_ERROR: [${uiState.error}]",
                    color = Color.Red,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = Color(0xFF0A0A0A),
            title = { Text("PURGE_RECORD?", color = Color.Red, fontFamily = FontFamily.Monospace) },
            text = { 
                Text("Executing this command will permanently remove the mission record.", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 14.sp) 
            },
            confirmButton = {
                Button(onClick = { viewModel.deleteTask(onBack) }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                    Text("CONFIRM", fontFamily = FontFamily.Monospace)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("ABORT", color = Color.Gray, fontFamily = FontFamily.Monospace)
                }
            }
        )
    }

    if (showShareDialog) {
        AlertDialog(
            onDismissRequest = { showShareDialog = false },
            containerColor = Color(0xFF0A0A0A),
            title = { Text("SHARE_MISSION", color = Color.Green, fontFamily = FontFamily.Monospace) },
            text = {
                Column {
                    Text("Select a linked node to sync this mission data:", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    if (uiState.availablePeers.isEmpty()) {
                        Text("NO_ACTIVE_NODES_FOUND", color = Color.Red, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                    } else {
                        uiState.availablePeers.forEach { peer ->
                            ListItem(
                                headlineContent = { Text(peer.id, color = Color.White, fontFamily = FontFamily.Monospace) },
                                modifier = Modifier.clickable {
                                    viewModel.shareTask(peer.id)
                                    showShareDialog = false
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showShareDialog = false }) {
                    Text("CANCEL", color = Color.Gray, fontFamily = FontFamily.Monospace)
                }
            }
        )
    }
}
