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
import com.hindrax.ss.domain.tools.AndraxToolCatalog
import com.hindrax.ss.domain.tools.ToolCatalogItem
import com.hindrax.ss.domain.tools.ToolRiskLevel
import com.hindrax.ss.presentation.tasks.AsciiBanners

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
    onNavigateToNfcLab: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
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
            .background(Color(0xFF050505)),
        topBar = if (!isFullScreen) {
            {
                // Use a compact top bar to save vertical space in landscape/tablet
                TopAppBar(
                    title = { Text("HINDRAX_CORE", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color.Green) },
                    actions = {
                        IconButton(onClick = { viewModel.refreshStatus(context, currentVersion) }) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh Status", tint = Color.Green)
                        }

                        IconButton(
                            enabled = uiState.updateAvailable,
                            onClick = { if (uiState.updateAvailable) viewModel.installUpdate() }
                        ) {
                            Icon(Icons.Default.Update, contentDescription = "Install Update", tint = if (uiState.updateAvailable) Color.Yellow else Color.DarkGray)
                        }

                        IconButton(onClick = { isFullScreenState.value = true }) {
                            Icon(Icons.Default.Fullscreen, contentDescription = "Full screen", tint = Color.Green)
                        }

                        IconButton(onClick = onNavigateToSettings) { Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.Green) }
                        IconButton(onClick = onNavigateToProfile) { Icon(Icons.Default.Person, contentDescription = "Profile", tint = Color.Green) }
                        IconButton(onClick = onNavigateToTermuxSetup) { Icon(Icons.Default.Build, contentDescription = "Termux Setup", tint = Color.Green) }
                        IconButton(onClick = onNavigateToTargets) { Icon(Icons.Default.Lock, contentDescription = "Authorized Targets", tint = Color.Green) }
                    },
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
                        containerColor = Color.DarkGray,
                        contentColor = Color.White,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(Icons.Default.FullscreenExit, contentDescription = "Exit full screen")
                    }

                    FloatingActionButton(
                        onClick = { viewModel.refreshStatus(context, currentVersion) },
                        containerColor = Color.DarkGray,
                        contentColor = Color.Green,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Status")
                    }

                    FloatingActionButton(
                        onClick = { if (uiState.updateAvailable) viewModel.installUpdate() },
                        containerColor = if (uiState.updateAvailable) Color.Yellow else Color.DarkGray,
                        contentColor = Color.Black,
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
                .nestedScroll(scrollBehavior.nestedScrollConnection)
        ) {
            // Header spans all columns so it scrolls with the content
            item(span = { GridItemSpan(columns) }) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = AsciiBanners.HINDRAX_MAIN,
                        color = Color.Green,
                        fontFamily = FontFamily.Monospace,
                        fontSize = if (isTablet) 12.sp else 7.sp,
                        lineHeight = if (isTablet) 14.sp else 8.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        softWrap = false
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Image(
                        painter = painterResource(id = R.drawable.hindrax_logo),
                        contentDescription = "Hindrax",
                        modifier = Modifier
                            .size(if (isTablet) 116.dp else 86.dp)
                            .align(Alignment.CenterHorizontally)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = if (uiState.updateAvailable) Color(0xFF1A1600) else Color(0xFF0A0A0A)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (uiState.updateAvailable) Color.Yellow else Color.DarkGray),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Update, contentDescription = null, tint = if (uiState.updateAvailable) Color.Yellow else Color.DarkGray)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    if (uiState.updateAvailable) "UPDATE_AVAILABLE: v${uiState.newVersion}" else "UPDATE_CHANNEL",
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = if (isTablet) 14.sp else 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    uiState.updateInfo?.assetName ?: "Waiting for GitHub release APK.",
                                    color = if (uiState.updateAvailable) Color.Yellow.copy(0.8f) else Color.Gray,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = if (isTablet) 12.sp else 10.sp
                                )
                                Text(uiState.updateStatus, color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = if (isTablet) 11.sp else 9.sp)
                            }
                            Button(
                                onClick = { viewModel.installUpdate() },
                                enabled = uiState.updateAvailable,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Yellow,
                                    contentColor = Color.Black,
                                    disabledContainerColor = Color.DarkGray,
                                    disabledContentColor = Color.Black
                                ),
                                shape = MaterialTheme.shapes.extraSmall,
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Text("ACTUALIZAR", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = if (isTablet) 12.sp else 10.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.DarkGray)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            StatusItem(label = "NET_ADDR", value = uiState.localIp, icon = Icons.Default.Info)
                            StatusItem(
                                label = "TERMUX",
                                value = if (uiState.isTermuxInstalled) "READY" else "OFFLINE",
                                icon = Icons.Default.Terminal,
                                color = if (uiState.isTermuxInstalled) Color.Green else Color.Red
                            )
                            StatusItem(label = "VERSION", value = "v$currentVersion", icon = Icons.Default.Fingerprint, color = Color.Cyan)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "--- CORE_MODULES ---",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = Color.Cyan
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            item { ModuleCard("Automation", Icons.Default.Bolt, onNavigateToAutomation, accentColor = Color.Green) }
            item { ModuleCard("AI_Assist", Icons.Default.Psychology, onNavigateToAiAssist, accentColor = Color.Green) }
            item { ModuleCard("Profile", Icons.Default.Person, onNavigateToProfile, accentColor = Color.Green) }
            item { ModuleCard("File_Analyzer", Icons.Default.FolderOpen, onNavigateToFileAnalyzer, accentColor = Color.Green) }
            item { ModuleCard("Missions", Icons.Default.Assignment, onNavigateToTasks, accentColor = Color.Yellow) }
            item { ModuleCard("Logistics", Icons.Default.Inventory, onNavigateToInventory, accentColor = Color.Cyan) }
            item { ModuleCard("CYD_Link", Icons.Default.DeveloperBoard, onNavigateToCydConnect, accentColor = Color.Magenta) }
            item { ModuleCard("Mesh Chat", Icons.Default.Chat, onNavigateToChat, accentColor = Color.White) }
            item { ModuleCard("Geo_Live", Icons.Default.MyLocation, onNavigateToLiveLocation, accentColor = Color.Cyan) }
            item { ModuleCard("NFC_Lab", Icons.Default.Nfc, onNavigateToNfcLab, accentColor = Color.Green) }
            item { ModuleCard("Net_Disc", Icons.Default.CellTower, onNavigateToNetworkDiscovery) }
            item { ModuleCard("Terminal", Icons.Default.Terminal, onNavigateToTermuxScripts) }
            item { ModuleCard("Scanner", Icons.Default.Lan, onNavigateToPortScanner) }
            item { ModuleCard("Web_Scan", Icons.Default.ManageSearch, onNavigateToWebScanner) }
            item { ModuleCard("Logs", Icons.Default.History, onNavigateToReports) }

            item(span = { GridItemSpan(columns) }) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "--- EXPOSED_TOOLS ---",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = Color.Cyan
                )
            }

            AndraxToolCatalog.categories.forEach { category ->
                item(span = { GridItemSpan(columns) }) {
                    ToolCategoryHeader(
                        title = category.name,
                        count = category.tools.size,
                        capabilities = category.capabilities.joinToString(" / ")
                    )
                }
                category.tools.forEach { tool ->
                    item {
                        ToolHomeCard(
                            tool = tool,
                            onClick = tool.homeAction(
                                onNavigateToNetwork = onNavigateToNetwork,
                                onNavigateToPortScanner = onNavigateToPortScanner,
                                onNavigateToNetworkDiscovery = onNavigateToNetworkDiscovery,
                                onNavigateToBannerGrabbing = onNavigateToBannerGrabbing,
                                onNavigateToDns = onNavigateToDns,
                                onNavigateToApk = onNavigateToApk,
                                onNavigateToWeb = onNavigateToWeb,
                                onNavigateToWebScanner = onNavigateToWebScanner,
                                onNavigateToOsint = onNavigateToOsint,
                                onNavigateToWhois = onNavigateToWhois,
                                onNavigateToMetadata = onNavigateToMetadata,
                                onNavigateToTermuxScripts = onNavigateToTermuxScripts,
                                onNavigateToCydConnect = onNavigateToCydConnect,
                                onNavigateToNfcLab = onNavigateToNfcLab,
                                onNavigateToFileAnalyzer = onNavigateToFileAnalyzer
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ToolCategoryHeader(title: String, count: Int, capabilities: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF061006)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1F7A00)),
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = ">> ${title.uppercase()} [$count]",
                color = Color.Green,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            Text(
                text = capabilities,
                color = Color.Gray,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                maxLines = 2
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolHomeCard(tool: ToolCatalogItem, onClick: () -> Unit) {
    val riskColor = when (tool.riskLevel) {
        ToolRiskLevel.LOW -> Color.Green
        ToolRiskLevel.MEDIUM -> Color.Yellow
        ToolRiskLevel.HIGH -> Color.Red
    }
    Card(
        onClick = onClick,
        modifier = Modifier.height(88.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF090909)),
        border = androidx.compose.foundation.BorderStroke(1.dp, riskColor.copy(alpha = 0.55f)),
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = tool.displayName.uppercase(),
                color = Color.White,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                maxLines = 1
            )
            Text(
                text = tool.tutorial.authorizedUse,
                color = Color.Gray,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                lineHeight = 11.sp,
                maxLines = 2
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = tool.executionMode.name,
                    color = Color.Cyan,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = tool.riskLevel.name,
                    color = riskColor,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp
                )
            }
        }
    }
}

