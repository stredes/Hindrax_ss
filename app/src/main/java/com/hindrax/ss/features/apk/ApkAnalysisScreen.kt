package com.hindrax.ss.features.apk

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.analyzeApk(context, it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("APK Deep Analysis") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Button(
                onClick = { launcher.launch("application/vnd.android.package-archive") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isRunning
            ) {
                Text("Select APK for Analysis")
            }

            Spacer(modifier = Modifier.height(16.dp))

            uiState.apkInfo?.let { info ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Package:", fontWeight = FontWeight.Bold)
                        Text(info.packageName)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Hash (SHA-256):", fontWeight = FontWeight.Bold)
                        Text(info.sha256, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Requested Permissions (${info.permissions.size})", style = MaterialTheme.typography.titleMedium)
                
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    items(info.permissions) { permission ->
                        val isSensitive = permission.contains("SMS") || permission.contains("LOCATION") || permission.contains("CAMERA")
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            if (isSensitive) {
                                Icon(Icons.Default.Warning, contentDescription = "Sensitive", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = permission.split(".").last(),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSensitive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }

            if (uiState.isRunning) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            if (uiState.apkInfo == null && !uiState.isRunning) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.05f)), contentAlignment = Alignment.Center) {
                    Text("Select a file to begin", color = Color.Gray)
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
