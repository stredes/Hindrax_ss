package com.hindrax.ss.features.termux

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hindrax.ss.data.entity.AuditSessionEntity
import com.hindrax.ss.data.repository.AuditRepository
import com.hindrax.ss.domain.tools.AndraxToolCatalog
import com.hindrax.ss.domain.tools.ToolCatalogItem
import com.hindrax.ss.domain.tools.ToolCategory
import com.hindrax.ss.domain.tools.ToolRiskLevel
import com.hindrax.ss.domain.tools.ToolWorkflowPlanner
import com.hindrax.ss.termux.TermuxBridge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TermuxScriptsUiState(
    val isTermuxInstalled: Boolean = false,
    val logs: String = "",
    val query: String = "",
    val selectedCategoryId: String = "network-recon",
    val selectedToolCommand: String? = null,
    val target: String = "",
    val customArguments: String = "",
    val authorizationConfirmed: Boolean = false,
    val categories: List<ToolCategory> = AndraxToolCatalog.categories,
    val visibleTools: List<ToolCatalogItem> = AndraxToolCatalog.categoryById("network-recon").tools,
    val selectedTool: ToolCatalogItem? = AndraxToolCatalog.categoryById("network-recon").tools.firstOrNull(),
    val commandPreview: String = "",
    val canRunSelectedTool: Boolean = false,
    val workflowTools: List<ToolCatalogItem> = emptyList(),
    val workflowPreview: String = "",
    val canRunWorkflow: Boolean = false
)

