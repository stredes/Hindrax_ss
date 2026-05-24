package com.hindrax.ss

import android.os.Bundle
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hindrax.ss.core.util.HindraxThemeStore
import com.hindrax.ss.features.dashboard.DashboardScreen
import com.hindrax.ss.features.network.NetworkScreen
import com.hindrax.ss.features.network.PortScannerScreen
import com.hindrax.ss.features.network.NetworkDiscoveryScreen
import com.hindrax.ss.features.network.BannerGrabbingScreen
import com.hindrax.ss.features.nfc.NfcLabScreen
import com.hindrax.ss.features.apk.ApkAnalysisScreen
import com.hindrax.ss.features.web.WebAnalysisScreen
import com.hindrax.ss.features.web.WebScannerScreen
import com.hindrax.ss.features.reports.ReportsScreen
import com.hindrax.ss.features.reports.SessionDetailScreen
import com.hindrax.ss.features.dns.DnsScreen
import com.hindrax.ss.features.osint.OsintScreen
import com.hindrax.ss.features.osint.MetadataScreen
import com.hindrax.ss.features.osint.WhoisScreen
import com.hindrax.ss.features.automation.AutomationScreen
import com.hindrax.ss.features.ai.HindraxAiScreen
import com.hindrax.ss.features.ai.OpenAiStartupKeyScreen
import com.hindrax.ss.features.files.FileAnalyzerScreen
import com.hindrax.ss.features.location.LiveLocationScreen
import com.hindrax.ss.features.music.OfflineMusicScreen
import com.hindrax.ss.features.profile.HindraxProfileScreen
import com.hindrax.ss.features.targets.AllowedTargetsScreen
import com.hindrax.ss.features.termux.TermuxSetupScreen
import com.hindrax.ss.features.termux.TermuxScriptsScreen
import com.hindrax.ss.features.settings.SettingsScreen
import com.hindrax.ss.features.utils.HindraxUtilsScreen
import com.hindrax.ss.presentation.cyd.CydDashboardScreen
import com.hindrax.ss.presentation.cyd.CydDiscoveryScreen
import com.hindrax.ss.presentation.cyd.CydFileTransferScreen
import com.hindrax.ss.presentation.cyd.CydTerminalScreen
import com.hindrax.ss.presentation.tasks.TaskDetailScreen
import com.hindrax.ss.presentation.tasks.TaskFormScreen
import com.hindrax.ss.presentation.tasks.TaskHistoryScreen
import com.hindrax.ss.presentation.tasks.TaskListScreen
import com.hindrax.ss.presentation.inventory.InventoryScreen
import com.hindrax.ss.presentation.chat.ChatScreen
import com.hindrax.ss.ui.theme.HindraxTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val prefs = remember { getSharedPreferences("hindrax_prefs", Context.MODE_PRIVATE) }
            var themePreset by remember {
                mutableStateOf(
                    HindraxThemeStore.loadActiveTheme(this@MainActivity)
                )
            }
            DisposableEffect(prefs) {
                val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    if (key == HindraxThemeStore.KEY_ACTIVE_THEME) {
                        themePreset = HindraxThemeStore.loadActiveTheme(this@MainActivity)
                    }
                }
                prefs.registerOnSharedPreferenceChangeListener(listener)
                onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
            }

            HindraxTheme(preset = themePreset) {
                val navController = rememberNavController()
                val hasOpenAiKey = remember {
                    prefs.getString("api_key_openai", "")
                        .orEmpty()
                        .isNotBlank()
                }
                Scaffold { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = if (hasOpenAiKey) "dashboard" else "openai_startup_key",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("openai_startup_key") {
                            OpenAiStartupKeyScreen(
                                onKeySaved = {
                                    navController.navigate("dashboard") {
                                        popUpTo("openai_startup_key") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("dashboard") {
                            DashboardScreen(
                                onNavigateToNetwork = { navController.navigate("network") },
                                onNavigateToPortScanner = { navController.navigate("port_scanner") },
                                onNavigateToNetworkDiscovery = { navController.navigate("network_discovery") },
                                onNavigateToBannerGrabbing = { navController.navigate("banner_grabbing") },
                                onNavigateToApk = { navController.navigate("apk") },
                                onNavigateToWeb = { navController.navigate("web") },
                                onNavigateToWebScanner = { navController.navigate("web_scanner") },
                                onNavigateToReports = { navController.navigate("reports") },
                                onNavigateToDns = { navController.navigate("dns") },
                                onNavigateToOsint = { navController.navigate("osint") },
                                onNavigateToWhois = { navController.navigate("whois") },
                                onNavigateToMetadata = { navController.navigate("metadata") },
                                onNavigateToTargets = { navController.navigate("targets") },
                                onNavigateToTermuxSetup = { navController.navigate("termux_setup") },
                                onNavigateToTermuxScripts = { navController.navigate("termux_scripts") },
                                onNavigateToAutomation = { navController.navigate("automation") },
                                onNavigateToSettings = { navController.navigate("settings") },
                                onNavigateToProfile = { navController.navigate("profile") },
                                onNavigateToFileAnalyzer = { navController.navigate("file_analyzer") },
                                onNavigateToAiAssist = { navController.navigate("ai_assist") },
                                onNavigateToCydConnect = { navController.navigate("cyd_discovery") },
                                onNavigateToTasks = { navController.navigate("tasks_list") },
                                onNavigateToInventory = { navController.navigate("inventory") },
                                onNavigateToChat = { navController.navigate("chat") },
                                onNavigateToLiveLocation = { navController.navigate("live_location") },
                                onNavigateToOfflineMusic = { navController.navigate("offline_music") },
                                onNavigateToNfcLab = { navController.navigate("nfc_lab") },
                                onNavigateToUtils = { navController.navigate("utils") }
                            )
                        }
                        
                        composable("network_discovery") {
                            NetworkDiscoveryScreen(
                                onBack = { navController.popBackStack() },
                                onNavigateToCyd = { navController.navigate("cyd_dashboard") },
                                onNavigateToChat = { navController.navigate("chat") }
                            )
                        }

                        composable("chat") {
                            ChatScreen(onBack = { navController.popBackStack() })
                        }

                        composable("cyd_discovery") {
                            CydDiscoveryScreen(
                                onBack = { navController.popBackStack() },
                                onDeviceConnected = { navController.navigate("cyd_dashboard") }
                            )
                        }
                        
                        composable("cyd_dashboard") {
                            CydDashboardScreen(
                                onBack = { navController.popBackStack() },
                                onNavigateToTerminal = { navController.navigate("cyd_terminal") },
                                onNavigateToFileManager = { navController.navigate("cyd_file_manager") }
                            )
                        }

                        composable("network") { NetworkScreen(onBack = { navController.popBackStack() }) }
                        composable("port_scanner") { PortScannerScreen(onBack = { navController.popBackStack() }) }
                        composable("banner_grabbing") { BannerGrabbingScreen(onBack = { navController.popBackStack() }) }
                        composable("dns") { DnsScreen(onBack = { navController.popBackStack() }) }
                        composable("osint") { OsintScreen(onBack = { navController.popBackStack() }) }
                        composable("whois") { WhoisScreen(onBack = { navController.popBackStack() }) }
                        composable("metadata") { MetadataScreen(onBack = { navController.popBackStack() }) }
                        composable("automation") { AutomationScreen(onBack = { navController.popBackStack() }) }
                        composable("apk") { ApkAnalysisScreen(onBack = { navController.popBackStack() }) }
                        composable("web") { WebAnalysisScreen(onBack = { navController.popBackStack() }) }
                        composable("web_scanner") { WebScannerScreen(onBack = { navController.popBackStack() }) }
                        composable("targets") { AllowedTargetsScreen(onBack = { navController.popBackStack() }) }
                        composable("termux_setup") { TermuxSetupScreen(onBack = { navController.popBackStack() }) }
                        composable("termux_scripts") { TermuxScriptsScreen(onBack = { navController.popBackStack() }) }
                        composable("nfc_lab") { NfcLabScreen(onBack = { navController.popBackStack() }) }
                        composable("utils") { HindraxUtilsScreen(onBack = { navController.popBackStack() }) }
                        composable("settings") { SettingsScreen(onBack = { navController.popBackStack() }) }
                        composable("profile") { HindraxProfileScreen(onBack = { navController.popBackStack() }) }
                        composable("file_analyzer") { FileAnalyzerScreen(onBack = { navController.popBackStack() }) }
                        composable("live_location") { LiveLocationScreen(onBack = { navController.popBackStack() }) }
                        composable("offline_music") { OfflineMusicScreen(onBack = { navController.popBackStack() }) }
                        composable("ai_assist") {
                            HindraxAiScreen(
                                onBack = { navController.popBackStack() },
                                onOpenSettings = { navController.navigate("settings") }
                            )
                        }
                        
                        composable("reports") {
                            ReportsScreen(
                                onBack = { navController.popBackStack() },
                                onNavigateToSession = { sessionId -> navController.navigate("session_detail/$sessionId") }
                            )
                        }
                        
                        composable(
                            route = "session_detail/{sessionId}",
                            arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: 0L
                            SessionDetailScreen(sessionId = sessionId, onBack = { navController.popBackStack() })
                        }
                        
                        composable("tasks_list") {
                            TaskListScreen(
                                onNavigateToCreate = { navController.navigate("tasks_form/-1") },
                                onNavigateToDetail = { id -> navController.navigate("tasks_detail/$id") }
                            )
                        }
                        
                        composable(
                            route = "tasks_detail/{taskId}",
                            arguments = listOf(navArgument("taskId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val taskId = backStackEntry.arguments?.getLong("taskId") ?: 0L
                            TaskDetailScreen(
                                taskId = taskId,
                                onEdit = { id -> navController.navigate("tasks_form/$id") },
                                onShowHistory = { id -> navController.navigate("tasks_history/$id") },
                                onBack = { navController.popBackStack() }
                            )
                        }
                        
                        composable(
                            route = "tasks_form/{taskId}",
                            arguments = listOf(navArgument("taskId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val taskId = backStackEntry.arguments?.getLong("taskId") ?: -1L
                            TaskFormScreen(taskId = taskId, onBack = { navController.popBackStack() })
                        }
                        
                        composable(
                            route = "tasks_history/{taskId}",
                            arguments = listOf(navArgument("taskId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val taskId = backStackEntry.arguments?.getLong("taskId") ?: 0L
                            TaskHistoryScreen(taskId = taskId, onBack = { navController.popBackStack() })
                        }
                        
                        composable("inventory") { InventoryScreen(onBack = { navController.popBackStack() }) }
                        composable("cyd_terminal") { CydTerminalScreen(onBack = { navController.popBackStack() }) }
                        composable("cyd_file_manager") { CydFileTransferScreen(onBack = { navController.popBackStack() }) }
                    }
                }
            }
        }
    }
}
