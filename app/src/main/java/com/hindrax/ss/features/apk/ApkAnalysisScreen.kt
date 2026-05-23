package com.hindrax.ss.features.apk

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApkAnalysisScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as HindraxApplication
    val viewModel: ApkAnalysisViewModel = viewModel(
        factory = ApkAnalysisViewModelFactory(app.auditRepository)
    )
    val uiState by viewModel.uiState.collectAsState()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.analyzeApk(context, it) }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("APK_DEEP_ANALYSIS", fontFamily = FontFamily.Monospace) },
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
            item {
                Button(
                    onClick = { launcher.launch("application/vnd.android.package-archive") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isRunning,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Green, contentColor = Color.Black),
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text("SELECT_APK_FOR_ANALYSIS", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }

            uiState.apkInfo?.let { info ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF151515)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.DarkGray),
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("PACKAGE:", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            Text(info.packageName, color = Color.White, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("HASH_SHA256:", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            Text(info.sha256, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = Color.Cyan)
                        }
                    }
                }

                item {
                    Text("--- REQUESTED_PERMISSIONS (${info.permissions.size}) ---", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = Color.Cyan)
                }

                items(info.permissions) { permission ->
                    val isSensitive = permission.contains("SMS") || permission.contains("LOCATION") || permission.contains("CAMERA") || permission.contains("RECORD_AUDIO")
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        if (isSensitive) {
                            Icon(Icons.Default.Warning, contentDescription = "Sensitive", tint = Color.Red, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = permission.split(".").last(),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = if (isSensitive) Color.Red else Color.Green
                        )
                    }
                    HorizontalDivider(color = Color.DarkGray, thickness = 0.5.dp)
                }
            }

            if (uiState.isRunning) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.Green)
                    }
                }
            }

            if (uiState.apkInfo == null && !uiState.isRunning) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize().background(Color(0xFF0A0A0A)).border(androidx.compose.foundation.BorderStroke(1.dp, Color.DarkGray)), contentAlignment = Alignment.Center) {
                        Text("WAITING_FOR_PAYLOAD...", color = Color.Gray, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

class ApkAnalysisViewModelFactory(private val repository: com.hindrax.ss.data.repository.AuditRepository) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return ApkAnalysisViewModel(repository) as T
    }
}
