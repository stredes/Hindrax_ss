package com.hindrax.ss.features.ai

import android.content.Context
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.hindrax.ss.HindraxApplication
import com.hindrax.ss.core.model.Severity
import com.hindrax.ss.core.security.TargetParser
import com.hindrax.ss.core.work.AuditWorker
import com.hindrax.ss.data.entity.AuditResultEntity
import com.hindrax.ss.data.entity.AuditSessionEntity
import com.hindrax.ss.domain.ai.OpenAiFunctionCall
import com.hindrax.ss.domain.ai.OpenAiResponseParser
import com.hindrax.ss.domain.ai.OpenAiToolOutput
import com.hindrax.ss.domain.tools.AndraxToolCatalog
import com.hindrax.ss.domain.tools.NetworkToolSuggestions
import com.hindrax.ss.domain.tools.ToolCatalogItem
import com.hindrax.ss.domain.tools.ToolRiskLevel
import com.hindrax.ss.termux.TermuxBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetAddress
import java.util.concurrent.TimeUnit

class HindraxAiToolExecutor(
    private val context: Context
) {
    private val app = context.applicationContext as HindraxApplication
    private val auditRepository = app.auditRepository
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun execute(call: OpenAiFunctionCall): OpenAiToolOutput {
        val output = when (call.name) {
            "list_hindrax_tools" -> listTools(call.arguments)
            "run_hindrax_tool" -> runTool(call.arguments)
            "run_termux_catalog_command" -> runTermuxCatalogCommand(call.arguments)
            else -> """{"status":"BLOCKED","reason":"UNKNOWN_TOOL_FUNCTION"}"""
        }
        return OpenAiToolOutput(call.callId, output)
    }

    private fun listTools(arguments: String): String {
        val category = OpenAiResponseParser.parseArguments(arguments)["category"]
            ?.jsonPrimitive
            ?.contentOrNull
            ?.uppercase()
            ?.takeIf { it != "ALL" }

        val nativeTools = listOf(
            ToolSummary("ping", "Ping Recon", "NETWORK", true),
            ToolSummary("dns_lookup", "DNS Lookup", "NETWORK", true),
            ToolSummary("web_headers", "Web Analysis", "WEB", true),
            ToolSummary("osint_discovery", "OSINT Discovery", "OSINT", true),
            ToolSummary("port_scan", "Port Scanner", "NETWORK", true),
            ToolSummary("net_disc", "Network Discovery", "NETWORK", false)
        ).filter { category == null || it.category == category }

        val nativeToolsJson = nativeTools.joinToString(prefix = "[", postfix = "]") { tool ->
            """{"id":"${tool.id}","name":"${tool.name}","category":"${tool.category}","directLaunch":${tool.directLaunch},"runner":"run_hindrax_tool"}"""
        }
        val catalogToolsJson = AndraxToolCatalog.categories
            .filter { category == null || it.id.uppercase() == category || it.name.uppercase() == category }
            .flatMap { toolCategory ->
                toolCategory.tools.map { tool -> toolCategory to tool }
            }
            .distinctBy { it.second.command.lowercase() }
            .joinToString(prefix = "[", postfix = "]") { (toolCategory, tool) ->
                """{"id":"${safe(tool.command)}","name":"${safe(tool.displayName)}","category":"${safe(toolCategory.id)}","risk":"${tool.riskLevel.name}","executionMode":"${tool.executionMode.name}","runner":"run_termux_catalog_command","example":"${safe(tool.tutorial.commandExample)}","authorizedUse":"${safe(tool.tutorial.authorizedUse)}"}"""
        }
        val portProfilesJson = NetworkToolSuggestions.profiles.joinToString(prefix = "[", postfix = "]") { profile ->
            """{"id":"${profile.id}","label":"${profile.label}","ports":"${profile.ports.joinToString(",")}","description":"${safe(profile.description)}"}"""
        }

        return """{"status":"OK","nativeTools":$nativeToolsJson,"termuxCatalogTools":$catalogToolsJson,"toolCount":${AndraxToolCatalog.allTools.size},"termuxInstalled":${TermuxBridge.isTermuxInstalled(context)},"netDiscSuggestions":{"recommendedTools":["ping","port_scan","banner_grab","dns_lookup","web_headers","hindrax_chat","live_location","cyd_console"],"portProfiles":$portProfilesJson},"note":"Use run_hindrax_tool for native app workflows and run_termux_catalog_command for allowlisted catalog commands installed in Termux."}"""
    }

    private suspend fun runTool(arguments: String): String {
        val args = OpenAiResponseParser.parseArguments(arguments)
        val toolId = args["tool_id"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        val targetValue = args["target"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        val ports = NetworkToolSuggestions.parsePorts(args["ports"]?.jsonPrimitive?.contentOrNull)
        val authorizationConfirmed = args["authorization_confirmed"]?.jsonPrimitive?.booleanOrNull == true

        if (toolId.isBlank() || targetValue.isBlank()) {
            return """{"status":"BLOCKED","reason":"MISSING_TOOL_OR_TARGET"}"""
        }

        val target = TargetParser.parse(targetValue)
            ?: return """{"status":"BLOCKED","reason":"INVALID_TARGET"}"""

        if (target.isPublic() && !authorizationConfirmed) {
            return """{"status":"REQUIRES_AUTHORIZATION","reason":"PUBLIC_TARGET_REQUIRES_USER_CONFIRMATION"}"""
        }

        return when (toolId) {
            "ping" -> runPing(target.value)
            "dns_lookup" -> runDnsLookup(target.value)
            "web_headers", "osint_discovery" -> runWebHeaders(target.value, toolId)
            "port_scan" -> enqueuePortScan(target.value, ports)
            "net_disc" -> netDiscGuidance(target.value)
            else -> """{"status":"BLOCKED","reason":"UNSUPPORTED_DIRECT_TOOL","tool_id":"$toolId"}"""
        }
    }

    private suspend fun runTermuxCatalogCommand(arguments: String): String = withContext(Dispatchers.IO) {
        val args = OpenAiResponseParser.parseArguments(arguments)
        val commandId = args["command_id"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        val targetValue = args["target"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        val authorizationConfirmed = args["authorization_confirmed"]?.jsonPrimitive?.booleanOrNull == true
        val commandArgs = parseCommandArguments(args["arguments"]?.jsonPrimitive?.contentOrNull.orEmpty())

        if (commandId.isBlank()) {
            return@withContext """{"status":"BLOCKED","reason":"MISSING_COMMAND_ID"}"""
        }

        val catalogTool = findCatalogTool(commandId)
            ?: return@withContext """{"status":"BLOCKED","reason":"COMMAND_NOT_IN_HINDRAX_CATALOG","command_id":"${safe(commandId)}"}"""

        if (!TermuxBridge.isTermuxInstalled(context)) {
            return@withContext """{"status":"BLOCKED","reason":"TERMUX_NOT_INSTALLED","command_id":"${safe(catalogTool.command)}"}"""
        }

        val parsedTarget = targetValue.takeIf { it.isNotBlank() }?.let { TargetParser.parse(it) }
        if (targetValue.isNotBlank() && parsedTarget == null) {
            return@withContext """{"status":"BLOCKED","reason":"INVALID_TARGET"}"""
        }
        if (parsedTarget?.isPublic() == true && !authorizationConfirmed) {
            return@withContext """{"status":"REQUIRES_AUTHORIZATION","reason":"PUBLIC_TARGET_REQUIRES_USER_CONFIRMATION"}"""
        }
        if (catalogTool.riskLevel == ToolRiskLevel.HIGH && !authorizationConfirmed) {
            return@withContext """{"status":"REQUIRES_AUTHORIZATION","reason":"HIGH_RISK_TOOL_REQUIRES_EXPLICIT_CONFIRMATION","command_id":"${safe(catalogTool.command)}"}"""
        }
        if (commandArgs.any { !isSafeArgument(it) }) {
            return@withContext """{"status":"BLOCKED","reason":"UNSAFE_ARGUMENTS","command_id":"${safe(catalogTool.command)}"}"""
        }

        val finalArgs = commandArgs.ifEmpty {
            targetValue.takeIf { it.isNotBlank() }?.let { listOf(it) }.orEmpty()
        }
        val sessionId = auditRepository.startSession(
            AuditSessionEntity(
                title = "AI Termux: ${catalogTool.command}",
                taskType = "TERMUX_${catalogTool.command.uppercase()}",
                target = targetValue.ifBlank { "LOCAL_TERMUX" },
                targetType = parsedTarget?.type?.javaClass?.simpleName ?: "LOCAL_COMMAND",
                authorizationMode = if (authorizationConfirmed) "AI_USER_CONFIRMED" else "LOCAL_OR_LOW_RISK",
                status = "SENT_TO_TERMUX",
                startedAt = System.currentTimeMillis(),
                summary = "Sent ${catalogTool.command} ${finalArgs.joinToString(" ")} to Termux via RUN_COMMAND."
            )
        )
        val sent = TermuxBridge.executeCommand(context, catalogTool.command, finalArgs.toTypedArray())
        if (sent) {
            """{"status":"SENT_TO_TERMUX","sessionId":$sessionId,"command":"${safe(catalogTool.command)}","arguments":"${safe(finalArgs.joinToString(" "))}","risk":"${catalogTool.riskLevel.name}","note":"Command launched in Termux background. Review Termux or audit history for execution context."}"""
        } else {
            auditRepository.getSessionById(sessionId)?.let { session ->
                auditRepository.updateSession(
                    session.copy(
                        status = "FAILED",
                        finishedAt = System.currentTimeMillis(),
                        summary = "Termux command launch failed."
                    )
                )
            }
            """{"status":"ERROR","sessionId":$sessionId,"reason":"TERMUX_RUN_COMMAND_FAILED","command":"${safe(catalogTool.command)}"}"""
        }
    }

    private suspend fun runPing(target: String): String = withContext(Dispatchers.IO) {
        val startedAt = System.currentTimeMillis()
        val sessionId = startSession("AI Ping Recon: $target", "PING", target, "IP/DOMAIN", startedAt)
        val result = runCatching {
            val address = InetAddress.getByName(target)
            val reachable = address.isReachable(3000)
            val body = if (reachable) {
                "Host reachable at ${address.hostAddress}"
            } else {
                "Host did not respond to ICMP reachability probe"
            }
            saveFinding(sessionId, "Network", "AI Ping Recon", body, if (reachable) Severity.INFO else Severity.LOW)
            finishSession(sessionId, body)
            """{"status":"OK","sessionId":$sessionId,"reachable":$reachable,"address":"${address.hostAddress}"}"""
        }.getOrElse { error ->
            finishSession(sessionId, "Ping failed: ${error.message}")
            """{"status":"ERROR","sessionId":$sessionId,"reason":"${safe(error.message)}"}"""
        }
        result
    }

    private suspend fun runDnsLookup(target: String): String = withContext(Dispatchers.IO) {
        val sessionId = startSession("AI DNS Lookup: $target", "DNS_LOOKUP", target, "DOMAIN/IP", System.currentTimeMillis())
        runCatching {
            val addresses = InetAddress.getAllByName(target).mapNotNull { it.hostAddress }.distinct()
            val body = "Resolved addresses: ${addresses.joinToString(", ")}"
            saveFinding(sessionId, "DNS", "AI DNS Lookup", body, Severity.INFO)
            finishSession(sessionId, body)
            val jsonAddresses = addresses.joinToString(prefix = "[", postfix = "]") { """"$it"""" }
            """{"status":"OK","sessionId":$sessionId,"addresses":$jsonAddresses}"""
        }.getOrElse { error ->
            finishSession(sessionId, "DNS lookup failed: ${error.message}")
            """{"status":"ERROR","sessionId":$sessionId,"reason":"${safe(error.message)}"}"""
        }
    }

    private suspend fun runWebHeaders(target: String, taskType: String): String = withContext(Dispatchers.IO) {
        val url = if (target.startsWith("http://") || target.startsWith("https://")) target else "https://$target"
        val sessionId = startSession("AI Web Analysis: $target", taskType.uppercase(), target, "WEB", System.currentTimeMillis())
        runCatching {
            val response = httpClient.newCall(Request.Builder().url(url).build()).execute()
            response.use {
                val server = it.header("Server") ?: "Not disclosed"
                val body = "HTTP ${it.code}; protocol=${it.protocol}; server=$server"
                saveFinding(sessionId, "Web", "AI Web Header Analysis", body, Severity.INFO)
                finishSession(sessionId, body)
                """{"status":"OK","sessionId":$sessionId,"httpCode":${it.code},"server":"${safe(server)}"}"""
            }
        }.getOrElse { error ->
            finishSession(sessionId, "Web analysis failed: ${error.message}")
            """{"status":"ERROR","sessionId":$sessionId,"reason":"${safe(error.message)}"}"""
        }
    }

    private fun netDiscGuidance(target: String): String {
        val profiles = NetworkToolSuggestions.profiles.joinToString(prefix = "[", postfix = "]") { profile ->
            """{"profile":"${profile.label}","ports":"${profile.ports.joinToString(",")}","description":"${safe(profile.description)}"}"""
        }
        return """{"status":"SUGGESTED","target":"${safe(target)}","tool":"net_disc","nextTools":["ping","port_scan","banner_grab","dns_lookup","web_headers"],"portProfiles":$profiles,"note":"Open NODES_DISCOVERY in the dashboard to scan BLE/LAN; run port_scan with a selected port profile for a specific authorized node."}"""
    }

    private suspend fun enqueuePortScan(target: String, ports: List<Int>): String {
        val sessionId = startSession("AI Port Scan: $target", "PORT_SCAN", target, "IP/DOMAIN", System.currentTimeMillis())
        val workRequest = OneTimeWorkRequestBuilder<AuditWorker>()
            .setInputData(
                workDataOf(
                    "SESSION_ID" to sessionId,
                    "TARGET" to target,
                    "TASK_TYPE" to "PORT_SCAN",
                    "PORTS" to ports.joinToString(",")
                )
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
        return """{"status":"QUEUED","sessionId":$sessionId,"workId":"${workRequest.id}","ports":"${ports.joinToString(",")}","note":"Port scan is running in WorkManager; results will appear in history."}"""
    }

    private suspend fun startSession(
        title: String,
        taskType: String,
        target: String,
        targetType: String,
        startedAt: Long
    ): Long {
        return auditRepository.startSession(
            AuditSessionEntity(
                title = title,
                taskType = taskType,
                target = target,
                targetType = targetType,
                authorizationMode = "AI_CONFIRMED",
                status = "RUNNING",
                startedAt = startedAt
            )
        )
    }

    private suspend fun saveFinding(
        sessionId: Long,
        category: String,
        title: String,
        body: String,
        severity: Severity
    ) {
        auditRepository.saveResult(
            AuditResultEntity(
                sessionId = sessionId,
                severity = severity.name,
                category = category,
                findingTitle = title,
                findingBody = body
            )
        )
    }

    private suspend fun finishSession(sessionId: Long, summary: String) {
        auditRepository.getSessionById(sessionId)?.let { session ->
            auditRepository.updateSession(
                session.copy(
                    status = "FINISHED",
                    finishedAt = System.currentTimeMillis(),
                    summary = summary
                )
            )
        }
    }

    private fun safe(value: String?): String {
        return value.orEmpty().replace("\\", "\\\\").replace("\"", "\\\"")
    }

    private fun findCatalogTool(commandId: String): ToolCatalogItem? {
        return AndraxToolCatalog.allTools.firstOrNull {
            it.command.equals(commandId, ignoreCase = true) ||
                it.displayName.equals(commandId, ignoreCase = true)
        }
    }

    private fun parseCommandArguments(raw: String): List<String> {
        if (raw.isBlank()) return emptyList()
        return Regex("""[^\s"]+|"([^"]*)"""")
            .findAll(raw)
            .map { match -> match.groups[1]?.value ?: match.value }
            .filter { it.isNotBlank() }
            .take(32)
            .toList()
    }

    private fun isSafeArgument(argument: String): Boolean {
        return argument.length <= 256 &&
            !argument.contains('\u0000') &&
            !argument.contains('\n') &&
            !argument.contains('\r')
    }

    private data class ToolSummary(
        val id: String,
        val name: String,
        val category: String,
        val directLaunch: Boolean
    )
}
