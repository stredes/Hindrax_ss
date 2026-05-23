package com.hindrax.ss.domain.tools

import org.junit.Assert.assertEquals
import org.junit.Test

class ToolWorkflowPlannerTest {
    @Test
    fun addsSelectedToolsWithoutDuplicates() {
        val nmap = ToolCatalogItem("nmap")
        val dig = ToolCatalogItem("dig")

        val workflow = ToolWorkflowPlanner.addTool(
            current = listOf(nmap),
            tool = dig
        )

        assertEquals(listOf("nmap", "dig"), workflow.map { it.command })
        assertEquals(workflow, ToolWorkflowPlanner.addTool(workflow, dig))
    }

    @Test
    fun presetBuildsWorkflowFromCatalogCommands() {
        val preset = ToolWorkflowPresetLibrary.presets.first { it.id == "red_basica" }

        assertEquals(listOf("nmap", "traceroute", "dig"), preset.tools.map { it.command.lowercase() })
    }
}
