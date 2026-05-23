package com.hindrax.ss.features.termux

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
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
import com.hindrax.ss.domain.tools.ToolCatalogItem
import com.hindrax.ss.domain.tools.ToolRiskLevel
import com.hindrax.ss.domain.tools.ToolWorkflowPreset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermuxScriptsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as HindraxApplication
    val viewModel: TermuxScriptsViewModel = viewModel(
        factory = TermuxScriptsViewModelFactory(app.auditRepository)
    )
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LaunchedEffect(Unit) {
        viewModel.checkTermux(context)
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("Terminal y herramientas", fontFamily = FontFamily.Monospace) },
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
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color(0xFF050505))
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!uiState.isTermuxInstalled) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF330000)),
                        modifier = Modifier.fillMaxWidth(),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red),
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            text = "[!] Termux no detectado. Instala Termux y configura el puente para ejecutar herramientas.",
                            modifier = Modifier.padding(16.dp),
                            color = Color.Red,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            item {
                Text(
                    text = "--- Flujo guiado de herramientas ---",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = Color.Cyan
                )
            }

            item {
                TerminalGuidePanel(isTermuxInstalled = uiState.isTermuxInstalled)
            }

            item {
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = viewModel::onQueryChange,
                    label = { Text("Buscar herramienta", fontFamily = FontFamily.Monospace) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Ej: nmap, apktool, binwalk, curl...", color = Color.DarkGray) },
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, color = Color.Green),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color.DarkGray,
                        focusedBorderColor = Color.Green,
                        cursorColor = Color.Green
                    ),
                    shape = MaterialTheme.shapes.extraSmall
                )
            }

            item {
                PresetWorkflowSelector(
                    presets = uiState.presets,
                    onPresetSelected = { viewModel.applyWorkflowPreset(it.id) }
                )
            }

            item {
                Text(
                    text = "Categorías",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = Color.Cyan
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.categories) { category ->
                        FilterChip(
                            selected = uiState.selectedCategoryId == category.id && uiState.query.isBlank(),
                            onClick = { viewModel.selectCategory(category.id) },
                            label = {
                                Text(category.name, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color.Green,
                                selectedLabelColor = Color.Black,
                                labelColor = Color.Green
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = uiState.selectedCategoryId == category.id && uiState.query.isBlank(),
                                borderColor = Color.DarkGray,
                                selectedBorderColor = Color.Green
                            )
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = uiState.target,
                    onValueChange = viewModel::onTargetChange,
                    label = { Text("Objetivo autorizado o archivo", fontFamily = FontFamily.Monospace) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Ej: 192.168.1.1 / dominio.com / archivo.apk", color = Color.DarkGray) },
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, color = Color.Green),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color.DarkGray,
                        focusedBorderColor = Color.Green,
                        cursorColor = Color.Green
                    ),
                    shape = MaterialTheme.shapes.extraSmall
                )
            }

            item {
                OutlinedTextField(
                    value = uiState.customArguments,
                    onValueChange = viewModel::onCustomArgumentsChange,
                    label = { Text("Argumentos manuales para herramienta individual", fontFamily = FontFamily.Monospace) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    placeholder = { Text("Opcional. Ej: -sV 192.168.1.10", color = Color.DarkGray) },
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, color = Color.Cyan, fontSize = 12.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color.DarkGray,
                        focusedBorderColor = Color.Cyan,
                        cursorColor = Color.Cyan
                    ),
                    shape = MaterialTheme.shapes.extraSmall
                )
            }

            item {
                Text(
                    text = "Herramientas visibles: ${uiState.visibleTools.size}",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = Color.Gray
                )
            }

            items(uiState.visibleTools) { tool ->
                ToolCatalogListItem(
                    tool = tool,
                    isSelected = uiState.selectedTool?.command == tool.command,
                    onClick = { viewModel.selectTool(tool) }
                )
            }

            uiState.selectedTool?.let { tool ->
                item {
                    ToolExecutionPanel(
                        tool = tool,
                        commandPreview = uiState.commandPreview,
                        authorizationConfirmed = uiState.authorizationConfirmed,
                        onAuthorizationConfirmedChange = viewModel::onAuthorizationConfirmedChange,
                        onInstallPackage = { viewModel.installSelectedToolPackage(context) },
                        canInstallPackage = uiState.isTermuxInstalled && tool.termuxPackage != null,
                        onAddToWorkflow = viewModel::addSelectedToolToWorkflow
                    )
                }
            }

            item {
                ToolWorkflowPanel(
                    tools = uiState.workflowTools,
                    workflowPreview = uiState.workflowPreview,
                    canRunWorkflow = uiState.canRunWorkflow,
                    onRemoveTool = viewModel::removeWorkflowTool,
                    onClearWorkflow = viewModel::clearWorkflow,
                    onRunWorkflow = { viewModel.executeWorkflow(context) }
                )
            }

            item {
                Button(
                    onClick = {
                        viewModel.executeSelectedTool(context)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.canRunSelectedTool,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Green, contentColor = Color.Black),
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ejecutar herramienta seleccionada", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }

            item {
                Text("--- Registro del sistema ---", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = Color.Cyan)
            }
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(Color(0xFF0A0A0A))
                        .border(1.dp, Color.DarkGray)
                        .padding(8.dp)
                ) {
                    Text(
                        text = uiState.logs,
                        color = Color.Green,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun TerminalGuidePanel(isTermuxInstalled: Boolean) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isTermuxInstalled) Color.Green else Color.Yellow),
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = if (isTermuxInstalled) "Termux listo para ejecutar" else "Termux pendiente",
                color = if (isTermuxInstalled) Color.Green else Color.Yellow,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Text(
                text = "1. Escribe el objetivo autorizado. 2. Elige un flujo recomendado o una herramienta. 3. Revisa la vista previa. 4. Ejecuta una herramienta o la cadena completa.",
                color = Color.Gray,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
private fun PresetWorkflowSelector(
    presets: List<ToolWorkflowPreset>,
    onPresetSelected: (ToolWorkflowPreset) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Flujos recomendados",
            color = Color.Cyan,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(presets) { preset ->
                AssistChip(
                    onClick = { onPresetSelected(preset) },
                    label = {
                        Column {
                            Text(preset.nombre, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                            Text("${preset.tools.size} pasos", fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                        }
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        labelColor = Color.Green,
                        containerColor = Color(0xFF0A0A0A)
                    ),
                    border = AssistChipDefaults.assistChipBorder(
                        enabled = true,
                        borderColor = Color.Green.copy(alpha = 0.45f)
                    )
                )
            }
        }
    }
}

@Composable
fun ToolCatalogListItem(tool: ToolCatalogItem, isSelected: Boolean, onClick: () -> Unit) {
    val riskColor = when (tool.riskLevel) {
        ToolRiskLevel.LOW -> Color.Green
        ToolRiskLevel.MEDIUM -> Color.Yellow
        ToolRiskLevel.HIGH -> Color.Red
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF002200) else Color(0xFF111111)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, 
            if (isSelected) Color.Green else Color.DarkGray
        ),
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = tool.displayName.uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (isSelected) Color.Green else Color.White
                )
                Text(
                    text = tool.riskLevel.name,
                    color = riskColor,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = tool.tutorial.authorizedUse,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = Color.Gray
            )
            Text(
                text = "Modo: ${tool.executionMode}  Comando: ${tool.command}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Cyan,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun ToolExecutionPanel(
    tool: ToolCatalogItem,
    commandPreview: String,
    authorizationConfirmed: Boolean,
    onAuthorizationConfirmedChange: (Boolean) -> Unit,
    onInstallPackage: () -> Unit,
    canInstallPackage: Boolean,
    onAddToWorkflow: () -> Unit
) {
    val riskColor = when (tool.riskLevel) {
        ToolRiskLevel.LOW -> Color.Green
        ToolRiskLevel.MEDIUM -> Color.Yellow
        ToolRiskLevel.HIGH -> Color.Red
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
        border = androidx.compose.foundation.BorderStroke(1.dp, riskColor),
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("--- Vista previa del comando ---", color = riskColor, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(commandPreview, color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            OutlinedButton(
                onClick = onAddToWorkflow,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraSmall,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Green)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.Green)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Agregar a la cadena", color = Color.Green, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
            tool.termuxPackage?.let { packageName ->
                Text(
                    "Paquete Termux: $packageName",
                    color = Color.Cyan,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )
                OutlinedButton(
                    onClick = onInstallPackage,
                    enabled = canInstallPackage,
                    shape = MaterialTheme.shapes.extraSmall,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Cyan),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Instalar paquete de la herramienta", color = Color.Cyan, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }
            Text(tool.tutorial.notes.joinToString("  "), color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
            if (tool.riskLevel == ToolRiskLevel.HIGH) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = authorizationConfirmed,
                        onCheckedChange = onAuthorizationConfirmedChange,
                        colors = CheckboxDefaults.colors(checkedColor = Color.Red)
                    )
                    Text(
                        "Confirmo que tengo autorización explícita",
                        color = Color.Red,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolWorkflowPanel(
    tools: List<ToolCatalogItem>,
    workflowPreview: String,
    canRunWorkflow: Boolean,
    onRemoveTool: (String) -> Unit,
    onClearWorkflow: () -> Unit,
    onRunWorkflow: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Cyan.copy(alpha = 0.7f)),
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "--- Cadena de trabajo ---",
                    color = Color.Cyan,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onClearWorkflow, enabled = tools.isNotEmpty()) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear workflow", tint = if (tools.isNotEmpty()) Color.Red else Color.DarkGray)
                }
            }

            if (tools.isEmpty()) {
                Text(
                    text = "Selecciona herramientas del catálogo o toca un flujo recomendado para crear una cadena.",
                    color = Color.Gray,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )
            } else {
                tools.forEachIndexed { index, tool ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "${index + 1}. ${tool.command}",
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onRemoveTool(tool.command) }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Remove workflow step", tint = Color.Red)
                        }
                    }
                }
                Text(
                    text = workflowPreview,
                    color = Color.Green,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )
            }

            Button(
                onClick = onRunWorkflow,
                enabled = canRunWorkflow,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Cyan, contentColor = Color.Black),
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ejecutar cadena completa", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
        }
    }
}
