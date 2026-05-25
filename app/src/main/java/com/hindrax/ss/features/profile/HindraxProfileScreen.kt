package com.hindrax.ss.features.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hindrax.ss.R
import com.hindrax.ss.domain.theme.HindraxThemePreset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HindraxProfileScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val viewModel: HindraxProfileViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LaunchedEffect(Unit) {
        viewModel.load(context)
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("HINDRAX_PROFILE", fontFamily = FontFamily.Monospace) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.hindrax_logo),
                contentDescription = "Hindrax",
                modifier = Modifier.size(120.dp)
            )

            Text(
                text = "--- LOCAL_IDENTITY ---",
                color = MaterialTheme.colorScheme.secondary,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "DEVICE_HASH: ${uiState.deviceId}",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )

                OutlinedTextField(
                    value = uiState.nickname,
                    onValueChange = viewModel::onNicknameChange,
                    label = { Text("MY_NICKNAME", fontFamily = FontFamily.Monospace) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = MaterialTheme.shapes.extraSmall
                )

                Text(
                    text = "Este nickname se envia en el pairing y aparece en otros dispositivos en lugar de solo el hash.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }

            Button(
                onClick = { viewModel.save(context) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.Black
                ),
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.size(8.dp))
                Text("SAVE_PROFILE", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }

            if (uiState.saved) {
                Text(
                    "PROFILE_SAVED",
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))

            RootProfilePanel(
                uiState = uiState,
                onRootKeyChange = viewModel::onRootKeyChange,
                onRootConfirmChange = viewModel::onRootConfirmChange,
                onUnlock = { viewModel.unlockRoot(context) },
                onLock = viewModel::lockRoot,
                onRefreshPeers = { viewModel.refreshRootPeers(context) },
                onResetLocal = { viewModel.resetLocalDatabase(context) },
                onResetFirebase = { viewModel.resetFirebaseDatabase(context) },
                onDeleteLocalPeer = { viewModel.deleteRootPeer(context, it) },
                onDeleteApiPeer = { viewModel.deleteRootPeerFromApi(context, it) }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))

            ThemeProfilePanel(
                activeTheme = uiState.activeTheme,
                savedThemes = uiState.savedThemes,
                importDraft = uiState.themeImportDraft,
                status = uiState.themeStatus,
                onApply = { viewModel.applyTheme(context, it) },
                onDelete = { viewModel.deleteTheme(context, it) },
                onCopy = { clipboardManager.setText(AnnotatedString(viewModel.themeExport(it))) },
                onImportDraftChange = viewModel::onThemeImportDraftChange,
                onImport = { viewModel.importTheme(context) }
            )
        }
    }
}

