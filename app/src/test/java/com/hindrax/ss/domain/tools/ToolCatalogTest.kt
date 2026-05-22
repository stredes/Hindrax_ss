package com.hindrax.ss.domain.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolCatalogTest {
    @Test
    fun andraxKnownCatalogContainsSixteenCategories() {
        assertEquals(16, AndraxToolCatalog.categories.size)
    }

    @Test
    fun networkReconCategoryContainsCoreToolsAndCapabilities() {
        val category = AndraxToolCatalog.categoryById("network-recon")

        assertTrue(category.tools.any { it.command == "nmap" })
        assertTrue(category.tools.any { it.command == "masscan" })
        assertTrue(category.capabilities.contains("Descubrimiento de hosts"))
        assertTrue(category.capabilities.contains("Escaneo TCP/UDP"))
    }

    @Test
    fun androidApkCategoryContainsReverseEngineeringTools() {
        val category = AndraxToolCatalog.categoryById("android-apk-analysis")

        assertTrue(category.tools.any { it.command == "apktool" })
        assertTrue(category.tools.any { it.command == "jadx" })
        assertTrue(category.capabilities.contains("Decompilacion APK"))
        assertTrue(category.capabilities.contains("Analisis de permisos"))
    }

    @Test
    fun wirelessCategoryPreservesHardwareRequirements() {
        val category = AndraxToolCatalog.categoryById("wireless-security")

        assertTrue(category.requirements.contains("Root"))
        assertTrue(category.requirements.contains("Adaptador compatible"))
    }

    @Test
    fun hindraxArchitectureIncludesModernSafetyAndWorkflowLayers() {
        assertTrue(HindraxToolArchitecture.layers.contains("UI Android moderna"))
        assertTrue(HindraxToolArchitecture.layers.contains("Safety Gate"))
        assertTrue(HindraxToolArchitecture.layers.contains("Task Catalog"))
        assertTrue(HindraxToolArchitecture.layers.contains("Workflow Automation"))
    }

    @Test
    fun everyToolHasTutorialSectionWithAuthorizedUsageAndCommand() {
        AndraxToolCatalog.categories.flatMap { it.tools }.forEach { tool ->
            assertEquals("TUTORIAL", tool.tutorial.section)
            assertTrue(tool.tutorial.authorizedUse.isNotBlank())
            assertTrue(tool.tutorial.commandExample.contains("<"))
            assertTrue(tool.tutorial.workflow.isNotEmpty())
        }
    }

    @Test
    fun environmentGuideDocumentsCompleteHindraxSetup() {
        assertTrue(HindraxEnvironmentGuide.sections.any { it.title == "TUTORIAL" })
        assertTrue(HindraxEnvironmentGuide.sections.any { it.title == "TERMUX_BRIDGE" })
        assertTrue(HindraxEnvironmentGuide.sections.any { it.title == "SAFETY_GATE" })
    }
}
