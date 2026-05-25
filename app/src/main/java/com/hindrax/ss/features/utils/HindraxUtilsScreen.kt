package com.hindrax.ss.features.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraManager
import android.media.MediaRecorder
import android.os.Build
import android.provider.CalendarContract
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.hindrax.ss.domain.ascii.AsciiAnimationCatalog
import com.hindrax.ss.domain.ascii.AsciiAnimationContext
import com.hindrax.ss.domain.utils.AsciiAnalogClock
import com.hindrax.ss.features.ascii.AsciiAnimationPlayer
import com.hindrax.ss.features.ascii.AsciiAnimationStrip
import kotlinx.coroutines.delay
import java.io.File
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

private data class ChecklistEntry(val text: String, val checked: Boolean)
private data class BudgetEntry(val label: String, val amount: Double)
private data class UtilityMenuItem(
    val id: UtilityId,
    val title: String,
    val category: String,
    val detail: String,
    val icon: ImageVector,
    val animationContext: AsciiAnimationContext
)

private enum class UtilityId {
    Timer,
    Stopwatch,
    Notes,
    DailyChecklist,
    Calculator,
    UnitConverter,
    System,
    Level,
    Ruler,
    VoiceRecorder,
    ShoppingChecklist,
    Budget,
    TextConverter,
    RandomPicker,
    Catalog
}

