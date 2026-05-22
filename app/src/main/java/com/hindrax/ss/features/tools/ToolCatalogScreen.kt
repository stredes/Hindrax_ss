package com.hindrax.ss.features.tools

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hindrax.ss.R
import com.hindrax.ss.domain.tools.AndraxInternalArchitecture
import com.hindrax.ss.domain.tools.AndraxToolCatalog
import com.hindrax.ss.domain.tools.HindraxEnvironmentGuide
import com.hindrax.ss.domain.tools.HindraxToolArchitecture
import com.hindrax.ss.domain.tools.ToolCategory
import com.hindrax.ss.domain.tools.ToolCatalogItem
import com.hindrax.ss.domain.tools.ToolRiskLevel
import com.hindrax.ss.presentation.tasks.AsciiBanners

private val NeonGreen = Color(0xFF64FF00)
private val NeonGreenDim = Color(0xFF1F7A00)
private val HindraxBlack = Color(0xFF020302)
private val HindraxPanel = Color(0xFF071007)
private val HindraxPanelAlt = Color(0xFF0B140B)
private val AlertRed = Color(0xFFFF1A1A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolCatalogScreen(onBack: () -> Unit) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("TOOL_CATALOG", fontFamily = FontFamily.Monospace) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NeonGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HindraxBlack,
                    titleContentColor = NeonGreen
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(HindraxBlack)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                CatalogSummaryCard()
            }

            item {
                EnvironmentGuideCard()
            }

            item {
                ArchitectureCard(
                    title = "ANDRAX_REFERENCE_STACK",
                    iconTint = Color.Cyan,
                    layers = AndraxInternalArchitecture.layers + AndraxInternalArchitecture.dependencies
                )
            }

            item {
                ArchitectureCard(
                    title = "HINDRAX_TARGET_ARCHITECTURE",
                    iconTint = Color.Green,
                    layers = HindraxToolArchitecture.layers
                )
            }

            items(AndraxToolCatalog.categories) { category ->
                CategoryCard(category = category)
            }
        }
    }
}

@Composable
private fun CatalogSummaryCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = HindraxPanel),
        border = BorderStroke(1.dp, NeonGreen),
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.hindrax_logo),
                    contentDescription = "Hindrax",
                    modifier = Modifier.size(54.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "HINDRAX TOOLSET INDEX",
                        color = NeonGreen,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        "ANDROID + LINUX USERSPACE + SAFETY GATE",
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                }
                Icon(Icons.Default.Security, contentDescription = null, tint = AlertRed)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = AsciiBanners.HINDRAX_TOOL_CATALOG,
                color = NeonGreen,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                lineHeight = 10.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                softWrap = false
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "ANDRAX_KNOWN_TOOLSET_RECONSTRUCTION",
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Catalogo no-oficial basado en herramientas conocidas por categoria. ANDRAX no tuvo una lista estable y verificable para todas las builds.",
                color = Color.Gray,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                "CATEGORIES=${AndraxToolCatalog.categories.size} TOOLS=${AndraxToolCatalog.allTools.size}",
                color = NeonGreen,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun EnvironmentGuideCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = HindraxPanel),
        border = BorderStroke(1.dp, NeonGreen),
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "[ TUTORIAL ]",
                color = NeonGreen,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            HindraxEnvironmentGuide.sections.forEach { section ->
                Text(
                    text = ">> ${section.title}",
                    color = if (section.title == "TUTORIAL") NeonGreen else Color.Cyan,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
                section.steps.forEach {
                    Text(
                        text = "  - $it",
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ArchitectureCard(title: String, iconTint: Color, layers: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = HindraxPanelAlt),
        border = BorderStroke(1.dp, NeonGreenDim),
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccountTree, contentDescription = null, tint = iconTint)
                Spacer(modifier = Modifier.width(10.dp))
                Text(title, color = Color.White, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(10.dp))
            layers.forEachIndexed { index, layer ->
                Text(
                    text = "${index + 1}. $layer",
                    color = if (layer in HindraxToolArchitecture.layers) NeonGreen else Color.Gray,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryCard(category: ToolCategory) {
    val maxRisk = category.tools.maxOfOrNull { it.riskLevel } ?: ToolRiskLevel.LOW
    val riskColor = when (maxRisk) {
        ToolRiskLevel.LOW -> NeonGreen
        ToolRiskLevel.MEDIUM -> Color.Yellow
        ToolRiskLevel.HIGH -> AlertRed
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = HindraxPanel),
        border = BorderStroke(1.dp, riskColor),
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Category, contentDescription = null, tint = riskColor, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(category.name.uppercase(), color = Color.White, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("${category.tools.size} tools / risk=$maxRisk", color = riskColor, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                category.tools.forEach { tool ->
                    SuggestionChip(
                        onClick = {},
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = HindraxBlack,
                            labelColor = NeonGreen
                        ),
                        border = BorderStroke(1.dp, NeonGreenDim),
                        label = { Text(tool.displayName, fontFamily = FontFamily.Monospace, fontSize = 10.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            CapabilityBox(title = "CAPABILITIES", values = category.capabilities, tint = NeonGreen)

            if (category.requirements.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                CapabilityBox(title = "REQUIRES", values = category.requirements, tint = Color.Yellow)
            }

            Spacer(modifier = Modifier.height(10.dp))
            ToolTutorialSection(tools = category.tools)
        }
    }
}

@Composable
private fun ToolTutorialSection(tools: List<ToolCatalogItem>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(HindraxBlack)
            .border(1.dp, NeonGreen)
            .padding(10.dp)
    ) {
        Column {
            Text(
                text = "[ TUTORIAL ]",
                color = NeonGreen,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            tools.forEach { tool ->
                ToolTutorialItem(tool = tool)
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun ToolTutorialItem(tool: ToolCatalogItem) {
    val riskColor = when (tool.riskLevel) {
        ToolRiskLevel.LOW -> NeonGreen
        ToolRiskLevel.MEDIUM -> Color.Yellow
        ToolRiskLevel.HIGH -> AlertRed
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, NeonGreenDim)
            .padding(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = tool.command.uppercase(),
                color = NeonGreen,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = tool.riskLevel.name,
                color = riskColor,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp
            )
        }
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = tool.tutorial.authorizedUse,
            color = Color.Gray,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            lineHeight = 14.sp
        )
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = "$ ${tool.tutorial.commandExample}",
            color = Color.Cyan,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            lineHeight = 14.sp
        )
        Spacer(modifier = Modifier.height(5.dp))
        tool.tutorial.workflow.take(2).forEach {
            Text(
                text = it,
                color = Color.DarkGray,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                lineHeight = 12.sp
            )
        }
    }
}

@Composable
private fun CapabilityBox(title: String, values: List<String>, tint: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 42.dp)
            .background(HindraxBlack)
            .border(1.dp, NeonGreenDim)
            .padding(10.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Build, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(title, color = tint, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            values.forEach {
                Text("- $it", color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 10.sp, lineHeight = 14.sp)
            }
        }
    }
}