@Composable
private fun RootProfilePanel(
    uiState: HindraxProfileUiState,
    onRootKeyChange: (String) -> Unit,
    onRootConfirmChange: (String) -> Unit,
    onUnlock: () -> Unit,
    onLock: () -> Unit,
    onRefreshPeers: () -> Unit,
    onResetLocal: () -> Unit,
    onResetFirebase: () -> Unit,
    onDeleteLocalPeer: (String) -> Unit,
    onDeleteApiPeer: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "--- ROOT_ADMIN_MODE ---",
                color = MaterialTheme.colorScheme.error,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }

        if (!uiState.rootUnlocked) {
            OutlinedTextField(
                value = uiState.rootKeyDraft,
                onValueChange = onRootKeyChange,
                label = { Text("ROOT_KEY", fontFamily = FontFamily.Monospace) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                textStyle = LocalTextStyle.current.copy(
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                shape = MaterialTheme.shapes.extraSmall
            )
            Button(
                onClick = onUnlock,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = Color.Black
                ),
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Icon(Icons.Default.LockOpen, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("UNLOCK_ROOT", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onRefreshPeers,
                    enabled = !uiState.rootBusy,
                    modifier = Modifier.weight(1f).height(42.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("REFRESH", fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                }
                OutlinedButton(
                    onClick = onLock,
                    modifier = Modifier.weight(1f).height(42.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("LOCK", fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                }
            }

            RootDangerPanel(
                confirmDraft = uiState.rootConfirmDraft,
                busy = uiState.rootBusy,
                onConfirmChange = onRootConfirmChange,
                onResetLocal = onResetLocal,
                onResetFirebase = onResetFirebase
            )

            RootDevicesPanel(
                peers = uiState.rootPeers,
                busy = uiState.rootBusy,
                onDeleteLocalPeer = onDeleteLocalPeer,
                onDeleteApiPeer = onDeleteApiPeer
            )
        }

        Text(
            text = "GPS_ROOT_SCOPE: solo muestra ubicacion compartida por dispositivos vinculados/sincronizados.",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            lineHeight = 14.sp
        )

        uiState.rootStatus?.let {
            Text(
                text = "ROOT_STATUS: $it",
                color = if (it.contains("ERROR") || it.contains("DENIED") || it.contains("REQUIRED")) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun RootDangerPanel(
    confirmDraft: String,
    busy: Boolean,
    onConfirmChange: (String) -> Unit,
    onResetLocal: () -> Unit,
    onResetFirebase: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.08f))
            .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "DANGER_ZONE",
                color = MaterialTheme.colorScheme.error,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
        OutlinedTextField(
            value = confirmDraft,
            onValueChange = onConfirmChange,
            label = { Text("TYPE RESET_LOCAL OR RESET_FIREBASE", fontFamily = FontFamily.Monospace) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface
            ),
            shape = MaterialTheme.shapes.extraSmall
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onResetLocal,
                enabled = !busy && confirmDraft.trim() == "RESET_LOCAL",
                modifier = Modifier.weight(1f).height(42.dp),
                contentPadding = PaddingValues(horizontal = 6.dp),
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("LOCAL", fontFamily = FontFamily.Monospace, fontSize = 10.sp)
            }
            Button(
                onClick = onResetFirebase,
                enabled = !busy && confirmDraft.trim() == "RESET_FIREBASE",
                modifier = Modifier.weight(1f).height(42.dp),
                contentPadding = PaddingValues(horizontal = 6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = Color.Black
                ),
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("FIREBASE", fontFamily = FontFamily.Monospace, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun RootDevicesPanel(
    peers: List<RootPeerUi>,
    busy: Boolean,
    onDeleteLocalPeer: (String) -> Unit,
    onDeleteApiPeer: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.People, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "LINKED_DEVICES: ${peers.size}",
                color = MaterialTheme.colorScheme.primary,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
        if (peers.isEmpty()) {
            Text(
                "NO_LINKED_DEVICES_IN_LOCAL_DB",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
            )
        }
        peers.forEach { peer ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        peer.displayName,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "ID=${peer.id} | IP=${peer.ip} | ${if (peer.isOnline) "ONLINE" else "OFFLINE"}",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.GpsFixed,
                            contentDescription = null,
                            tint = if (peer.hasLocation) MaterialTheme.colorScheme.primary else Color.Gray,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "${peer.locationLabel} | seen=${peer.lastSeenLabel}",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { onDeleteLocalPeer(peer.id) },
                            enabled = !busy,
                            modifier = Modifier.weight(1f).height(36.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text("DEL_LOCAL", fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                        }
                        OutlinedButton(
                            onClick = { onDeleteApiPeer(peer.id) },
                            enabled = !busy,
                            modifier = Modifier.weight(1f).height(36.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text("DEL_API", fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeProfilePanel(
    activeTheme: HindraxThemePreset,
    savedThemes: List<HindraxThemePreset>,
    importDraft: String,
    status: String?,
    onApply: (HindraxThemePreset) -> Unit,
    onDelete: (HindraxThemePreset) -> Unit,
    onCopy: (HindraxThemePreset) -> Unit,
    onImportDraftChange: (String) -> Unit,
    onImport: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "--- PROFILE_THEME_LIBRARY ---",
                color = MaterialTheme.colorScheme.primary,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }

        Text(
            text = "ACTIVE_THEME: ${activeTheme.name}",
            color = MaterialTheme.colorScheme.onBackground,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp
        )

        savedThemes.forEach { preset ->
            ThemeProfileCard(
                preset = preset,
                isActive = preset.name.equals(activeTheme.name, ignoreCase = true),
                onApply = { onApply(preset) },
                onDelete = { onDelete(preset) },
                onCopy = { onCopy(preset) }
            )
        }

        OutlinedTextField(
            value = importDraft,
            onValueChange = onImportDraftChange,
            label = { Text("IMPORT_THEME_TO_PROFILE", fontFamily = FontFamily.Monospace) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            textStyle = LocalTextStyle.current.copy(
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface
            ),
            shape = MaterialTheme.shapes.extraSmall
        )
        Button(
            onClick = onImport,
            enabled = importDraft.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.Black
            ),
            shape = MaterialTheme.shapes.extraSmall
        ) {
            Text("SAVE_AND_APPLY_IMPORTED_THEME", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }

        status?.let {
            Text(it, color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
        }
    }
}

@Composable
private fun ThemeProfileCard(
    preset: HindraxThemePreset,
    isActive: Boolean,
    onApply: () -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = preset.name,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = if (isActive) "ACTIVE" else "SAVED_IN_PROFILE",
                        color = if (isActive) MaterialTheme.colorScheme.primary else Color.Gray,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                }
                if (isActive) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf(preset.background, preset.surface, preset.text, preset.accent, preset.info, preset.warning, preset.danger)) { color ->
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .background(color.toProfilePreviewColor(), MaterialTheme.shapes.extraSmall)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), MaterialTheme.shapes.extraSmall)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onApply,
                    enabled = !isActive,
                    modifier = Modifier.weight(1f).height(38.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.Black
                    ),
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text("APPLY", fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                }
                OutlinedButton(
                    onClick = onCopy,
                    modifier = Modifier.weight(1f).height(38.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("COPY", fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                }
                OutlinedButton(
                    onClick = onDelete,
                    enabled = !isActive,
                    modifier = Modifier.weight(1f).height(38.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("DEL", fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                }
            }
        }
    }
}

private fun String.toProfilePreviewColor(): Color {
    return runCatching { Color(android.graphics.Color.parseColor(this)) }
        .getOrDefault(Color.DarkGray)
}
