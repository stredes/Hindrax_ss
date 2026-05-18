package com.hindrax.ss.presentation.cyd

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CydFileTransferScreen(
    onBack: () -> Unit,
    viewModel: CydFileTransferViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val content = context.contentResolver.openInputStream(it)?.readBytes()
            val fileName = it.path?.substringAfterLast("/") ?: "upload.bin"
            if (content != null) {
                viewModel.uploadFile(fileName, content)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CYD File Manager") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshFileList() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { filePickerLauncher.launch("*/*") }) {
                Icon(Icons.Default.UploadFile, contentDescription = "Upload")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (uiState.isUploading || uiState.isDownloading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            uiState.errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.files) { fileName ->
                    FileListItem(
                        fileName = fileName,
                        onDownload = { viewModel.downloadFile(fileName) }
                    )
                }
            }
        }
    }
}

@Composable
fun FileListItem(fileName: String, onDownload: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.InsertDriveFile, contentDescription = null)
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = fileName)
            }
            IconButton(onClick = onDownload) {
                Icon(Icons.Default.Download, contentDescription = "Download")
            }
        }
    }
}
