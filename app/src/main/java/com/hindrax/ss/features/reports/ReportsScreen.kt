package com.hindrax.ss.features.reports

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hindrax.ss.HindraxApplication
import com.hindrax.ss.data.entity.AuditSessionEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    onBack: () -> Unit,
    onNavigateToSession: (Long) -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as HindraxApplication
    val viewModel: ReportsViewModel = viewModel(
        factory = ReportsViewModelFactory(app.auditRepository)
    )
    val sessions by viewModel.sessions.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Audit History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (sessions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No sessions found.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sessions) { session ->
                    SessionItem(session = session, onClick = { onNavigateToSession(session.id) })
                }
            }
        }
    }
}

@Composable
fun SessionItem(session: AuditSessionEntity, onClick: () -> Unit) {
    val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    val dateStr = sdf.format(Date(session.startedAt))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = session.title, fontWeight = FontWeight.Bold)
                Text(text = "Target: ${session.target}", style = MaterialTheme.typography.bodySmall)
                Text(text = dateStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            }
            Badge(
                containerColor = if (session.status == "FINISHED") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
            ) {
                Text(session.status)
            }
        }
    }
}

class ReportsViewModelFactory(private val repository: com.hindrax.ss.data.repository.AuditRepository) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return ReportsViewModel(repository) as T
    }
}
