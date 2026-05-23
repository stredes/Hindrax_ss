package com.hindrax.ss.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val viewModel: SettingsViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LaunchedEffect(Unit) {
        viewModel.loadSettings(context)
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("CORE_SYSTEM_SETTINGS", fontFamily = FontFamily.Monospace) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Green)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ThemeEditorPanel(
                    state = uiState,
                    onNameChange = { viewModel.updateThemeName(context, it) },
                    onColorChange = { key, value -> viewModel.updateThemeColor(context, key, value) },
                    onReset = { viewModel.resetTheme(context) },
                    onCopy = { clipboardManager.setText(AnnotatedString(uiState.themeExport)) },
                    onImportDraftChange = viewModel::updateThemeImportDraft,
                    onImport = { viewModel.importTheme(context) }
                )
            }

            item {
                Text(
                    text = "--- AI_CONFIGURATION (OPENAI) ---",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = Color.Cyan
                )
                Spacer(modifier = Modifier.height(12.dp))
                ApiKeyInput(
                    name = "OpenAI",
                    value = uiState.apiKeys["OpenAI"].orEmpty(),
                    onValueChange = { viewModel.updateApiKey(context, "OpenAI", it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.openAiModel,
                    onValueChange = { viewModel.updateOpenAiModel(context, it) },
                    label = { Text("OPENAI_MODEL", fontFamily = FontFamily.Monospace) },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Psychology, contentDescription = null, tint = Color.Green) },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, color = Color.Green),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color.DarkGray,
                        focusedBorderColor = Color.Green
                    ),
                    shape = MaterialTheme.shapes.extraSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Responses API / Tool Calling / Safety Gate activo.",
                    color = Color.Gray,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )
            }

            item {
                Text(
                    text = "--- AI_FALLBACK (OLLAMA_LITE) ---",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = Color.Cyan
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("USE_OLLAMA_ON_QUOTA_LIMIT", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                        Text("Fallback local cuando OpenAI devuelve cuota/rate-limit.", color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                    }
                    Switch(
                        checked = uiState.ollamaFallbackEnabled,
                        onCheckedChange = { viewModel.toggleOllamaFallback(context, it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.Cyan, checkedTrackColor = Color.Cyan.copy(alpha = 0.45f))
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = uiState.ollamaBaseUrl,
                    onValueChange = { viewModel.updateOllamaBaseUrl(context, it) },
                    label = { Text("OLLAMA_BASE_URL", fontFamily = FontFamily.Monospace) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, color = Color.Cyan),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color.DarkGray,
                        focusedBorderColor = Color.Cyan,
                        cursorColor = Color.Cyan
                    ),
                    shape = MaterialTheme.shapes.extraSmall
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Telefono fisico: usa http://IP_DEL_PC:11434. Emulador: http://10.0.2.2:11434.",
                    color = if (uiState.ollamaBaseUrl.contains("10.0.2.2")) Color.Yellow else Color.Gray,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    lineHeight = 13.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.ollamaModel,
                    onValueChange = { viewModel.updateOllamaModel(context, it) },
                    label = { Text("OLLAMA_LITE_MODEL", fontFamily = FontFamily.Monospace) },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Psychology, contentDescription = null, tint = Color.Cyan) },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, color = Color.Cyan),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color.DarkGray,
                        focusedBorderColor = Color.Cyan,
                        cursorColor = Color.Cyan
                    ),
                    shape = MaterialTheme.shapes.extraSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OllamaPresetButton("gemma3:1b", Modifier.weight(1f)) { viewModel.updateOllamaModel(context, it) }
                    OllamaPresetButton("llama3.2:1b", Modifier.weight(1f)) { viewModel.updateOllamaModel(context, it) }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OllamaPresetButton("qwen2.5:0.5b", Modifier.weight(1f)) { viewModel.updateOllamaModel(context, it) }
                    OllamaPresetButton("phi3:mini", Modifier.weight(1f)) { viewModel.updateOllamaModel(context, it) }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { viewModel.pullOllamaModel(context) },
                    enabled = !uiState.isPullingOllamaModel && uiState.ollamaBaseUrl.isNotBlank() && uiState.ollamaModel.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Cyan, contentColor = Color.Black),
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (uiState.isPullingOllamaModel) "DOWNLOADING_MODEL" else "DOWNLOAD_SELECTED_MODEL",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
                uiState.ollamaPullStatus?.let { status ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = status,
                        color = if (status.contains("ERROR")) Color.Red else Color.Cyan,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "El modelo se descarga en el servidor Ollama configurado, no dentro del APK.",
                    color = Color.Gray,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )
            }

            item {
                Text(
                    text = "--- API_CONFIGURATIONS (OSINT) ---", 
                    style = MaterialTheme.typography.labelSmall, 
                    fontFamily = FontFamily.Monospace, 
                    color = Color.Cyan
                )
                Spacer(modifier = Modifier.height(12.dp))
                uiState.apiKeys.filterKeys { it != "OpenAI" }.forEach { (name, value) ->
                    ApiKeyInput(
                        name = name,
                        value = value,
                        onValueChange = { viewModel.updateApiKey(context, name, it) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            item {
                HorizontalDivider(color = Color.DarkGray, thickness = 0.5.dp)
            }

            item {
                Text(
                    text = "--- USER_PREFERENCES ---", 
                    style = MaterialTheme.typography.labelSmall, 
                    fontFamily = FontFamily.Monospace, 
                    color = Color.Cyan
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("AUTO_SAVE_REPORTS", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                    Switch(
                        checked = uiState.saveReportsAutomatically,
                        onCheckedChange = { viewModel.toggleAutoSave(context, it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.Green, checkedTrackColor = Color.Green.copy(alpha = 0.5f))
                    )
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "HINDRAX_SS_TERMINAL v1.0.0",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontFamily = FontFamily.Monospace
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun ThemeEditorPanel(
    state: SettingsUiState,
    onNameChange: (String) -> Unit,
    onColorChange: (String, String) -> Unit,
    onReset: () -> Unit,
    onCopy: () -> Unit,
    onImportDraftChange: (String) -> Unit,
    onImport: () -> Unit
) {
    val preset = state.themePreset
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "--- GUI_THEME_CONTROL ---",
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
            OutlinedTextField(
                value = preset.name,
                onValueChange = onNameChange,
                label = { Text("THEME_NAME", fontFamily = FontFamily.Monospace) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurface),
                shape = MaterialTheme.shapes.extraSmall
            )
            ThemeColorInput("BG", "background", preset.background, onColorChange)
            ThemeColorInput("SURFACE", "surface", preset.surface, onColorChange)
            ThemeColorInput("TEXT", "text", preset.text, onColorChange)
            ThemeColorInput("ACCENT", "accent", preset.accent, onColorChange)
            ThemeColorInput("WARNING", "warning", preset.warning, onColorChange)
            ThemeColorInput("DANGER", "danger", preset.danger, onColorChange)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onCopy,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.Black),
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("COPY_THEME", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text("RESET", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
            }
            OutlinedTextField(
                value = state.themeImportDraft,
                onValueChange = onImportDraftChange,
                label = { Text("IMPORT_SHARED_THEME", fontFamily = FontFamily.Monospace) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurface),
                shape = MaterialTheme.shapes.extraSmall
            )
            Button(
                onClick = onImport,
                enabled = state.themeImportDraft.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary, contentColor = Color.Black),
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Text("APPLY_IMPORTED_THEME", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
            state.themeStatus?.let {
                Text(it, color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun ThemeColorInput(
    label: String,
    key: String,
    value: String,
    onColorChange: (String, String) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(value.toPreviewColor(), MaterialTheme.shapes.extraSmall)
        )
        OutlinedTextField(
            value = value,
            onValueChange = { onColorChange(key, it) },
            label = { Text(label, fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurface),
            shape = MaterialTheme.shapes.extraSmall
        )
    }
}

private fun String.toPreviewColor(): Color {
    return runCatching { Color(android.graphics.Color.parseColor(this)) }
        .getOrDefault(Color.DarkGray)
}

@Composable
fun ApiKeyInput(name: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("$name API_KEY", fontFamily = FontFamily.Monospace) },
        modifier = Modifier.fillMaxWidth(),
        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = Color.Green) },
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, color = Color.Green),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = Color.DarkGray,
            focusedBorderColor = Color.Green
        ),
        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
        shape = MaterialTheme.shapes.extraSmall
    )
}

@Composable
private fun OllamaPresetButton(
    model: String,
    modifier: Modifier = Modifier,
    onClick: (String) -> Unit
) {
    OutlinedButton(
        onClick = { onClick(model) },
        modifier = modifier.height(38.dp),
        border = BorderStroke(1.dp, Color.DarkGray),
        shape = MaterialTheme.shapes.extraSmall,
        contentPadding = PaddingValues(horizontal = 6.dp)
    ) {
        Text(
            text = model,
            color = Color.Cyan,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            maxLines = 1
        )
    }
}
