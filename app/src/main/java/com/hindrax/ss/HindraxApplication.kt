package com.hindrax.ss

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.hindrax.ss.core.model.Severity
import com.hindrax.ss.core.work.UpdateWorker
import com.hindrax.ss.data.db.HindraxDatabase
import com.hindrax.ss.data.entity.ToolTaskEntity
import com.hindrax.ss.data.repository.AuditRepository
import com.hindrax.ss.data.repository.TargetRepository
import com.hindrax.ss.data.repository.TaskRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
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
    private val applicationScope = CoroutineScope(SupervisorJob())

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    val database by lazy { HindraxDatabase.getDatabase(this) }
    val auditRepository by lazy { AuditRepository(database.auditSessionDao(), database.auditResultDao()) }
    val targetRepository by lazy { TargetRepository(database.allowedTargetDao()) }
    val taskRepository by lazy { TaskRepository(database.toolTaskDao()) }

    override fun onCreate() {
        super.onCreate()
        prepopulateTasks()
        scheduleUpdateChecks()
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
            val tasks = listOf(
                ToolTaskEntity("ping", "Ping Recon", "NETWORK", "NATIVE", null, Severity.LOW.name, true, false),
                ToolTaskEntity("port_scan", "Port Scanner", "NETWORK", "NATIVE", null, Severity.MEDIUM.name, true, false),
                ToolTaskEntity("dns_lookup", "DNS Lookup", "NETWORK", "NATIVE", null, Severity.LOW.name, true, false),
                ToolTaskEntity("apk_analysis", "APK Analysis", "FILES", "NATIVE", null, Severity.LOW.name, true, true),
                ToolTaskEntity("web_headers", "Web Analysis", "WEB", "NATIVE", null, Severity.LOW.name, true, false),
                ToolTaskEntity("osint_discovery", "OSINT Discovery", "OSINT", "NATIVE", null, Severity.LOW.name, true, false)
            )
            tasks.forEach { taskRepository.insertTask(it) }
        }
    }
}
