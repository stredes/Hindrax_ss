package com.hindrax.ss.core.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hindrax.ss.HindraxApplication
import com.hindrax.ss.core.model.Severity
import com.hindrax.ss.data.entity.AuditResultEntity
import com.hindrax.ss.domain.tools.NetworkToolSuggestions
import java.net.InetSocketAddress
import java.net.Socket

class AuditWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val sessionId = inputData.getLong("SESSION_ID", -1)
        val target = inputData.getString("TARGET") ?: return Result.failure()
        val taskType = inputData.getString("TASK_TYPE") ?: return Result.failure()
        val ports = NetworkToolSuggestions.parsePorts(inputData.getString("PORTS"))

        val app = applicationContext as HindraxApplication
        val repository = app.auditRepository

        return try {
            when (taskType) {
                "PORT_SCAN" -> runPortScan(sessionId, target, ports, repository)
                else -> Result.failure()
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun runPortScan(
        sessionId: Long,
        target: String,
        ports: List<Int>,
        repository: com.hindrax.ss.data.repository.AuditRepository
    ) {
        val openPorts = mutableListOf<Int>()

        ports.forEach { port ->
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(target, port), 1000)
                    openPorts.add(port)
                }
            } catch (e: Exception) { /* Closed */ }
        }

        if (openPorts.isNotEmpty()) {
            repository.saveResult(
                AuditResultEntity(
                    sessionId = sessionId,
                    severity = Severity.MEDIUM.name,
                    category = "Network",
                    findingTitle = "Open Ports Found",
                    findingBody = "Detected open ports: ${openPorts.joinToString(", ")}\nScanned ports: ${ports.joinToString(", ")}"
                )
            )
        }

        repository.getSessionById(sessionId)?.let { session ->
            repository.updateSession(
                session.copy(
                    status = "FINISHED",
                    finishedAt = System.currentTimeMillis(),
                    summary = if (openPorts.isEmpty()) {
                        "No open ports detected in ${ports.joinToString(", ")}"
                    } else {
                        "Open ports: ${openPorts.joinToString(", ")}"
                    }
                )
            )
        }
    }
}
