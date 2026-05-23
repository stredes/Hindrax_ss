package com.hindrax.ss

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.hindrax.ss.core.model.Severity
import com.hindrax.ss.core.util.HindraxNotificationCenter
import com.hindrax.ss.core.work.UpdateWorker
import com.hindrax.ss.data.db.HindraxDatabase
import com.hindrax.ss.data.entity.ToolTaskEntity
import com.hindrax.ss.data.remote.ApiHindraxRemoteSyncRepository
import com.hindrax.ss.data.repository.AuditRepository
import com.hindrax.ss.data.repository.TargetRepository
import com.hindrax.ss.data.repository.ToolRepository
import com.hindrax.ss.domain.tools.AndraxToolCatalog
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import androidx.work.WorkManager
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class HindraxApplication : Application(), Configuration.Provider {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var notificationCenter: HindraxNotificationCenter
    @Inject lateinit var remoteSyncRepository: ApiHindraxRemoteSyncRepository

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    val database by lazy { HindraxDatabase.getDatabase(this) }
    val auditRepository by lazy { AuditRepository(database.auditSessionDao(), database.auditResultDao()) }
    val targetRepository by lazy { TargetRepository(database.allowedTargetDao()) }
    val taskRepository by lazy { ToolRepository(database.toolTaskDao()) }

    override fun onCreate() {
        super.onCreate()
        // Initialize WorkManager manually after Hilt injection so the HiltWorkerFactory is used.
        // Guard against double-initialization: if WorkManager was already initialized by the
        // platform (older installs, manifest merge issues), catching IllegalStateException
        // prevents a fatal crash. The preferred flow is disabling WorkManagerInitializer
        // via manifest meta-data so initialization happens only here.
        try {
            androidx.work.WorkManager.initialize(this, workManagerConfiguration)
        } catch (ise: IllegalStateException) {
            // WorkManager already initialized; keep going.
            android.util.Log.i("HindraxApplication", "WorkManager already initialized: ${ise.message}")
        }

        notificationCenter.createChannels()
        prepopulateTasks()
        syncRemoteOnStartup()
        scheduleUpdateChecks()
    }

    private fun syncRemoteOnStartup() {
        applicationScope.launch {
            runCatching {
                remoteSyncRepository.syncAll()
            }.onFailure { error ->
                android.util.Log.w("HindraxApplication", "API_HINDRAX startup sync failed", error)
            }
        }
    }

    private fun scheduleUpdateChecks() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val updateRequest = PeriodicWorkRequestBuilder<UpdateWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "HindraxUpdateCheck",
            ExistingPeriodicWorkPolicy.KEEP,
            updateRequest
        )
    }

    private fun prepopulateTasks() {
        applicationScope.launch {
            val nativeTasks = listOf(
                ToolTaskEntity("ping", "Ping Recon", "NETWORK", "NATIVE", null, Severity.LOW.name, true, false),
                ToolTaskEntity("port_scan", "Port Scanner", "NETWORK", "NATIVE", null, Severity.MEDIUM.name, true, false),
                ToolTaskEntity("dns_lookup", "DNS Lookup", "NETWORK", "NATIVE", null, Severity.LOW.name, true, false),
                ToolTaskEntity("apk_analysis", "APK Analysis", "FILES", "NATIVE", null, Severity.LOW.name, true, true),
                ToolTaskEntity("web_headers", "Web Analysis", "WEB", "NATIVE", null, Severity.LOW.name, true, false),
                ToolTaskEntity("osint_discovery", "OSINT Discovery", "OSINT", "NATIVE", null, Severity.LOW.name, true, false)
            )
            val catalogTasks = AndraxToolCatalog.categories.flatMap { category ->
                category.tools.map { tool ->
                    ToolTaskEntity(
                        id = "andrax_${tool.command.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')}",
                        name = tool.displayName,
                        category = category.name.uppercase(),
                        executionMode = tool.executionMode.name,
                        scriptName = null,
                        riskLevel = tool.riskLevel.name,
                        enabled = true,
                        localOnly = category.requirements.isNotEmpty()
                    )
                }
            }

            for (task in nativeTasks + catalogTasks) {
                taskRepository.insertTask(task)
            }
        }
    }
}
