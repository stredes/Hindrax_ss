package com.hindrax.ss.features.targets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hindrax.ss.HindraxApplication
import com.hindrax.ss.data.entity.AllowedTargetEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllowedTargetsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as HindraxApplication
    val viewModel: AllowedTargetsViewModel = viewModel(
        factory = AllowedTargetsViewModelFactory(app.targetRepository)
    )
    val targets by viewModel.allowedTargets.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("AUTHORIZED_TARGETS_DB", fontFamily = FontFamily.Monospace) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Green)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF050505),
                    titleContentColor = Color.Green
                ),
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color.Green,
                contentColor = Color.Black,
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Target")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color(0xFF050505))
        ) {
            if (targets.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("NO_AUTHORIZED_TARGETS_DEFINED", color = Color.Gray, fontFamily = FontFamily.Monospace)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(targets) { target ->
                        TargetItem(target = target, onDelete = { viewModel.removeTarget(target) })
                    }
                }
            }
        }

        if (showAddDialog) {
            AddTargetDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { valValue, type, note ->
                    viewModel.addTarget(valValue, type, note)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun TargetItem(target: AllowedTargetEntity, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.DarkGray),
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = target.targetValue, 
                    fontWeight = FontWeight.Bold, 
                    fontFamily = FontFamily.Monospace,
                    color = Color.White,
                    fontSize = 14.sp
                )
                Text(
                    text = "TYPE: ${target.targetType}", 
                    style = MaterialTheme.typography.bodySmall, 
                    fontFamily = FontFamily.Monospace,
                    color = Color.Cyan
                )
                Text(
                    text = "NOTE: ${target.authorizationNote}", 
                    style = MaterialTheme.typography.labelSmall, 
                    fontFamily = FontFamily.Monospace,
                    color = Color.Gray
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
            }
        }
    }
}

@Composable
fun AddTargetDialog(onDismiss: () -> Unit, onConfirm: (String, String, String) -> Unit) {
    var value by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("DOMAIN") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0A0A0A),
        title = { Text("AUTHORIZE_NEW_TARGET", color = Color.Green, fontFamily = FontFamily.Monospace) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = value, 
                    onValueChange = { value = it }, 
                    label = { Text("TARGET (IP/DOMAIN)", fontFamily = FontFamily.Monospace) },
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, color = Color.Green),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color.DarkGray,
                        focusedBorderColor = Color.Green
                    )
                )
                OutlinedTextField(
                    value = note, 
                    onValueChange = { note = it }, 
                    label = { Text("AUTH_NOTE", fontFamily = FontFamily.Monospace) },
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, color = Color.Green),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color.DarkGray,
                        focusedBorderColor = Color.Green
                    )
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = type == "DOMAIN", 
                        onClick = { type = "DOMAIN" },
                        colors = RadioButtonDefaults.colors(selectedColor = Color.Green)
                    )
                    Text("DOMAIN", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    RadioButton(
                        selected = type == "PUBLIC_IP", 
                        onClick = { type = "PUBLIC_IP" },
                        colors = RadioButtonDefaults.colors(selectedColor = Color.Green)
                    )
                    Text("IP", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(value, type, note) }, 
                enabled = value.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Green, contentColor = Color.Black),
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Text("REGISTER", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { 
                Text("CANCEL", color = Color.Gray, fontFamily = FontFamily.Monospace) 
            }
        }
    )
}

class AllowedTargetsViewModelFactory(private val repository: com.hindrax.ss.data.repository.TargetRepository) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return AllowedTargetsViewModel(repository) as T
    }
}
