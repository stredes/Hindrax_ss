package com.hindrax.ss.features.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hindrax.ss.R
import com.hindrax.ss.core.util.UpdateStatusMessage
import com.hindrax.ss.presentation.tasks.AsciiBanners

private data class DashboardPalette(
    val background: Color,
    val backgroundDeep: Color,
    val panel: Color,
    val panelHot: Color,
    val accent: Color,
    val secondary: Color,
    val warning: Color,
    val danger: Color,
    val text: Color,
    val muted: Color,
    val grid: Color,
    val onAccent: Color
)

@Composable
private fun dashboardPalette(): DashboardPalette {
    val scheme = MaterialTheme.colorScheme
    return DashboardPalette(
        background = scheme.background,
        backgroundDeep = scheme.background.copy(alpha = 0.92f),
        panel = scheme.surface,
        panelHot = scheme.secondary.copy(alpha = 0.16f),
        accent = scheme.primary,
        secondary = scheme.tertiary,
        warning = scheme.secondary,
        danger = scheme.error,
        text = scheme.onSurface,
        muted = scheme.onSurface.copy(alpha = 0.62f),
        grid = scheme.primary.copy(alpha = 0.32f),
        onAccent = scheme.onPrimary
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigateToNetwork: () -> Unit,
    onNavigateToPortScanner: () -> Unit,
    onNavigateToNetworkDiscovery: () -> Unit,
    onNavigateToBannerGrabbing: () -> Unit,
    onNavigateToDns: () -> Unit,
    onNavigateToApk: () -> Unit,
    onNavigateToWeb: () -> Unit,
    onNavigateToWebScanner: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToOsint: () -> Unit,
    onNavigateToWhois: () -> Unit,
    onNavigateToMetadata: () -> Unit,
    onNavigateToTargets: () -> Unit,
    onNavigateToTermuxSetup: () -> Unit,
    onNavigateToTermuxScripts: () -> Unit,
    onNavigateToAutomation: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToFileAnalyzer: () -> Unit,
    onNavigateToAiAssist: () -> Unit,
    onNavigateToCydConnect: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToLiveLocation: () -> Unit,
    onNavigateToOfflineMusic: () -> Unit,
    onNavigateToNfcLab: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val palette = dashboardPalette()
    val context = LocalContext.current
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    val currentVersion = packageInfo.versionName ?: "1.0.0"

    // Configuration-based responsiveness
    val configuration = LocalConfiguration.current
    val isTablet = configuration.smallestScreenWidthDp >= 600
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val columns = when {
        isTablet && isLandscape -> 4
        isTablet && !isLandscape -> 3
        else -> 2
    }

    // Scroll behavior for collapsing app bar (kept minimal)
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    // Full screen toggle state
    val isFullScreenState = remember { mutableStateOf(false) }
    val isFullScreen = isFullScreenState.value

    LaunchedEffect(Unit) {
        viewModel.refreshStatus(context, currentVersion)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(palette.background, palette.backgroundDeep, palette.background)
                )
            ),
        topBar = if (!isFullScreen) {
            {
                // Use a compact top bar to save vertical space in landscape/tablet
                TopAppBar(
                    title = {
                        Text(
                            "[ HINDRAX_CORE :: NEON_ASCII ]",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = palette.accent,
                            fontSize = 14.sp
                        )
                    },
                    actions = {
                        IconButton(onClick = { viewModel.refreshStatus(context, currentVersion) }) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh Status", tint = palette.accent)
                        }

                        IconButton(
                            enabled = uiState.updateAvailable,
                            onClick = { if (uiState.updateAvailable) viewModel.installUpdate() }
                        ) {
                            Icon(Icons.Default.Update, contentDescription = "Install Update", tint = if (uiState.updateAvailable) palette.warning else palette.muted)
                        }

                        IconButton(onClick = { isFullScreenState.value = true }) {
                            Icon(Icons.Default.Fullscreen, contentDescription = "Full screen", tint = palette.accent)
                        }

                        IconButton(onClick = onNavigateToSettings) { Icon(Icons.Default.Settings, contentDescription = "Settings", tint = palette.accent) }
                        IconButton(onClick = onNavigateToProfile) { Icon(Icons.Default.Person, contentDescription = "Profile", tint = palette.secondary) }
                        IconButton(onClick = onNavigateToTermuxSetup) { Icon(Icons.Default.Build, contentDescription = "Termux Setup", tint = palette.warning) }
                        IconButton(onClick = onNavigateToTargets) { Icon(Icons.Default.Lock, contentDescription = "Authorized Targets", tint = palette.danger) }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = palette.background,
                        titleContentColor = palette.accent
                    ),
                    scrollBehavior = scrollBehavior
                )
            }
        } else {
            { /* no top bar when full screen */ }
        },
        floatingActionButton = {
            if (isFullScreen) {
                Column {
                    FloatingActionButton(
                        onClick = { isFullScreenState.value = false },
                        containerColor = palette.panel,
                        contentColor = palette.text,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(Icons.Default.FullscreenExit, contentDescription = "Exit full screen")
                    }

                    FloatingActionButton(
                        onClick = { viewModel.refreshStatus(context, currentVersion) },
                        containerColor = palette.panel,
                        contentColor = palette.accent,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Status")
                    }

                    FloatingActionButton(
                        onClick = { if (uiState.updateAvailable) viewModel.installUpdate() },
                        containerColor = if (uiState.updateAvailable) palette.warning else palette.panel,
                        contentColor = palette.onAccent,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Icon(Icons.Default.Update, contentDescription = "Install Update")
                    }
                }
            }
        }
    ) { innerPadding ->
        // Put the ASCII header, update card and status card INSIDE the grid as a single header item
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            horizontalArrangement = Arrangement.spacedBy(if (isTablet) 16.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(if (isTablet) 16.dp else 12.dp),
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
                .background(palette.background.copy(alpha = 0.35f))
                .nestedScroll(scrollBehavior.nestedScrollConnection)
        ) {
            // Header spans all columns so it scrolls with the content
            item(span = { GridItemSpan(columns) }) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    ResponsiveAsciiBanner(
                        text = AsciiBanners.HINDRAX_MAIN,
                        smallestScreenWidthDp = configuration.smallestScreenWidthDp,
                        color = palette.accent,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    NeonAsciiPanel(
                        title = "SYSTEM_SIGNAL",
                        accent = palette.secondary,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ResponsiveSystemSignal(
                            currentVersion = currentVersion,
                            isTablet = isTablet,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = if (uiState.updateAvailable) palette.panelHot else palette.panel),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (uiState.updateAvailable) palette.warning else palette.grid),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Update, contentDescription = null, tint = if (uiState.updateAvailable) palette.warning else palette.muted)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    if (uiState.updateAvailable) "!! UPDATE_AVAILABLE :: v${uiState.newVersion} !!" else "[ UPDATE_CHANNEL ]",
                                    color = palette.text,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = if (isTablet) 14.sp else 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    uiState.updateInfo?.assetName ?: uiState.updateStatus,
                                    color = if (uiState.updateAvailable) palette.warning.copy(0.9f) else palette.muted,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = if (isTablet) 12.sp else 10.sp
                                )
                                Text(
                                    "status> ${UpdateStatusMessage.human(uiState.updateStatus)}",
                                    color = palette.muted,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = if (isTablet) 11.sp else 9.sp
                                )
                            }
                            Button(
                                onClick = { viewModel.installUpdate() },
                                enabled = uiState.updateAvailable,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.warning,
                                    contentColor = palette.onAccent,
                                    disabledContainerColor = palette.panel,
                                    disabledContentColor = palette.muted
                                ),
                                shape = MaterialTheme.shapes.extraSmall,
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Text("ACTUALIZAR", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = if (isTablet) 12.sp else 10.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    NeonAsciiPanel(
                        title = "NODE_STATUS",
                        accent = palette.accent,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            StatusItem(label = "NET_ADDR", value = uiState.localIp, icon = Icons.Default.Info)
                            StatusItem(
                                label = "API_HINDRAX",
                                value = uiState.apiHindraxStatus,
                                icon = Icons.Default.CloudSync,
                                color = when (uiState.apiHindraxStatus) {
                                    "ONLINE" -> palette.accent
                                    "CONFIG_PENDING" -> palette.warning
                                    else -> palette.muted
                                }
                            )
                            StatusItem(
                                label = "TERMUX",
                                value = if (uiState.isTermuxInstalled) "READY" else "OFFLINE",
                                icon = Icons.Default.Terminal,
                                color = if (uiState.isTermuxInstalled) palette.accent else palette.danger
                            )
                            StatusItem(label = "VERSION", value = "v$currentVersion", icon = Icons.Default.Fingerprint, color = palette.secondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    NeonSectionLabel("CORE_MODULES", "FAST_ACCESS")

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            item { ModuleCard("Automation", Icons.Default.Bolt, onNavigateToAutomation, accentColor = palette.accent) }
            item { ModuleCard("AI_Assist", Icons.Default.Psychology, onNavigateToAiAssist, accentColor = palette.accent) }
            item { ModuleCard("Profile", Icons.Default.Person, onNavigateToProfile, accentColor = palette.accent) }
            item { ModuleCard("File_Analyzer", Icons.Default.FolderOpen, onNavigateToFileAnalyzer, accentColor = palette.secondary) }
            item { ModuleCard("Missions", Icons.Default.Assignment, onNavigateToTasks, accentColor = palette.warning) }
            item { ModuleCard("Logistics", Icons.Default.Inventory, onNavigateToInventory, accentColor = palette.secondary) }
            item { ModuleCard("CYD_Link", Icons.Default.DeveloperBoard, onNavigateToCydConnect, accentColor = palette.danger) }
            item { ModuleCard("Mesh Chat", Icons.Default.Chat, onNavigateToChat, accentColor = palette.text) }
            item { ModuleCard("Geo_Live", Icons.Default.MyLocation, onNavigateToLiveLocation, accentColor = palette.secondary) }
            item { ModuleCard("Offline_Music", Icons.Default.LibraryMusic, onNavigateToOfflineMusic, accentColor = palette.warning) }
            item { ModuleCard("NFC_Lab", Icons.Default.Nfc, onNavigateToNfcLab, accentColor = palette.accent) }
            item { ModuleCard("Net_Disc", Icons.Default.CellTower, onNavigateToNetworkDiscovery, accentColor = palette.secondary) }
            item { ModuleCard("Terminal", Icons.Default.Terminal, onNavigateToTermuxScripts, accentColor = palette.accent) }
            item { ModuleCard("Scanner", Icons.Default.Lan, onNavigateToPortScanner, accentColor = palette.warning) }
            item { ModuleCard("Web_Scan", Icons.Default.ManageSearch, onNavigateToWebScanner, accentColor = palette.danger) }
            item { ModuleCard("Logs", Icons.Default.History, onNavigateToReports) }

            item(span = { GridItemSpan(columns) }) {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ResponsiveAsciiBanner(
    text: String,
    smallestScreenWidthDp: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val availableWidth = maxWidth.value.toInt()
        val fontSp = DashboardAsciiMetrics.bannerFontSp(
            availableWidthDp = availableWidth,
            smallestScreenWidthDp = smallestScreenWidthDp
        )
        Text(
            text = text.trim('\n'),
            color = color,
            fontFamily = FontFamily.Monospace,
            fontSize = fontSp.sp,
            lineHeight = DashboardAsciiMetrics.bannerLineHeightSp(fontSp).sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            softWrap = false
        )
    }
}

@Composable
private fun ResponsiveSystemSignal(
    currentVersion: String,
    isTablet: Boolean,
    modifier: Modifier = Modifier
) {
    val palette = dashboardPalette()
    BoxWithConstraints(modifier = modifier) {
        val availableWidth = maxWidth.value.toInt()
        val logoSize = DashboardAsciiMetrics.nodeLogoSizeDp(availableWidth).dp
        val textSize = DashboardAsciiMetrics.nodeTextSp(availableWidth, isTablet).sp
        val metaTextSize = (DashboardAsciiMetrics.nodeTextSp(availableWidth, isTablet) - 1).coerceAtLeast(8).sp
        val stack = DashboardAsciiMetrics.shouldStackSystemSignal(availableWidth)

        if (stack) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.hindrax_logo),
                    contentDescription = "Hindrax",
                    modifier = Modifier.size(logoSize)
                )
                SystemSignalText(
                    currentVersion = currentVersion,
                    titleSize = textSize,
                    metaSize = metaTextSize,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.hindrax_logo),
                    contentDescription = "Hindrax",
                    modifier = Modifier.size(logoSize)
                )
                Spacer(modifier = Modifier.width(if (isTablet) 16.dp else 10.dp))
                SystemSignalText(
                    currentVersion = currentVersion,
                    titleSize = textSize,
                    metaSize = metaTextSize,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SystemSignalText(
    currentVersion: String,
    titleSize: androidx.compose.ui.unit.TextUnit,
    metaSize: androidx.compose.ui.unit.TextUnit,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start
) {
    val palette = dashboardPalette()
    Column(modifier = modifier) {
        Text(
            "+- HINDRAX NODE ONLINE -+",
            color = palette.accent,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = titleSize,
            maxLines = 1,
            softWrap = false,
            textAlign = textAlign,
            modifier = Modifier.fillMaxWidth()
        )
        Text("| MODE: DASHBOARD_ASCII", color = palette.secondary, fontFamily = FontFamily.Monospace, fontSize = metaSize, maxLines = 1, softWrap = false, textAlign = textAlign, modifier = Modifier.fillMaxWidth())
        Text("| CORE: v$currentVersion", color = palette.warning, fontFamily = FontFamily.Monospace, fontSize = metaSize, maxLines = 1, softWrap = false, textAlign = textAlign, modifier = Modifier.fillMaxWidth())
        Text("+------------------------+", color = palette.grid, fontFamily = FontFamily.Monospace, fontSize = metaSize, maxLines = 1, softWrap = false, textAlign = textAlign, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun NeonAsciiPanel(
    title: String,
    accent: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = dashboardPalette()
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = palette.panel),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.72f)),
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "+--[ $title ]--------------------------------+",
                color = accent,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.background)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
            content()
            Text(
                text = "+--------------------------------------------+",
                color = palette.grid,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun NeonSectionLabel(title: String, meta: String) {
    val palette = dashboardPalette()
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "+-- $title :: $meta --+",
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = palette.secondary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>",
            fontFamily = FontFamily.Monospace,
            color = palette.grid,
            fontSize = 9.sp,
            maxLines = 1
        )
    }
}

@Composable
fun StatusItem(label: String, value: String, icon: ImageVector, color: Color = MaterialTheme.colorScheme.primary) {
    val palette = dashboardPalette()
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = color)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "| $label: ", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = palette.muted)
        Text(text = value, color = color, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleCard(
    name: String, 
    icon: ImageVector, 
    onClick: () -> Unit,
    accentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val palette = dashboardPalette()
    Card(
        onClick = onClick,
        modifier = Modifier.height(88.dp),
        colors = CardDefaults.cardColors(containerColor = palette.panel),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.68f)),
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("+---------+", color = accentColor.copy(alpha = 0.55f), fontFamily = FontFamily.Monospace, fontSize = 8.sp, maxLines = 1)
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = accentColor)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = name.uppercase(),
                fontWeight = FontWeight.Bold, 
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = palette.text,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
            Text("+-run-+", color = palette.grid, fontFamily = FontFamily.Monospace, fontSize = 8.sp, maxLines = 1)
        }
    }
}
