package com.hindrax.ss.features.utils

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hindrax.ss.domain.utils.AsciiAnalogClock
import kotlinx.coroutines.delay
import java.util.Locale

private data class UtilityItem(
    val name: String,
    val detail: String
)

private val everydayUtilities = listOf(
    UtilityItem("Temporizador", "Cuenta regresiva con reloj ASCII analogico."),
    UtilityItem("Cronometro", "Medicion de tiempo transcurrido por vueltas."),
    UtilityItem("Notas rapidas", "Texto corto local para compras, ideas y pendientes."),
    UtilityItem("Checklist diaria", "Lista simple de tareas personales reutilizables."),
    UtilityItem("Calculadora", "Operaciones basicas y porcentajes."),
    UtilityItem("Conversor de unidades", "Peso, distancia, volumen, temperatura y moneda manual."),
    UtilityItem("Recordatorios", "Avisos locales por fecha y hora."),
    UtilityItem("Calendario rapido", "Eventos simples sincronizables con el calendario del telefono."),
    UtilityItem("Linterna", "Acceso rapido al flash del dispositivo."),
    UtilityItem("Nivel", "Herramienta de inclinacion usando sensores del celular."),
    UtilityItem("Regla", "Medicion aproximada en pantalla."),
    UtilityItem("Brujula", "Orientacion simple con magnetometro."),
    UtilityItem("Generador de QR", "Crear codigos QR para texto, links o contactos."),
    UtilityItem("Lector QR", "Escanear codigos QR cotidianos."),
    UtilityItem("Escaner de documentos", "Guardar fotos recortadas como documentos."),
    UtilityItem("Grabadora de voz", "Notas de audio locales."),
    UtilityItem("Lista de compras", "Agregar productos comunes y cantidades."),
    UtilityItem("Presupuesto", "Registro simple de ingresos/gastos."),
    UtilityItem("Conversor de texto", "Mayusculas, minusculas, limpiar espacios y contar palabras."),
    UtilityItem("Selector aleatorio", "Elegir un item al azar desde una lista.")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HindraxUtilsScreen(onBack: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val panel = scheme.surface
    val accent = scheme.primary
    val muted = scheme.onSurface.copy(alpha = 0.62f)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("UTILS :: DAILY_TOOLS", fontFamily = FontFamily.Monospace, fontSize = 16.sp)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(scheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = panel),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.65f)),
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("+-[ ASCII_TIMER ]----------------", color = accent, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            AsciiAnalogClock.render(totalSeconds, remainingSeconds),
                            color = accent,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            lineHeight = 15.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(formatTime(remainingSeconds), color = scheme.onSurface, fontFamily = FontFamily.Monospace, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Text(if (running) "RUNNING" else "READY", color = muted, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf(1, 5, 10, 25).forEach { minutes ->
                                AssistChip(
                                    onClick = { applyMinutes(minutes) },
                                    label = { Text("${minutes}m", fontFamily = FontFamily.Monospace) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = minutesText,
                            onValueChange = { value ->
                                minutesText = value.filter { it.isDigit() }.take(3)
                            },
                            label = { Text("MINUTOS", fontFamily = FontFamily.Monospace) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    if (remainingSeconds <= 0) {
                                        val minutes = minutesText.toIntOrNull() ?: 5
                                        applyMinutes(minutes)
                                    }
                                    running = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = scheme.onPrimary)
                            ) {
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
            }

            item {
                Text("+-[ UTILS_CATALOG ]--------------", color = accent, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }

            items(everydayUtilities) { item ->
                Surface(
                    color = panel,
                    border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.25f)),
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Row(modifier = Modifier.padding(12.dp)) {
                        Icon(Icons.Default.HourglassTop, contentDescription = null, tint = accent)
                        Column(modifier = Modifier.padding(start = 10.dp)) {
                            Text(item.name, color = scheme.onSurface, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            Text(item.detail, color = muted, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(totalSeconds: Int): String {
    val minutes = totalSeconds.coerceAtLeast(0) / 60
    val seconds = totalSeconds.coerceAtLeast(0) % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
