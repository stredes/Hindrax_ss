package com.hindrax.ss.presentation.tasks

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hindrax.ss.data.entity.PeerEntity
import com.hindrax.ss.domain.tasks.model.EventScheduleFormatter
import com.hindrax.ss.domain.tasks.model.InventoryItem
import com.hindrax.ss.domain.tasks.model.ShoppingChecklistSelector
import com.hindrax.ss.domain.tasks.model.TaskStatus
import com.hindrax.ss.domain.tasks.model.TaskType
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskFormScreen(
    taskId: Long,
    onBack: () -> Unit,
    viewModel: TaskFormViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current

    LaunchedEffect(taskId) {
        viewModel.loadTask(taskId)
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onBack()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Green)
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

                if (uiState.type == TaskType.EVENT) {
                    EventSchedulePanel(
                        scheduledTime = uiState.scheduledTime,
                        title = uiState.title,
                        description = uiState.description,
                        locationName = uiState.locationName,
                        onScheduledTimeChange = viewModel::onScheduledTimeChange,
                        onSyncCalendar = {
                            openCalendarInsert(
                                context = context,
                                title = uiState.title,
                                description = uiState.description,
                                locationName = uiState.locationName,
                                scheduledTime = uiState.scheduledTime
                            )
                        }
                    )
                }

                Text(
                    text = "--- DESIGNATED_DEVICE ---",
                    color = Color.Cyan,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )

                AssignedDeviceSelector(
                    peers = uiState.availablePeers,
                    selectedPeerId = uiState.assignedPeerId,
                    onPeerSelected = viewModel::onAssignedPeerChange
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
                        inventoryItems = uiState.availableInventory,
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
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun ChecklistEditor(
    items: List<com.hindrax.ss.domain.tasks.model.ChecklistItem>,
    inventoryItems: List<InventoryItem> = emptyList(),
    onAddItem: (String, Double?, String?) -> Unit,
    onRemoveItem: (String) -> Unit,
    onToggleItem: (String) -> Unit,
    onUpdateItem: (String, String?, Double?, String?) -> Unit
) {
    var newItemText by remember { mutableStateOf("") }
    var newItemQty by remember { mutableStateOf("") }
    var newItemUnit by remember { mutableStateOf("unid") }
    var inventoryAccordionOpen by remember { mutableStateOf(false) }
    var selectedInventoryItem by remember(inventoryItems) { mutableStateOf<InventoryItem?>(null) }
    var selectedInventoryQty by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (inventoryItems.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F0F)),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (selectedInventoryItem != null) Color.Green else Color.DarkGray
                ),
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "PRODUCTO_GUARDADO",
                                color = Color.Yellow,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            )
                            Text(
                                text = selectedInventoryItem?.let { "${it.name.uppercase()} :: ${it.currentQuantity} ${it.unit}" }
                                    ?: "[ SELECCIONAR_PRODUCTO_EXISTENTE ]",
                                color = if (selectedInventoryItem != null) Color.White else Color.Gray,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        }
                        IconButton(onClick = { inventoryAccordionOpen = !inventoryAccordionOpen }) {
                            Icon(
                                imageVector = if (inventoryAccordionOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Toggle inventory accordion",
                                tint = Color.Cyan
                            )
                        }
                    }

                    if (inventoryAccordionOpen) {
                        inventoryItems.forEach { inventory ->
                            OutlinedButton(
                                onClick = {
                                    selectedInventoryItem = inventory
                                    selectedInventoryQty = ""
                                    inventoryAccordionOpen = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.extraSmall,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (selectedInventoryItem?.id == inventory.id) Color.Green else Color.White
                                )
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = inventory.name.uppercase(),
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = "STOCK: ${inventory.currentQuantity} ${inventory.unit}",
                                            color = Color.Cyan,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.sp
                                        )
                                    }
                                    if (selectedInventoryItem?.id == inventory.id) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.Green)
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = selectedInventoryQty,
                            onValueChange = { selectedInventoryQty = it },
                            modifier = Modifier.weight(1f),
                            enabled = selectedInventoryItem != null,
                            label = { Text("QTY_SELECTED", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(
                                fontFamily = FontFamily.Monospace,
                                color = Color.White,
                                fontSize = 14.sp
                            ),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Cyan),
                            shape = MaterialTheme.shapes.extraSmall
                        )
                        Text(
                            text = selectedInventoryItem?.unit ?: "unit",
                            color = Color.Cyan,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            modifier = Modifier.widthIn(min = 42.dp)
                        )
                        IconButton(
                            onClick = {
                                val inventory = selectedInventoryItem ?: return@IconButton
                                val quantity = ShoppingChecklistSelector.parseQuantity(selectedInventoryQty) ?: return@IconButton
                                onAddItem(inventory.name, quantity, inventory.unit)
                                selectedInventoryItem = null
                                selectedInventoryQty = ""
                            },
                            enabled = selectedInventoryItem != null &&
                                ShoppingChecklistSelector.parseQuantity(selectedInventoryQty) != null,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.Default.AddCircle,
                                contentDescription = "Add selected inventory item",
                                tint = if (
                                    selectedInventoryItem != null &&
                                    ShoppingChecklistSelector.parseQuantity(selectedInventoryQty) != null
                                ) Color.Green else Color.DarkGray
                            )
                        }
                    }
                }
            }
        }

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
                    label = { Text("NUEVO_PRODUCTO", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                    placeholder = { Text("Ej: Cerveza Austral", fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
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
                        label = { Text("Cantidad", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
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
                        label = { Text("Unidad", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, color = Color.White, fontSize = 14.sp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Cyan),
                        shape = MaterialTheme.shapes.extraSmall
                    )
                    IconButton(
                        onClick = {
                            if (newItemText.isNotBlank()) {
                                onAddItem(
                                    newItemText,
                                    ShoppingChecklistSelector.parseQuantity(newItemQty),
                                    newItemUnit
                                )
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
            value = selectedItem?.name ?: "[ VINCULAR_PRODUCTO_DEL_INVENTARIO ]",
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
fun AssignedDeviceSelector(
    peers: List<PeerEntity>,
    selectedPeerId: String?,
    onPeerSelected: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedPeer = peers.find { it.id == selectedPeerId }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedPeer?.displayName ?: "[ ALL_SYNCED_DEVICES ]",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(
                    fontFamily = FontFamily.Monospace,
                    color = if (selectedPeer != null) Color.Green else Color.Gray,
                    fontSize = 13.sp
                ),
                supportingText = {
                    Text(
                        text = selectedPeer?.id ?: "Sin dispositivo designado: todos los peers reciben la mision por sincronizacion.",
                        color = Color.DarkGray,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Green,
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
                    text = { Text("--- ALL_SYNCED_DEVICES ---", fontFamily = FontFamily.Monospace, color = Color.Gray, fontSize = 12.sp) },
                    onClick = {
                        onPeerSelected(null)
                        expanded = false
                    }
                )
                peers.forEach { peer ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(peer.displayName.uppercase(), fontFamily = FontFamily.Monospace, color = Color.Green, fontSize = 12.sp)
                                Text(peer.id, fontFamily = FontFamily.Monospace, color = Color.Gray, fontSize = 10.sp)
                            }
                        },
                        onClick = {
                            onPeerSelected(peer.id)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventSchedulePanel(
    scheduledTime: Long?,
    title: String,
    description: String,
    locationName: String?,
    onScheduledTimeChange: (Long?) -> Unit,
    onSyncCalendar: () -> Unit
) {
    val context = LocalContext.current
    val calendar = remember(scheduledTime) {
        Calendar.getInstance().apply {
            timeInMillis = scheduledTime ?: System.currentTimeMillis()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "--- EVENT_DATE ---",
            color = Color.Cyan,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
        OutlinedTextField(
            value = EventScheduleFormatter.format(scheduledTime),
            onValueChange = {},
            readOnly = true,
            label = { Text("EVENT_DATE_LABEL", fontFamily = FontFamily.Monospace) },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = { Icon(Icons.Default.Event, contentDescription = null, tint = Color.Cyan) },
            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, color = Color.White),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Cyan),
            shape = MaterialTheme.shapes.extraSmall
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = {
                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            val updated = Calendar.getInstance().apply {
                                timeInMillis = scheduledTime ?: System.currentTimeMillis()
                                set(Calendar.YEAR, year)
                                set(Calendar.MONTH, month)
                                set(Calendar.DAY_OF_MONTH, day)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            onScheduledTimeChange(updated.timeInMillis)
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                    ).show()
                },
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Icon(Icons.Default.DateRange, contentDescription = null, tint = Color.Cyan)
                Spacer(modifier = Modifier.width(6.dp))
                Text("DATE", fontFamily = FontFamily.Monospace)
            }
            OutlinedButton(
                onClick = {
                    TimePickerDialog(
                        context,
                        { _, hour, minute ->
                            val updated = Calendar.getInstance().apply {
                                timeInMillis = scheduledTime ?: System.currentTimeMillis()
                                set(Calendar.HOUR_OF_DAY, hour)
                                set(Calendar.MINUTE, minute)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            onScheduledTimeChange(updated.timeInMillis)
                        },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        true
                    ).show()
                },
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = Color.Cyan)
                Spacer(modifier = Modifier.width(6.dp))
                Text("TIME", fontFamily = FontFamily.Monospace)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onSyncCalendar,
                enabled = scheduledTime != null && title.isNotBlank(),
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.extraSmall,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Cyan, contentColor = Color.Black)
            ) {
                Icon(Icons.Default.EventAvailable, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("SYNC_CALENDAR", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
            OutlinedButton(
                onClick = { onScheduledTimeChange(null) },
                enabled = scheduledTime != null,
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Text("CLEAR_DATE", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
        }
        if (title.isBlank()) {
            Text("EVENT_TITLE_REQUIRED_FOR_CALENDAR", color = Color.Yellow, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
        }
    }
}

private fun openCalendarInsert(
    context: Context,
    title: String,
    description: String,
    locationName: String?,
    scheduledTime: Long?
) {
    val start = scheduledTime ?: return
    val intent = Intent(Intent.ACTION_INSERT).apply {
        data = CalendarContract.Events.CONTENT_URI
        putExtra(CalendarContract.Events.TITLE, title)
        putExtra(CalendarContract.Events.DESCRIPTION, description)
        putExtra(CalendarContract.Events.EVENT_LOCATION, locationName.orEmpty())
        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start)
        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, start + 60 * 60 * 1000)
    }
    runCatching {
        context.startActivity(intent)
    }.onFailure {
        Toast.makeText(context, "CALENDAR_APP_NOT_AVAILABLE", Toast.LENGTH_SHORT).show()
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
