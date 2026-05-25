package com.hindrax.ss.presentation.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hindrax.ss.domain.tasks.model.InventoryItem
import com.hindrax.ss.presentation.tasks.AsciiBanners

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    onBack: () -> Unit,
    viewModel: InventoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddItemDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val visibleItems = remember(uiState.items, searchQuery) {
        uiState.items.filterByLogisticsSearch(searchQuery)
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("STRATEGIC_RESOURCES_INVENTORY", fontFamily = FontFamily.Monospace) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Green)
                    }
                },
                actions = {
                    IconButton(onClick = { showAddItemDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Item", tint = Color.Green)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0A0A0A),
                    titleContentColor = Color.Green
                ),
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddItemDialog = true },
                containerColor = Color.Green,
                contentColor = Color.Black,
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Item")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFF0A0A0A)),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ASCII Banner
            item {
                Text(
                    text = AsciiBanners.INVENTORY_HEADER,
                    color = Color.Cyan,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp,
                    lineHeight = 9.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                LogisticsSearchBox(
                    query = searchQuery,
                    totalItems = uiState.items.size,
                    visibleItems = visibleItems.size,
                    onQueryChange = { searchQuery = it },
                    onClear = { searchQuery = "" }
                )
            }

            when {
                uiState.isLoading -> {
                    item {
                        Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color.Green)
                        }
                    }
                }
                uiState.items.isEmpty() -> {
                    item {
                        Column(
                        modifier = Modifier.fillParentMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Inventory, contentDescription = null, tint = Color.DarkGray, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "NO_RESOURCES_DETECTED",
                            color = Color.Gray,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    }
                }
                visibleItems.isEmpty() -> {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = Color.DarkGray,
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "SIN_PRODUCTOS_PARA: ${searchQuery.trim()}",
                                color = Color.Gray,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
                else -> {
                    items(visibleItems) { item ->
                        InventoryItemCard(
                            item = item,
                            onUpdateQty = { delta -> viewModel.updateQuantity(item.id, delta) },
                            onDelete = { viewModel.deleteItem(item) }
                        )
                    }
                }
            }

            if (uiState.error != null) {
                item {
                    Text(
                        text = "SYSTEM_ERROR: ${uiState.error}",
                        color = Color.Red,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    if (showAddItemDialog) {
        AddItemDialog(
            onDismiss = { showAddItemDialog = false },
            onConfirm = { name, category, minQty, unit ->
                viewModel.addItem(name, category, minQty, unit)
                showAddItemDialog = false
            }
        )
    }
}

@Composable
private fun LogisticsSearchBox(
    query: String,
    totalItems: Int,
    visibleItems: Int,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF101010), MaterialTheme.shapes.extraSmall)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = Color.Cyan)
            },
            trailingIcon = {
                if (query.isNotBlank()) {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Default.Close, contentDescription = "Limpiar busqueda", tint = Color.Gray)
                    }
                }
            },
            label = { Text("BUSCAR_PRODUCTO", fontFamily = FontFamily.Monospace) },
            placeholder = {
                Text("nombre / categoria / unidad / cantidad", fontFamily = FontFamily.Monospace)
            },
            textStyle = LocalTextStyle.current.copy(
                fontFamily = FontFamily.Monospace,
                color = Color.White
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Cyan,
                unfocusedBorderColor = Color.DarkGray,
                cursorColor = Color.Cyan,
                focusedLabelColor = Color.Cyan,
                unfocusedLabelColor = Color.Gray
            ),
            shape = MaterialTheme.shapes.extraSmall
        )
        Text(
            text = if (query.isBlank()) {
                "PRODUCTOS_INDEXADOS: $totalItems"
            } else {
                "RESULTADOS: $visibleItems / $totalItems"
            },
            color = if (query.isBlank()) Color.Gray else Color.Cyan,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp
        )
    }
}

@Composable
fun InventoryItemCard(
    item: InventoryItem,
    onUpdateQty: (Double) -> Unit,
    onDelete: () -> Unit
) {
    val isLow = item.currentQuantity <= item.minQuantity
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, 
            if (isLow) Color.Red.copy(alpha = 0.5f) else Color.DarkGray
        ),
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = item.name.uppercase(),
                        color = if (isLow) Color.Red else Color.Green,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "CAT: ${item.category}",
                        color = Color.Cyan,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.DarkGray, modifier = Modifier.size(18.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${item.currentQuantity}",
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = item.unit,
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onUpdateQty(-1.0) }) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = Color.White)
                    }
                    IconButton(onClick = { onUpdateQty(1.0) }) {
                        Icon(Icons.Default.Add, contentDescription = "Increase", tint = Color.Green)
                    }
                }
            }
            
            if (isLow) {
                Text(
                    text = "! CRITICAL_LEVEL_DETECTED (MIN: ${item.minQuantity})",
                    color = Color.Red,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun AddItemDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Double, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("DESPENSA") }
    var minQty by remember { mutableStateOf("1.0") }
    var unit by remember { mutableStateOf("unid") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1A),
        title = { Text("REGISTER_NEW_RESOURCE", color = Color.Green, fontFamily = FontFamily.Monospace) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("RESOURCE_NAME", fontFamily = FontFamily.Monospace) },
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Green)
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("CATEGORY", fontFamily = FontFamily.Monospace) },
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Green)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = minQty,
                        onValueChange = { minQty = it },
                        label = { Text("MIN_ALERT", fontFamily = FontFamily.Monospace) },
                        modifier = Modifier.weight(1f),
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, color = Color.White),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Green)
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("UNIT", fontFamily = FontFamily.Monospace) },
                        modifier = Modifier.weight(1f),
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, color = Color.White),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Green)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, category, minQty.toDoubleOrNull() ?: 0.0, unit) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Green, contentColor = Color.Black)
            ) {
                Text("REGISTER", fontFamily = FontFamily.Monospace)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = Color.Gray, fontFamily = FontFamily.Monospace)
            }
        }
    )
}

private fun List<InventoryItem>.filterByLogisticsSearch(query: String): List<InventoryItem> {
    val terms = query.trim()
        .lowercase()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
    if (terms.isEmpty()) return this

    return filter { item ->
        val searchable = buildString {
            append(item.name).append(' ')
            append(item.category).append(' ')
            append(item.unit).append(' ')
            append(item.currentQuantity).append(' ')
            append(item.minQuantity)
        }.lowercase()
        terms.all { term -> searchable.contains(term) }
    }
}