private fun ToolCatalogItem.homeAction(
    onNavigateToNetwork: () -> Unit,
    onNavigateToPortScanner: () -> Unit,
    onNavigateToNetworkDiscovery: () -> Unit,
    onNavigateToBannerGrabbing: () -> Unit,
    onNavigateToDns: () -> Unit,
    onNavigateToApk: () -> Unit,
    onNavigateToWeb: () -> Unit,
    onNavigateToWebScanner: () -> Unit,
    onNavigateToOsint: () -> Unit,
    onNavigateToWhois: () -> Unit,
    onNavigateToMetadata: () -> Unit,
    onNavigateToTermuxScripts: () -> Unit,
    onNavigateToCydConnect: () -> Unit,
    onNavigateToNfcLab: () -> Unit,
    onNavigateToFileAnalyzer: () -> Unit
): () -> Unit {
    return when (command.lowercase()) {
        "nmap", "masscan", "zmap" -> onNavigateToPortScanner
        "netdiscover", "arp-scan", "fping" -> onNavigateToNetworkDiscovery
        "hping3", "traceroute", "mtr" -> onNavigateToNetwork
        "dnsrecon", "dnsenum", "dig" -> onNavigateToDns
        "whois" -> onNavigateToWhois
        "nikto", "whatweb", "wapiti" -> onNavigateToWeb
        "dirsearch", "gobuster", "wfuzz", "ffuf", "sqlmap", "xsser", "burpsuite" -> onNavigateToWebScanner
        "theharvester", "recon-ng", "sublist3r", "amass", "photon", "maltego" -> onNavigateToOsint
        "metagoofil" -> onNavigateToMetadata
        "apktool", "jadx", "dex2jar", "smali", "baksmali", "aapt", "apksigner", "uber-apk-signer" -> onNavigateToApk
        "mfoc", "mfcuk", "libnfc", "nfc-tools" -> onNavigateToNfcLab
        "binwalk", "foremost", "scalpel", "bulk_extractor", "strings", "dcfldd",
        "firmwalker", "sasquatch" -> onNavigateToFileAnalyzer
        "aircrack-ng", "aireplay-ng", "airodump-ng", "reaver", "bully", "hcxdumptool", "hcxtools",
        "blue_hydra", "btscanner", "bluelog", "bluez" -> onNavigateToCydConnect
        "tcpdump", "ettercap", "bettercap", "dsniff", "wireshark-cli" -> onNavigateToBannerGrabbing
        else -> onNavigateToTermuxScripts
    }
}

@Composable
fun StatusItem(label: String, value: String, icon: ImageVector, color: Color = Color.Green) {
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = color)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "$label: ", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = Color.Gray)
        Text(text = value, color = color, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleCard(
    name: String, 
    icon: ImageVector, 
    onClick: () -> Unit,
    accentColor: Color = Color.White
) {
    Card(
        onClick = onClick,
        modifier = Modifier.height(80.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.DarkGray),
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = accentColor)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = name.uppercase(), 
                fontWeight = FontWeight.Bold, 
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = Color.White
            )
        }
    }
}
