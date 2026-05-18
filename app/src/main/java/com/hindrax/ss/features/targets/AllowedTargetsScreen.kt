package com.hindrax.ss.features.targets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Authorized Targets") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Target")
            }
        }
    ) { innerPadding ->
        if (targets.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("No authorized targets defined.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(targets) { target ->
                    TargetItem(target = target, onDelete = { viewModel.removeTarget(target) })
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = target.targetValue, fontWeight = FontWeight.Bold)
                Text(text = "Type: ${target.targetType}", style = MaterialTheme.typography.bodySmall)
                Text(text = "Note: ${target.authorizationNote}", style = MaterialTheme.typography.labelSmall)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
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
        title = { Text("Authorize Target") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = value, onValueChange = { value = it }, label = { Text("Target (IP/Domain)") })
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Authorization Note") })
                // Simple selector for MVP
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = type == "DOMAIN", onClick = { type = "DOMAIN" })
                    Text("Domain")
                    Spacer(modifier = Modifier.width(8.dp))
                    RadioButton(selected = type == "PUBLIC_IP", onClick = { type = "PUBLIC_IP" })
                    Text("Public IP")
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(value, type, note) }, enabled = value.isNotBlank()) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

class AllowedTargetsViewModelFactory(private val repository: com.hindrax.ss.data.repository.TargetRepository) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return AllowedTargetsViewModel(repository) as T
    }
}
