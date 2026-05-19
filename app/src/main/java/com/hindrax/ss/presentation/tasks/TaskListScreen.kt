package com.hindrax.ss.presentation.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.hilt.navigation.compose.hiltViewModel
import com.hindrax.ss.domain.tasks.model.Task
import com.hindrax.ss.domain.tasks.model.TaskStatus
import com.hindrax.ss.domain.tasks.model.TaskType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    onNavigateToCreate: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    viewModel: TaskListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFilterDialog by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val isTablet = configuration.smallestScreenWidthDp >= 600
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val useGrid = isTablet && isLandscape
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("COMMAND_QUEUE_INTERFACE", fontFamily = FontFamily.Monospace) },
                actions = {
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter", tint = Color.Green)
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
                onClick = onNavigateToCreate,
                containerColor = Color.Green,
                contentColor = Color.Black,
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Mission")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFF050505))
        ) {
            if (uiState.isLoading) {
                Column(modifier = Modifier.fillMaxSize()) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Color.Green, trackColor = Color(0xFF1A1A1A))
                }
            } else if (uiState.tasks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("[ ! ]", color = Color.Red, fontFamily = FontFamily.Monospace, fontSize = 24.sp)
                        Text("ZERO_TASKS_DETECTED", color = Color.Red, fontFamily = FontFamily.Monospace)
                    }
                }
            } else {
                if (useGrid) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 16.dp,
                            bottom = 80.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                            Text(
                                text = AsciiBanners.TASKS_MODULE,
                                color = Color.Green,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 8.sp,
                                lineHeight = 9.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                            TaskSummarySection(uiState.tasks)
                        }
                        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                            OutlinedTextField(
                                value = uiState.searchQuery,
                                onValueChange = viewModel::onSearchQueryChange,
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("CMD> query_database...", color = Color.DarkGray, fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
                                leadingIcon = { Text(" $ ", color = Color.Green, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
                                trailingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Green) },
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, color = Color.Green, fontSize = 14.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = Color.DarkGray,
                                    focusedBorderColor = Color.Green,
                                    cursorColor = Color.Green
                                ),
                                shape = MaterialTheme.shapes.extraSmall
                            )
                        }
                        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        items(uiState.tasks) { task ->
                            TaskTerminalItem(task = task, onClick = { onNavigateToDetail(task.id) })
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 16.dp,
                            bottom = 80.dp
                        )
                    ) {
                        item {
                            Text(
                                text = AsciiBanners.TASKS_MODULE,
                                color = Color.Green,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 8.sp,
                                lineHeight = 9.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            TaskSummarySection(uiState.tasks)
                        }
                        item {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        item {
                            OutlinedTextField(
                                value = uiState.searchQuery,
                                onValueChange = viewModel::onSearchQueryChange,
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("CMD> query_database...", color = Color.DarkGray, fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
                                leadingIcon = { Text(" $ ", color = Color.Green, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
                                trailingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Green) },
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, color = Color.Green, fontSize = 14.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = Color.DarkGray,
                                    focusedBorderColor = Color.Green,
                                    cursorColor = Color.Green
                                ),
                                shape = MaterialTheme.shapes.extraSmall
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        items(uiState.tasks) { task ->
                            TaskTerminalItem(task = task, onClick = { onNavigateToDetail(task.id) })
                        }
                    }
                }
            }
        }
    }

    if (showFilterDialog) {
        FilterDialog(
            selectedStatus = uiState.selectedStatus,
            onStatusSelected = {
                viewModel.onStatusFilterChange(it)
                showFilterDialog = false
            },
            onDismiss = { showFilterDialog = false }
        )
    }
}

@Composable
fun TaskSummarySection(tasks: List<Task>) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SummaryCard("TOTAL", tasks.size.toString(), Modifier.weight(1f), Color.Cyan)
        val activeCount = tasks.count { it.status == TaskStatus.PENDIENTE || it.status == TaskStatus.EN_PROGRESO }
        SummaryCard("ACTIVE", activeCount.toString(), Modifier.weight(1f), Color.Yellow)
        val doneCount = tasks.count { it.status == TaskStatus.COMPLETADA }
        SummaryCard("DONE", doneCount.toString(), Modifier.weight(1f), Color.Green)
    }
}

@Composable
fun SummaryCard(label: String, value: String, modifier: Modifier = Modifier, accentColor: Color) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.DarkGray),
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            Text(text = value.padStart(2, '0'), color = accentColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun TaskTerminalItem(task: Task, onClick: () -> Unit) {
    val statusColor = when (task.status) {
        TaskStatus.COMPLETADA -> Color.Green
        TaskStatus.EN_PROGRESO -> Color.Cyan
        TaskStatus.PENDIENTE -> Color.Yellow
        TaskStatus.PAUSADA -> Color.Gray
        TaskStatus.CANCELADA -> Color.Red
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F0F)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.DarkGray),
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Type Icon
            val typeIcon = when(task.type) {
                TaskType.SHOPPING -> Icons.Default.ShoppingBag
                TaskType.FERIA -> Icons.Default.Storefront
                else -> Icons.Default.TaskAlt
            }
            Icon(typeIcon, contentDescription = null, tint = statusColor, modifier = Modifier.size(18.dp))
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    maxLines = 1
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "[${task.status.name}]",
                        color = statusColor,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                    if (task.checklist.isNotEmpty()) {
                        val done = task.checklist.count { it.isChecked }
                        Text(
                            text = " | DATA: $done/${task.checklist.size}",
                            color = Color.Gray,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    }
                }
            }
            Text(text = "»", color = Color.Green, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun FilterDialog(
    selectedStatus: TaskStatus?,
    onStatusSelected: (TaskStatus?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0A0A0A),
        title = { Text("FILTER_PARAMETERS", color = Color.Green, fontFamily = FontFamily.Monospace) },
        text = {
            Column {
                FilterOption("ALL_RECORDS", selectedStatus == null) { onStatusSelected(null) }
                TaskStatus.values().forEach { status ->
                    FilterOption(status.name, selectedStatus == status) { onStatusSelected(status) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("CLOSE", color = Color.Green, fontFamily = FontFamily.Monospace) }
        }
    )
}

@Composable
fun FilterOption(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = null,
            colors = RadioButtonDefaults.colors(selectedColor = Color.Green, unselectedColor = Color.DarkGray)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, color = if (isSelected) Color.Green else Color.White, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
    }
}