private val utilityMenuItems = listOf(
    UtilityMenuItem(UtilityId.Timer, "Temporizador", "Tiempo", "Cuenta regresiva con reloj ASCII.", Icons.Default.HourglassTop, AsciiAnimationContext.Time),
    UtilityMenuItem(UtilityId.Stopwatch, "Cronometro", "Tiempo", "Medicion por vueltas.", Icons.Default.Tune, AsciiAnimationContext.Time),
    UtilityMenuItem(UtilityId.Notes, "Notas", "Personal", "Nota rapida guardada localmente.", Icons.Default.NoteAlt, AsciiAnimationContext.Notes),
    UtilityMenuItem(UtilityId.DailyChecklist, "Checklist", "Personal", "Pendientes rapidos del dia.", Icons.Default.Checklist, AsciiAnimationContext.Checklist),
    UtilityMenuItem(UtilityId.Calculator, "Calculadora", "Calculo", "Operaciones y porcentajes.", Icons.Default.Calculate, AsciiAnimationContext.Calculator),
    UtilityMenuItem(UtilityId.UnitConverter, "Conversor", "Calculo", "Peso, distancia y temperatura.", Icons.Default.Straighten, AsciiAnimationContext.Converter),
    UtilityMenuItem(UtilityId.System, "Sistema movil", "Dispositivo", "Flash, calendario, camara y QR.", Icons.Default.FlashlightOn, AsciiAnimationContext.System),
    UtilityMenuItem(UtilityId.Level, "Nivel", "Medicion", "Inclinacion por acelerometro.", Icons.Default.WaterDrop, AsciiAnimationContext.Measure),
    UtilityMenuItem(UtilityId.Ruler, "Regla", "Medicion", "Regla visual aproximada.", Icons.Default.Straighten, AsciiAnimationContext.Measure),
    UtilityMenuItem(UtilityId.VoiceRecorder, "Grabadora", "Audio", "Notas de voz locales.", Icons.Default.Mic, AsciiAnimationContext.Audio),
    UtilityMenuItem(UtilityId.ShoppingChecklist, "Compras", "Personal", "Lista rapida de compras.", Icons.Default.Checklist, AsciiAnimationContext.Checklist),
    UtilityMenuItem(UtilityId.Budget, "Presupuesto", "Calculo", "Suma simple de gastos.", Icons.Default.Event, AsciiAnimationContext.Calculator),
    UtilityMenuItem(UtilityId.TextConverter, "Texto", "Texto", "Mayusculas, minusculas y conteo.", Icons.Default.TextFields, AsciiAnimationContext.Text),
    UtilityMenuItem(UtilityId.RandomPicker, "Aleatorio", "Decision", "Elige una opcion al azar.", Icons.Default.Casino, AsciiAnimationContext.Random),
    UtilityMenuItem(UtilityId.Catalog, "Catalogo", "Indice", "Listado completo de utilidades.", Icons.Default.QrCode, AsciiAnimationContext.Catalog)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HindraxUtilsScreen(onBack: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val accent = scheme.primary
    var selectedUtility by remember { mutableStateOf<UtilityMenuItem?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        selectedUtility?.let { "UTILS :: ${it.title.uppercase(Locale.ROOT)}" } ?: "UTILS :: DASH_MENU",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 16.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedUtility == null) {
                            onBack()
                        } else {
                            selectedUtility = null
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = scheme.background,
                    titleContentColor = accent,
                    navigationIconContentColor = accent
                )
            )
        }
    ) { padding ->
        if (selectedUtility == null) {
            DashMenuParaUtils(
                modifier = Modifier.padding(padding),
                onSelect = { selectedUtility = it }
            )
        } else {
            UtilityDetailScreen(
                item = selectedUtility!!,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun DashMenuParaUtils(
    modifier: Modifier = Modifier,
    onSelect: (UtilityMenuItem) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        item {
            Text(
                "+-[ DASH_MENU_PARA_UTILS ]----------------",
                color = scheme.primary,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Selecciona una utilidad para abrir su panel dedicado.",
                color = scheme.onSurface.copy(alpha = 0.72f),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        item {
            ToolCard("ASCII_ROUTER_ANIM", Icons.Default.QrCode) {
                AsciiAnimationPlayer(
                    spec = AsciiAnimationCatalog.forContext(AsciiAnimationContext.UtilsHub),
                    color = scheme.primary
                )
            }
        }

        utilityMenuItems.groupBy { it.category }.forEach { (category, items) ->
            item {
                Text(
                    ":: $category",
                    color = scheme.secondary,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
            items.chunked(2).forEach { rowItems ->
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        rowItems.forEach { item ->
                            UtilityMenuCard(
                                item = item,
                                onClick = { onSelect(item) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UtilityMenuCard(
    item: UtilityMenuItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Card(
        colors = CardDefaults.cardColors(containerColor = scheme.surface),
        border = BorderStroke(1.dp, scheme.primary.copy(alpha = 0.42f)),
        shape = MaterialTheme.shapes.extraSmall,
        modifier = modifier
            .height(176.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Icon(item.icon, contentDescription = null, tint = scheme.primary)
            Column {
                Text(
                    item.title.uppercase(Locale.ROOT),
                    color = scheme.onSurface,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    item.detail,
                    color = scheme.onSurface.copy(alpha = 0.68f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 13.sp
                )
            }
            AsciiAnimationPlayer(
                spec = AsciiAnimationCatalog.forContext(item.animationContext),
                color = scheme.primary.copy(alpha = 0.72f)
            )
        }
    }
}

@Composable
private fun UtilityDetailScreen(
    item: UtilityMenuItem,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        item {
            UtilityDetailHeader(item)
        }
        item {
            when (item.id) {
                UtilityId.Timer -> TimerTool()
                UtilityId.Stopwatch -> StopwatchTool()
                UtilityId.Notes -> NotesTool()
                UtilityId.DailyChecklist -> ChecklistTool(title = "CHECKLIST_DIARIA")
                UtilityId.Calculator -> CalculatorTool()
                UtilityId.UnitConverter -> UnitConverterTool()
                UtilityId.System -> SystemTools()
                UtilityId.Level -> LevelTool()
                UtilityId.Ruler -> RulerTool()
                UtilityId.VoiceRecorder -> VoiceRecorderTool()
                UtilityId.ShoppingChecklist -> ChecklistTool(title = "LISTA_DE_COMPRAS")
                UtilityId.Budget -> BudgetTool()
                UtilityId.TextConverter -> TextConverterTool()
                UtilityId.RandomPicker -> RandomPickerTool()
                UtilityId.Catalog -> UtilityCatalog()
            }
        }
    }
}

@Composable
private fun UtilityDetailHeader(item: UtilityMenuItem) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        color = scheme.surface,
        border = BorderStroke(1.dp, scheme.primary.copy(alpha = 0.38f)),
        shape = MaterialTheme.shapes.extraSmall,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(item.icon, contentDescription = null, tint = scheme.primary)
                Column {
                    Text(
                        "+-[ ${item.title.uppercase(Locale.ROOT)} ]",
                        color = scheme.primary,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        item.detail,
                        color = scheme.onSurface.copy(alpha = 0.7f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            AsciiAnimationPlayer(
                spec = AsciiAnimationCatalog.forContext(item.animationContext),
                color = scheme.primary.copy(alpha = 0.86f)
            )
        }
    }
}

@Composable
private fun ToolCard(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val animationContext = animationContextForToolTitle(title)
    Card(
        colors = CardDefaults.cardColors(containerColor = scheme.surface),
        border = BorderStroke(1.dp, scheme.primary.copy(alpha = 0.45f)),
        shape = MaterialTheme.shapes.extraSmall,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = scheme.primary)
                Text("  +-[ $title ]", color = scheme.primary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
            AsciiAnimationStrip(
                spec = AsciiAnimationCatalog.forContext(animationContext),
                color = scheme.primary.copy(alpha = 0.52f),
                modifier = Modifier.padding(top = 6.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

private fun animationContextForToolTitle(title: String): AsciiAnimationContext {
    return when {
        title.contains("TIMER", ignoreCase = true) || title.contains("CRONOMETRO", ignoreCase = true) -> AsciiAnimationContext.Time
        title.contains("NOTA", ignoreCase = true) -> AsciiAnimationContext.Notes
        title.contains("CHECKLIST", ignoreCase = true) || title.contains("COMPRAS", ignoreCase = true) -> AsciiAnimationContext.Checklist
        title.contains("CALCULADORA", ignoreCase = true) || title.contains("PRESUPUESTO", ignoreCase = true) -> AsciiAnimationContext.Calculator
        title.contains("CONVERSOR_UNIDADES", ignoreCase = true) -> AsciiAnimationContext.Converter
        title.contains("SISTEMA", ignoreCase = true) -> AsciiAnimationContext.System
        title.contains("NIVEL", ignoreCase = true) || title.contains("REGLA", ignoreCase = true) -> AsciiAnimationContext.Measure
        title.contains("GRABADORA", ignoreCase = true) -> AsciiAnimationContext.Audio
        title.contains("TEXTO", ignoreCase = true) -> AsciiAnimationContext.Text
        title.contains("ALEATORIO", ignoreCase = true) -> AsciiAnimationContext.Random
        title.contains("LISTADO", ignoreCase = true) -> AsciiAnimationContext.Catalog
        else -> AsciiAnimationContext.TemplarSeal
    }
}

@Composable
private fun TimerTool() {
    var minutesText by remember { mutableStateOf("5") }
    var totalSeconds by remember { mutableIntStateOf(5 * 60) }
    var remainingSeconds by remember { mutableIntStateOf(5 * 60) }
    var running by remember { mutableStateOf(false) }

    LaunchedEffect(running, remainingSeconds) {
        if (running && remainingSeconds > 0) {
            delay(1000)
            remainingSeconds -= 1
        } else if (remainingSeconds <= 0) {
            running = false
        }
    }

    fun applyMinutes(value: Int) {
        val seconds = value.coerceIn(1, 180) * 60
        minutesText = value.toString()
        totalSeconds = seconds
        remainingSeconds = seconds
        running = false
    }

    ToolCard("ASCII_TIMER", Icons.Default.HourglassTop) {
        Text(
            AsciiAnalogClock.render(totalSeconds, remainingSeconds),
            color = MaterialTheme.colorScheme.primary,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            lineHeight = 15.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(formatTime(remainingSeconds), fontFamily = FontFamily.Monospace, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(1, 5, 10, 25).forEach { minutes ->
                AssistChip(onClick = { applyMinutes(minutes) }, label = { Text("${minutes}m", fontFamily = FontFamily.Monospace) })
            }
        }
        OutlinedTextField(
            value = minutesText,
            onValueChange = { minutesText = it.filter(Char::isDigit).take(3) },
            label = { Text("MINUTOS", fontFamily = FontFamily.Monospace) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { if (remainingSeconds <= 0) applyMinutes(minutesText.toIntOrNull() ?: 5); running = true }) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Text("INICIAR", fontFamily = FontFamily.Monospace)
            }
            Button(onClick = { running = false }) {
                Icon(Icons.Default.Pause, contentDescription = null)
                Text("PAUSA", fontFamily = FontFamily.Monospace)
            }
            Button(onClick = { applyMinutes(minutesText.toIntOrNull() ?: 5) }) {
                Icon(Icons.Default.RestartAlt, contentDescription = null)
                Text("RESET", fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
private fun StopwatchTool() {
    var running by remember { mutableStateOf(false) }
    var elapsed by remember { mutableLongStateOf(0L) }
    val laps = remember { mutableStateListOf<Long>() }

    LaunchedEffect(running) {
        while (running) {
            delay(1000)
            elapsed += 1
        }
    }

    ToolCard("CRONOMETRO", Icons.Default.Tune) {
        Text(formatTime(elapsed.toInt()), fontFamily = FontFamily.Monospace, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { running = true }) { Text("START", fontFamily = FontFamily.Monospace) }
            Button(onClick = { running = false }) { Text("STOP", fontFamily = FontFamily.Monospace) }
            Button(onClick = { laps.add(elapsed) }) { Text("VUELTA", fontFamily = FontFamily.Monospace) }
            Button(onClick = { running = false; elapsed = 0L; laps.clear() }) { Text("RESET", fontFamily = FontFamily.Monospace) }
        }
        laps.takeLast(4).forEachIndexed { index, lap ->
            Text("LAP_${index + 1}: ${formatTime(lap.toInt())}", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
    }
}

@Composable
private fun NotesTool() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("hindrax_utils", Context.MODE_PRIVATE) }
    var note by remember { mutableStateOf(prefs.getString("quick_note", "").orEmpty()) }
    ToolCard("NOTAS_RAPIDAS", Icons.Default.NoteAlt) {
        OutlinedTextField(
            value = note,
            onValueChange = {
                note = it
                prefs.edit().putString("quick_note", it).apply()
            },
            label = { Text("NOTA_LOCAL", fontFamily = FontFamily.Monospace) },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ChecklistTool(title: String) {
    val entries = remember { mutableStateListOf<ChecklistEntry>() }
    var draft by remember { mutableStateOf("") }
    ToolCard(title, Icons.Default.Checklist) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                label = { Text("ITEM", fontFamily = FontFamily.Monospace) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {
                if (draft.isNotBlank()) {
                    entries.add(ChecklistEntry(draft.trim(), false))
                    draft = ""
                }
            }) {
                Icon(Icons.Default.Add, contentDescription = "Agregar")
            }
        }
        entries.forEachIndexed { index, entry ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = entry.checked,
                    onCheckedChange = { entries[index] = entry.copy(checked = it) }
                )
                Text(entry.text, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
private fun CalculatorTool() {
    var a by remember { mutableStateOf("") }
    var b by remember { mutableStateOf("") }
    var op by remember { mutableStateOf("+") }
    val first = a.toDoubleOrNull()
    val second = b.toDoubleOrNull()
    val result = if (first != null && second != null) {
        when (op) {
            "-" -> first - second
            "*" -> first * second
            "/" -> if (second == 0.0) null else first / second
            "%" -> first * second / 100.0
            else -> first + second
        }
    } else null
    ToolCard("CALCULADORA", Icons.Default.Calculate) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(a, { a = numericInput(it) }, label = { Text("A") }, modifier = Modifier.weight(1f), singleLine = true)
            OutlinedTextField(b, { b = numericInput(it) }, label = { Text("B") }, modifier = Modifier.weight(1f), singleLine = true)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("+", "-", "*", "/", "%").forEach { symbol ->
                AssistChip(onClick = { op = symbol }, label = { Text(symbol) })
            }
        }
        Text("RESULTADO: ${result?.let(::formatNumber) ?: "--"}", fontFamily = FontFamily.Monospace, fontSize = 18.sp)
    }
}

@Composable
private fun UnitConverterTool() {
    var value by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf("KG_LB") }
    val number = value.toDoubleOrNull()
    val result = number?.let {
        when (mode) {
            "KG_LB" -> it * 2.20462
            "LB_KG" -> it / 2.20462
            "KM_MI" -> it * 0.621371
            "MI_KM" -> it / 0.621371
            "C_F" -> it * 9.0 / 5.0 + 32.0
            else -> (it - 32.0) * 5.0 / 9.0
        }
    }
    ToolCard("CONVERSOR_UNIDADES", Icons.Default.Straighten) {
        OutlinedTextField(value, { value = numericInput(it) }, label = { Text("VALOR") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf("KG_LB", "LB_KG", "KM_MI", "MI_KM", "C_F", "F_C").forEach {
                AssistChip(onClick = { mode = it }, label = { Text(it, fontSize = 10.sp) })
            }
        }
        Text("RESULTADO: ${result?.let(::formatNumber) ?: "--"}", fontFamily = FontFamily.Monospace, fontSize = 18.sp)
    }
}

@Composable
private fun SystemTools() {
    val context = LocalContext.current
    var torchOn by remember { mutableStateOf(false) }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    ToolCard("SISTEMA_MOVIL", Icons.Default.FlashlightOn) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                val cameraId = cameraManager.cameraIdList.firstOrNull()
                if (cameraId == null) {
                    toast(context, "SIN_FLASH")
                } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    cameraPermission.launch(Manifest.permission.CAMERA)
                } else {
                    val nextState = !torchOn
                    runCatching {
                        cameraManager.setTorchMode(cameraId, nextState)
                        torchOn = nextState
                    }.onFailure {
                        toast(context, "FLASH_NO_DISPONIBLE")
                    }
                }
            }) {
                Icon(Icons.Default.FlashlightOn, contentDescription = null)
                Text(if (torchOn) "FLASH_OFF" else "FLASH_ON", fontFamily = FontFamily.Monospace)
            }
            Button(onClick = {
                val intent = Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI)
                runCatching { context.startActivity(intent) }.onFailure { toast(context, "SIN_APP_CALENDARIO") }
            }) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null)
                Text("CALENDARIO", fontFamily = FontFamily.Monospace)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                runCatching { context.startActivity(Intent(MediaStore.ACTION_IMAGE_CAPTURE)) }
                    .onFailure { toast(context, "SIN_APP_CAMARA") }
            }) {
                Icon(Icons.Default.Description, contentDescription = null)
                Text("DOC_SCAN", fontFamily = FontFamily.Monospace)
            }
            Button(onClick = {
                val intent = Intent("com.google.zxing.client.android.SCAN")
                runCatching { context.startActivity(intent) }.onFailure { toast(context, "Instala un lector QR compatible") }
            }) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                Text("QR_SCAN", fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
private fun LevelTool() {
    val context = LocalContext.current
    var pitch by remember { mutableDoubleStateOf(0.0) }
    var roll by remember { mutableDoubleStateOf(0.0) }
    DisposableEffect(Unit) {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val x = event.values[0].toDouble()
                val y = event.values[1].toDouble()
                val z = event.values[2].toDouble().coerceAtLeast(0.1)
                roll = Math.toDegrees(kotlin.math.atan2(x, z))
                pitch = Math.toDegrees(kotlin.math.atan2(y, z))
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (sensor != null) manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { manager.unregisterListener(listener) }
    }
    ToolCard("NIVEL", Icons.Default.WaterDrop) {
        Text("PITCH: ${pitch.roundToInt()}  ROLL: ${roll.roundToInt()}", fontFamily = FontFamily.Monospace)
        Canvas(modifier = Modifier.fillMaxWidth().height(70.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            drawCircle(Color.Gray, radius = 28.dp.toPx(), center = center, style = Stroke(2.dp.toPx()))
            drawCircle(
                Color.Green,
                radius = 8.dp.toPx(),
                center = Offset(
                    center.x + roll.toFloat().coerceIn(-45f, 45f),
                    center.y + pitch.toFloat().coerceIn(-25f, 25f)
                )
            )
        }
    }
}

@Composable
private fun RulerTool() {
    ToolCard("REGLA", Icons.Default.Straighten) {
        Text("0----1----2----3----4----5----6----7----8----9----10 cm", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        Canvas(modifier = Modifier.fillMaxWidth().height(42.dp)) {
            val step = size.width / 10f
            repeat(11) { index ->
                val x = step * index
                drawLine(Color.Green, Offset(x, 0f), Offset(x, if (index % 5 == 0) size.height else size.height * 0.65f), 2f)
            }
        }
    }
}

@Composable
private fun VoiceRecorderTool() {
    val context = LocalContext.current
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var status by remember { mutableStateOf("READY") }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    DisposableEffect(Unit) {
        onDispose {
            recorder?.runCatching {
                stop()
                release()
            }
        }
    }
    ToolCard("GRABADORA_VOZ", Icons.Default.Mic) {
        Text(status, fontFamily = FontFamily.Monospace)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    permission.launch(Manifest.permission.RECORD_AUDIO)
                    return@Button
                }
                val file = File(context.getExternalFilesDir(null), "hindrax_voice_${System.currentTimeMillis()}.m4a")
                recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
                runCatching {
                    recorder?.apply {
                        setAudioSource(MediaRecorder.AudioSource.MIC)
                        setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                        setOutputFile(file.absolutePath)
                        prepare()
                        start()
                    }
                }.onSuccess {
                    status = "REC: ${file.name}"
                }.onFailure {
                    recorder?.runCatching { release() }
                    recorder = null
                    status = "ERROR_GRABACION"
                }
            }) {
                Icon(Icons.Default.Mic, contentDescription = null)
                Text("REC", fontFamily = FontFamily.Monospace)
            }
            Button(onClick = {
                recorder?.runCatching {
                    stop()
                    release()
                }
                recorder = null
                status = "GUARDADO_EN_APP_FILES"
            }) {
                Text("STOP", fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
private fun BudgetTool() {
    val entries = remember { mutableStateListOf<BudgetEntry>() }
    var label by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    val total = entries.sumOf { it.amount }
    ToolCard("PRESUPUESTO", Icons.Default.Event) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(label, { label = it }, label = { Text("CONCEPTO") }, modifier = Modifier.weight(1f), singleLine = true)
            OutlinedTextField(amount, { amount = numericInput(it) }, label = { Text("MONTO") }, modifier = Modifier.weight(1f), singleLine = true)
            IconButton(onClick = {
                val parsed = amount.toDoubleOrNull()
                if (label.isNotBlank() && parsed != null) {
                    entries.add(BudgetEntry(label.trim(), parsed))
                    label = ""
                    amount = ""
                }
            }) { Icon(Icons.Default.Add, contentDescription = "Agregar") }
        }
        entries.takeLast(5).forEach { Text("${it.label}: ${formatNumber(it.amount)}", fontFamily = FontFamily.Monospace, fontSize = 12.sp) }
        Text("TOTAL: ${formatNumber(total)}", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TextConverterTool() {
    val clipboard = LocalClipboardManager.current
    var text by remember { mutableStateOf("") }
    val cleaned = text.trim().replace(Regex("\\s+"), " ")
    ToolCard("CONVERSOR_TEXTO", Icons.Default.TextFields) {
        OutlinedTextField(text, { text = it }, label = { Text("TEXTO") }, minLines = 3, modifier = Modifier.fillMaxWidth())
        Text("WORDS: ${cleaned.split(" ").filter { it.isNotBlank() }.size}  CHARS: ${text.length}", fontFamily = FontFamily.Monospace)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { text = text.uppercase(Locale.ROOT) }) { Text("MAYUS", fontFamily = FontFamily.Monospace) }
            Button(onClick = { text = text.lowercase(Locale.ROOT) }) { Text("MINUS", fontFamily = FontFamily.Monospace) }
            Button(onClick = { text = cleaned }) { Text("LIMPIAR", fontFamily = FontFamily.Monospace) }
            Button(onClick = { clipboard.setText(AnnotatedString(text)) }) {
                Icon(Icons.Default.ContentCopy, contentDescription = null)
                Text("COPY", fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
private fun RandomPickerTool() {
    var text by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf("--") }
    ToolCard("SELECTOR_ALEATORIO", Icons.Default.Casino) {
        OutlinedTextField(text, { text = it }, label = { Text("OPCIONES_SEPARADAS_POR_COMA") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            val options = text.split(",").map { it.trim() }.filter { it.isNotBlank() }
            selected = options.randomOrNull(Random(System.currentTimeMillis())) ?: "--"
        }) { Text("ELEGIR", fontFamily = FontFamily.Monospace) }
        Text("RESULT: $selected", fontFamily = FontFamily.Monospace, fontSize = 18.sp)
    }
}

@Composable
private fun UtilityCatalog() {
    val utilities = listOf(
        "Temporizador", "Cronometro", "Notas rapidas", "Checklist diaria", "Calculadora",
        "Conversor de unidades", "Recordatorios", "Calendario rapido", "Linterna", "Nivel",
        "Regla", "Brujula", "Generador de QR", "Lector QR", "Escaner de documentos",
        "Grabadora de voz", "Lista de compras", "Presupuesto", "Conversor de texto", "Selector aleatorio"
    )
    ToolCard("LISTADO_UTILS", Icons.Default.QrCode) {
        utilities.forEach { name ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                shape = MaterialTheme.shapes.extraSmall,
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
            ) {
                Text(name, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(8.dp))
            }
        }
    }
}

private fun numericInput(value: String): String {
    return value.filter { it.isDigit() || it == '.' || it == '-' }.take(12)
}

private fun formatTime(totalSeconds: Int): String {
    val minutes = totalSeconds.coerceAtLeast(0) / 60
    val seconds = totalSeconds.coerceAtLeast(0) % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}

private fun formatNumber(value: Double): String {
    return if (abs(value % 1.0) < 0.0001) {
        value.toLong().toString()
    } else {
        String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
    }
}

private fun toast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}
