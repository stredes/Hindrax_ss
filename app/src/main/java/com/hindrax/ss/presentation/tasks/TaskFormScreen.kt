package com.hindrax.ss.presentation.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hindrax.ss.domain.tasks.model.InventoryItem
import com.hindrax.ss.domain.tasks.model.TaskStatus
import com.hindrax.ss.domain.tasks.model.TaskType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskFormScreen(
    taskId: Long,
    onBack: () -> Unit,
    viewModel: TaskFormViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(taskId) {
        viewModel.loadTask(taskId)
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = if (taskId == -1L) "INIT_MISSION_PROTOCOL" else "PATCH_CORE_LOGIC",
                        fontFamily = FontFamily.Monospace
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Green)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.saveTask() }) {
                        Icon(Icons.Default.Save, contentDescription = "Save", tint = Color.Green)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF050505),
                    titleContentColor = Color.Green
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFF050505))
                .verticalScroll(rememberScrollState())
        ) {
            // ASCII Header
            Text(
                text = AsciiBanners.FORM_HEADER,
                color = Color.Green,
                fontFamily = FontFamily.Monospace,
                fontSize = 8.sp,
                lineHeight = 9.sp,
                modifier = Modifier.padding(16.dp).fillMaxWidth()
            )

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "--- █ INPUT_PARAMETERS █ ---",
                    color = Color.Cyan,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )

                OutlinedTextField(
                    value = uiState.title,
                    onValueChange = viewModel::onTitleChange,
                    label = { Text("MISSION_IDENTIFIER", fontFamily = FontFamily.Monospace) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = uiState.error != null && uiState.title.isBlank(),
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, color = Color.Green),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color.DarkGray,
                        focusedBorderColor = Color.Green,
                        cursorColor = Color.Green
                    ),
                    shape = MaterialTheme.shapes.extraSmall
                )

                Text(
                    text = "SELECT_CLASSIFICATION:",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = Color.Cyan
                )
                
                TaskTypeSelector(
                    selectedType = uiState.type,
                    onTypeSelected = viewModel::onTypeChange
                )

                // Dynamic fields based on type
                if (uiState.type == TaskType.SHOPPING || uiState.type == TaskType.INVENTORY || uiState.type == TaskType.FERIA) {
                    Text(
                        text = "LINK_TO_INVENTORY_RESOURCE (OPTIONAL)",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = Color.Cyan
                    )
                    
                    InventoryLinkSelector(
                        items = uiState.availableInventory,
                        selectedId = uiState.inventoryItemId,
                        onItemSelected = viewModel::onInventoryLinkChange
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = uiState.quantity?.toString() ?: "",
                            onValueChange = { viewModel.onQuantityChange(it.toDoubleOrNull(), uiState.unit) },
                            label = { Text("QTY", fontFamily = FontFamily.Monospace) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, color = Color.White),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Cyan),
                            shape = MaterialTheme.shapes.extraSmall
                        )
                        OutlinedTextField(
                            value = uiState.unit ?: "",
                            onValueChange = { viewModel.onQuantityChange(uiState.quantity, it) },
                            label = { Text("UNIT", fontFamily = FontFamily.Monospace) },
                            modifier = Modifier.weight(1f),
                            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, color = Color.White),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Cyan),
                            shape = MaterialTheme.shapes.extraSmall
                        )
                    }
                }

                // Checklist Editor for Shopping and Feria
                if (uiState.type == TaskType.SHOPPING || uiState.type == TaskType.FERIA) {
                    Text(
                        text = "--- PROCUREMENT_MANIFEST ---",
                        color = Color.Cyan,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                    
                    ChecklistEditor(
                        items = uiState.checklist,
                        onAddItem = { text, qty, unit -> viewModel.addChecklistItem(text, qty, unit) },
                        onRemoveItem = viewModel::removeChecklistItem,
                        onToggleItem = viewModel::toggleChecklistItem,
                        onUpdateItem = { id, text, qty, unit -> viewModel.updateChecklistItem(id, text, qty, unit) }
                    )
                }

                if (uiState.type == TaskType.EVENT || uiState.type == TaskType.SHOPPING || uiState.type == TaskType.FERIA) {
                    OutlinedTextField(
                        value = uiState.locationName ?: "",
                        onValueChange = { viewModel.onLocationChange(it, uiState.latitude, uiState.longitude) },
                        label = { Text("TARGET_COORDINATES", fontFamily = FontFamily.Monospace) },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Cyan) },
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, color = Color.White),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Cyan),
                        shape = MaterialTheme.shapes.extraSmall
                    )
                }

                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = viewModel::onDescriptionChange,
                    label = { Text("INTEL_DETAILS / NOTES", fontFamily = FontFamily.Monospace) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color.DarkGray,
                        focusedBorderColor = Color.Cyan,
                        cursorColor = Color.Cyan
                    ),
                    shape = MaterialTheme.shapes.extraSmall
                )

                Text(
                    text = "PRIORITY_LEVEL:",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = Color.Yellow
                )
                
                TaskStatusSelector(
                    selectedStatus = uiState.status,
                    onStatusSelected = viewModel::onStatusChange
                )

                if (uiState.error != null) {
                    Text(
                        text = ">>> [ERROR]: ${uiState.error}",
                        color = Color.Red,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { viewModel.saveTask() },
                    modifier = Modifier.fillMaxWidth().height(56.dp).padding(bottom = 32.dp),
                    enabled = !uiState.isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Green,
                        contentColor = Color.Black
                    ),
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black)
                    } else {
                        Text(
                            text = if (taskId == -1L) "EXECUTE_INIT_CMD" else "PATCH_DATABASE",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChecklistEditor(
    items: List<com.hindrax.ss.domain.tasks.model.ChecklistItem>,
    onAddItem: (String, Double?, String?) -> Unit,
    onRemoveItem: (String) -> Unit,
    onToggleItem: (String) -> Unit,
    onUpdateItem: (String, String?, Double?, String?) -> Unit
) {
    var newItemText by remember { mutableStateOf("") }
    var newItemQty by remember { mutableStateOf("") }
    var newItemUnit by remember { mutableStateOf("unid") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Add Header
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F0F)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.DarkGray),
            shape = MaterialTheme.shapes.extraSmall
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                OutlinedTextField(
                    value = newItemText,
                    onValueChange = { newItemText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Resource Name", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                    placeholder = { Text("Input data...", fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, color = Color.White, fontSize = 14.sp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Cyan),
                    shape = MaterialTheme.shapes.extraSmall
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newItemQty,
                        onValueChange = { newItemQty = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Qty", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, color = Color.White, fontSize = 14.sp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Cyan),
                        shape = MaterialTheme.shapes.extraSmall
                    )
                    OutlinedTextField(
                        value = newItemUnit,
                        onValueChange = { newItemUnit = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Unit", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, color = Color.White, fontSize = 14.sp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Cyan),
                        shape = MaterialTheme.shapes.extraSmall
                    )
                    IconButton(
                        onClick = {
                            if (newItemText.isNotBlank()) {
                                onAddItem(newItemText, newItemQty.toDoubleOrNull(), newItemUnit)
                                newItemText = ""
                                newItemQty = ""
                            }
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = "Add", tint = Color.Green)
                    }
                }
            }
        }

        // List of items
        items.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = item.isChecked,
                    onCheckedChange = { onToggleItem(item.id) },
                    colors = CheckboxDefaults.colors(checkedColor = Color.Green)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.text.uppercase(),
                        color = if (item.isChecked) Color.Gray else Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    )
                    if (item.quantity != null) {
                        Text(
                            text = "└─ ${item.quantity} ${item.unit ?: ""}",
                            color = Color.Cyan,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                IconButton(onClick = { onRemoveItem(item.id) }) {
                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.Red, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryLinkSelector(
    items: List<InventoryItem>,
    selectedId: Long?,
    onItemSelected: (Long?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedItem = items.find { it.id == selectedId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedItem?.name ?: "[ SELECT_RESOURCE_LINK ]",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            textStyle = LocalTextStyle.current.copy(
                fontFamily = FontFamily.Monospace, 
                color = if (selectedId != null) Color.Cyan else Color.Gray,
                fontSize = 13.sp
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Cyan,
                unfocusedBorderColor = Color.DarkGray
            ),
            shape = MaterialTheme.shapes.extraSmall
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color(0xFF0F0F0F))
        ) {
            DropdownMenuItem(
                text = { Text("--- DISCONNECT_LINK ---", fontFamily = FontFamily.Monospace, color = Color.Gray, fontSize = 12.sp) },
                onClick = {
                    onItemSelected(null)
                    expanded = false
                }
            )
            items.forEach { item ->
                DropdownMenuItem(
                    text = { 
                        Text(
                            text = "${item.name.uppercase()} [${item.currentQuantity} ${item.unit}]",
                            fontFamily = FontFamily.Monospace,
                            color = Color.White,
                            fontSize = 12.sp
                        ) 
                    },
                    onClick = {
                        onItemSelected(item.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskTypeSelector(
    selectedType: TaskType,
    onTypeSelected: (TaskType) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TaskType.values().forEach { type ->
            val isSelected = type == selectedType
            val icon = when (type) {
                TaskType.GENERAL -> Icons.Default.TaskAlt
                TaskType.SHOPPING -> Icons.Default.ShoppingBag
                TaskType.EVENT -> Icons.Default.Event
                TaskType.INVENTORY -> Icons.Default.Inventory
                TaskType.FERIA -> Icons.Default.Storefront
            }
            
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(
                    onClick = { onTypeSelected(type) },
                    modifier = Modifier.background(
                        if (isSelected) Color.Cyan.copy(alpha = 0.2f) else Color.Transparent,
                        shape = MaterialTheme.shapes.extraSmall
                    )
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = type.name,
                        tint = if (isSelected) Color.Cyan else Color.Gray
                    )
                }
                Text(
                    text = if (type == TaskType.FERIA) "FERI" else type.name.take(4),
                    fontSize = 10.sp,
                    color = if (isSelected) Color.Cyan else Color.Gray,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskStatusSelector(
    selectedStatus: TaskStatus,
    onStatusSelected: (TaskStatus) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedStatus.name,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, color = Color.Yellow),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.DarkGray,
                focusedBorderColor = Color.Yellow,
                unfocusedTrailingIconColor = Color.Gray,
                focusedTrailingIconColor = Color.Yellow
            ),
            shape = MaterialTheme.shapes.extraSmall
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color(0xFF0F0F0F))
        ) {
            TaskStatus.values().forEach { status ->
                DropdownMenuItem(
                    text = { 
                        Text(
                            text = status.name,
                            fontFamily = FontFamily.Monospace,
                            color = if (status == selectedStatus) Color.Yellow else Color.White,
                            fontSize = 12.sp
                        ) 
                    },
                    onClick = {
                        onStatusSelected(status)
                        expanded = false
                    },
                    colors = MenuDefaults.itemColors()
                )
            }
        }
    }
}