class TermuxScriptsViewModel(
    private val auditRepository: AuditRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(TermuxScriptsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        recalculate()
    }

    fun checkTermux(context: Context) {
        _uiState.value = _uiState.value.copy(
            isTermuxInstalled = TermuxBridge.isTermuxInstalled(context)
        )
        recalculate()
    }

    fun onQueryChange(value: String) {
        _uiState.value = _uiState.value.copy(query = value)
        recalculate()
    }

    fun selectCategory(categoryId: String) {
        val category = AndraxToolCatalog.categories.firstOrNull { it.id == categoryId } ?: return
        _uiState.value = _uiState.value.copy(
            selectedCategoryId = category.id,
            selectedToolCommand = category.tools.firstOrNull()?.command,
            customArguments = "",
            authorizationConfirmed = false
        )
        recalculate()
    }

    fun selectTool(tool: ToolCatalogItem) {
        _uiState.value = _uiState.value.copy(
            selectedToolCommand = tool.command,
            customArguments = "",
            authorizationConfirmed = false
        )
        recalculate()
    }

    fun onTargetChange(value: String) {
        _uiState.value = _uiState.value.copy(target = value)
        recalculate()
    }

    fun onCustomArgumentsChange(value: String) {
        _uiState.value = _uiState.value.copy(customArguments = value)
        recalculate()
    }

    fun onAuthorizationConfirmedChange(value: Boolean) {
        _uiState.value = _uiState.value.copy(authorizationConfirmed = value)
        recalculate()
    }

    fun executeSelectedTool(context: Context) {
        val state = _uiState.value
        val tool = state.selectedTool ?: return
        if (!state.canRunSelectedTool) return

        viewModelScope.launch {
            val args = commandArgumentsFor(tool, state.target, state.customArguments)
            appendLog("RUN ${tool.command} ${args.joinToString(" ")}")

            val sessionId = auditRepository.startSession(
                AuditSessionEntity(
                    title = "Termux Tool: ${tool.command}",
                    taskType = "TERMUX_${tool.command.uppercase()}",
                    target = state.target.ifBlank { "LOCAL_TERMUX" },
                    targetType = "TERMUX_TOOL",
                    authorizationMode = if (state.authorizationConfirmed) "USER_CONFIRMED" else "LOW_RISK_OR_LOCAL",
                    status = "SENT_TO_TERMUX",
                    startedAt = System.currentTimeMillis(),
                    summary = "Command: ${tool.command} ${args.joinToString(" ")}"
                )
            )

            val sent = TermuxBridge.executeCommand(context, tool.command, args.toTypedArray())
            appendLog(
                if (sent) {
                    "SENT_TO_TERMUX session=$sessionId. Revisa Termux para salida en tiempo real."
                } else {
                    "ERROR_TERMUX_RUN_COMMAND_FAILED session=$sessionId"
                }
            )
        }
    }

    fun addSelectedToolToWorkflow() {
        val tool = _uiState.value.selectedTool ?: return
        _uiState.value = _uiState.value.copy(
            workflowTools = ToolWorkflowPlanner.addTool(_uiState.value.workflowTools, tool)
        )
        recalculate()
    }

    fun removeWorkflowTool(command: String) {
        _uiState.value = _uiState.value.copy(
            workflowTools = ToolWorkflowPlanner.removeTool(_uiState.value.workflowTools, command)
        )
        recalculate()
    }

    fun clearWorkflow() {
        _uiState.value = _uiState.value.copy(workflowTools = emptyList())
        recalculate()
    }

    fun executeWorkflow(context: Context) {
        val state = _uiState.value
        if (!state.canRunWorkflow) return

        viewModelScope.launch {
            appendLog("WORKFLOW_START steps=${state.workflowTools.size} target=${state.target.ifBlank { "LOCAL_TERMUX" }}")
            state.workflowTools.forEachIndexed { index, tool ->
                val args = commandArgumentsFor(tool, state.target, "")
                appendLog("WORKFLOW_STEP ${index + 1}/${state.workflowTools.size}: ${tool.command} ${args.joinToString(" ")}")
                val sent = TermuxBridge.executeCommand(context, tool.command, args.toTypedArray())
                appendLog(if (sent) "WORKFLOW_SENT ${tool.command}" else "WORKFLOW_ERROR ${tool.command}")
            }
            auditRepository.startSession(
                AuditSessionEntity(
                    title = "Termux Workflow: ${state.workflowTools.joinToString(" -> ") { it.command }}",
                    taskType = "TERMUX_WORKFLOW",
                    target = state.target.ifBlank { "LOCAL_TERMUX" },
                    targetType = "TERMUX_WORKFLOW",
                    authorizationMode = if (state.authorizationConfirmed) "USER_CONFIRMED" else "LOW_RISK_OR_LOCAL",
                    status = "SENT_TO_TERMUX",
                    startedAt = System.currentTimeMillis(),
                    summary = state.workflowPreview
                )
            )
            appendLog("WORKFLOW_DONE")
        }
    }

    fun installSelectedToolPackage(context: Context) {
        val state = _uiState.value
        val tool = state.selectedTool ?: return
        val packageName = tool.termuxPackage ?: return
        if (!state.isTermuxInstalled) return

        viewModelScope.launch {
            appendLog("INSTALL package=$packageName for ${tool.command}")
            val sent = TermuxBridge.executeCommand(
                context = context,
                command = "pkg",
                arguments = arrayOf("install", "-y", packageName)
            )
            appendLog(
                if (sent) {
                    "INSTALL_SENT_TO_TERMUX package=$packageName"
                } else {
                    "ERROR_TERMUX_INSTALL_FAILED package=$packageName"
                }
            )
        }
    }

    private fun recalculate() {
        val state = _uiState.value
        val categoryTools = AndraxToolCatalog.categories
            .firstOrNull { it.id == state.selectedCategoryId }
            ?.tools
            .orEmpty()
        val query = state.query.trim().lowercase()
        val visibleTools = if (query.isBlank()) {
            categoryTools
        } else {
            AndraxToolCatalog.allTools.filter { tool ->
                tool.command.lowercase().contains(query) ||
                    tool.displayName.lowercase().contains(query) ||
                    tool.tutorial.authorizedUse.lowercase().contains(query)
            }
        }
        val selectedTool = visibleTools.firstOrNull { it.command == state.selectedToolCommand }
            ?: visibleTools.firstOrNull()
        val preview = selectedTool?.let {
            buildPreview(it, state.target, state.customArguments)
        }.orEmpty()
        val canRun = state.isTermuxInstalled &&
            selectedTool != null &&
            preview.isNotBlank() &&
            !preview.contains("<") &&
            (selectedTool.riskLevel != ToolRiskLevel.HIGH || state.authorizationConfirmed)
        val workflowPreview = state.workflowTools.joinToString("\n") { tool ->
            buildPreview(tool, state.target, "")
        }
        val workflowHasHighRisk = state.workflowTools.any { it.riskLevel == ToolRiskLevel.HIGH }
        val canRunWorkflow = state.isTermuxInstalled &&
            state.workflowTools.isNotEmpty() &&
            workflowPreview.isNotBlank() &&
            !workflowPreview.contains("<") &&
            (!workflowHasHighRisk || state.authorizationConfirmed)

        _uiState.value = state.copy(
            visibleTools = visibleTools,
            selectedTool = selectedTool,
            selectedToolCommand = selectedTool?.command,
            commandPreview = preview,
            canRunSelectedTool = canRun,
            workflowPreview = workflowPreview,
            canRunWorkflow = canRunWorkflow
        )
    }

    private fun buildPreview(tool: ToolCatalogItem, target: String, customArguments: String): String {
        val args = commandArgumentsFor(tool, target, customArguments)
        return listOf(tool.command)
            .plus(args)
            .joinToString(" ")
            .trim()
    }

    private fun commandArgumentsFor(
        tool: ToolCatalogItem,
        target: String,
        customArguments: String
    ): List<String> {
        val custom = customArguments.trim()
        if (custom.isNotBlank()) return splitArgs(custom)

        val targetValue = target.trim()
        val normalized = tool.tutorial.commandExample
            .substringBefore("#")
            .trim()
            .replace("<TARGET_AUTORIZADO>", targetValue)
            .replace("<DOMINIO_AUTORIZADO>", targetValue)
            .replace("<RANGO_PRIVADO_AUTORIZADO>", targetValue)
            .replace("<RANGO_AUTORIZADO>", targetValue)
            .replace("<APK_PROPIO.apk>", targetValue)
            .replace("<ARCHIVO_LAB>", targetValue)
            .replace("<BINARIO_LAB>", targetValue)
            .replace("<FIRMWARE_LAB.bin>", targetValue)
            .replace("<RECURSO_AUTORIZADO>", targetValue)

        val parts = splitArgs(normalized)
        return if (parts.firstOrNull().equals(tool.command, ignoreCase = true)) {
            parts.drop(1)
        } else {
            parts
        }
    }

    private fun splitArgs(value: String): List<String> {
        return Regex("""[^\s"]+|"([^"]*)"""")
            .findAll(value)
            .map { match -> match.groups[1]?.value ?: match.value }
            .filter { it.isNotBlank() }
            .take(48)
            .toList()
    }

    private fun appendLog(line: String) {
        _uiState.value = _uiState.value.copy(
            logs = _uiState.value.logs + line + "\n"
        )
    }
}

class TermuxScriptsViewModelFactory(private val repository: AuditRepository) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return TermuxScriptsViewModel(repository) as T
    }
}
