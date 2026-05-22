package com.hindrax.ss.features.nfc

import android.app.Activity
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hindrax.ss.domain.nfc.NfcTagFormatter
import com.hindrax.ss.domain.nfc.NfcTagSnapshot

private val NeonGreen = Color(0xFF64FF00)
private val Panel = Color(0xFF071007)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NfcLabScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    val adapter = remember { NfcAdapter.getDefaultAdapter(context) }
    var snapshot by remember { mutableStateOf<NfcTagSnapshot?>(null) }
    var status by remember {
        mutableStateOf(
            when {
                adapter == null -> "NFC_NOT_AVAILABLE"
                adapter.isEnabled -> "READY_SCAN_TAG"
                else -> "NFC_DISABLED"
            }
        )
    }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    DisposableEffect(adapter, activity) {
        if (adapter != null && activity != null && adapter.isEnabled) {
            val callback = NfcAdapter.ReaderCallback { tag ->
                activity.runOnUiThread {
                    snapshot = tag.toSnapshot()
                    status = "TAG_DETECTED"
                }
            }
            adapter.enableReaderMode(
                activity,
                callback,
                NfcAdapter.FLAG_READER_NFC_A or
                    NfcAdapter.FLAG_READER_NFC_B or
                    NfcAdapter.FLAG_READER_NFC_F or
                    NfcAdapter.FLAG_READER_NFC_V or
                    NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
                null
            )
        }
        onDispose {
            if (adapter != null && activity != null) {
                adapter.disableReaderMode(activity)
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("NFC_LAB_SAFE_INSPECTOR", fontFamily = FontFamily.Monospace) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NeonGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF050505),
                    titleContentColor = NeonGreen
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color(0xFF050505))
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = """
 _   _ _____ ____   _        _    ____
| \ | |  ___/ ___| | |      / \  | __ )
|  \| | |_ | |     | |     / _ \ |  _ \
| |\  |  _|| |___  | |___ / ___ \| |_) |
|_| \_|_|   \____| |_____/_/   \_\____/
 [ SAFE TAG INSPECTOR ]
                """.trimIndent(),
                color = NeonGreen,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                lineHeight = 10.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                softWrap = false
            )

            SafetyCard()

            Card(
                colors = CardDefaults.cardColors(containerColor = Panel),
                border = BorderStroke(1.dp, NeonGreen),
                shape = MaterialTheme.shapes.extraSmall,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Nfc, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.size(12.dp))
                    Column {
                        Text("STATUS: $status", color = NeonGreen, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Text("Acerca un tag NFC propio o de laboratorio.", color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    }
                }
            }

            SnapshotCard(snapshot)
        }
    }
}

@Composable
private fun SafetyCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF120808))
            .border(1.dp, Color.Red)
            .padding(12.dp)
    ) {
        Text(
            text = "SAFETY_GATE: este modulo no clona ni emula tarjetas de acceso. Usa inventario/diagnostico NFC y credenciales oficiales emitidas por sistemas que controles.",
            color = Color.Red,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            lineHeight = 15.sp
        )
    }
}

@Composable
private fun SnapshotCard(snapshot: NfcTagSnapshot?) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Panel),
        border = BorderStroke(1.dp, Color.DarkGray),
        shape = MaterialTheme.shapes.extraSmall,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("--- TAG_METADATA ---", color = Color.Cyan, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            if (snapshot == null) {
                Text("NO_TAG_CAPTURED", color = Color.Gray, fontFamily = FontFamily.Monospace)
            } else {
                Field("UID_MASKED", snapshot.maskedTagId)
                Field("UID_FULL_LAB", snapshot.tagId)
                Field("TECH", snapshot.technologies.joinToString(", "))
                Field("NDEF_TYPE", snapshot.ndefType ?: "N/A")
                Field("NDEF_MAX", snapshot.maxSizeBytes?.toString() ?: "N/A")
                Field("WRITABLE", snapshot.isWritable?.toString() ?: "N/A")
            }
        }
    }
}

@Composable
private fun Field(label: String, value: String) {
    Text(
        text = "$label: $value",
        color = if (label.startsWith("UID")) NeonGreen else Color.White,
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        lineHeight = 15.sp
    )
}

private fun Tag.toSnapshot(): NfcTagSnapshot {
    val tagId = NfcTagFormatter.formatId(id)
    val ndef = Ndef.get(this)
    return NfcTagSnapshot(
        tagId = tagId,
        maskedTagId = NfcTagFormatter.maskId(tagId),
        technologies = techList.map { it.substringAfterLast('.') },
        ndefType = ndef?.type,
        maxSizeBytes = ndef?.maxSize,
        isWritable = ndef?.isWritable
    )
}
