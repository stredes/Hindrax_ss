package com.hindrax.ss.features.nfc

import android.app.Activity
import android.content.pm.PackageManager
import android.nfc.NdefMessage
import android.nfc.NdefRecord
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
import com.hindrax.ss.domain.nfc.NfcHceEmulator
import com.hindrax.ss.domain.nfc.NfcLabMethod
import com.hindrax.ss.domain.nfc.NfcLabMethodCatalog
import com.hindrax.ss.domain.nfc.NfcTagFormatter
import com.hindrax.ss.domain.nfc.NfcTagSnapshot
import java.nio.charset.Charset

private val NeonGreen = Color(0xFF64FF00)
private val Panel = Color(0xFF071007)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NfcLabScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    val adapter = remember { NfcAdapter.getDefaultAdapter(context) }
    val hceSupported = remember {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION)
    }
    var snapshot by remember { mutableStateOf<NfcTagSnapshot?>(null) }
    var selectedMethod by remember { mutableStateOf(NfcLabMethod.READ) }
    var copiedPayload by remember { mutableStateOf("") }
    var writePayload by remember { mutableStateOf(NfcLabHceProfileStore.payload(context)) }
    var emulationEnabled by remember { mutableStateOf(NfcLabHceProfileStore.isEnabled(context)) }
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

    DisposableEffect(adapter, activity, selectedMethod, writePayload) {
        if (adapter != null && activity != null && adapter.isEnabled && selectedMethod != NfcLabMethod.EMUL) {
            val callback = NfcAdapter.ReaderCallback { tag ->
                val capturedSnapshot = tag.toSnapshot()
                val resultStatus = if (selectedMethod == NfcLabMethod.WRITE) {
                    tag.writeLabPayload(writePayload)
                } else {
                    "TAG_DETECTED"
                }
                activity.runOnUiThread {
                    snapshot = capturedSnapshot
                    status = resultStatus
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

            MethodPanel(
                selectedMethod = selectedMethod,
                snapshot = snapshot,
                copiedPayload = copiedPayload,
                writePayload = writePayload,
                emulationEnabled = emulationEnabled,
                hceSupported = hceSupported,
                onMethodSelected = { method ->
                    selectedMethod = method
                    status = when (method) {
                        NfcLabMethod.READ -> "READY_SCAN_TAG"
                        NfcLabMethod.COPY -> {
                            val payload = snapshot?.ndefText.orEmpty()
                            if (payload.isBlank()) {
                                "COPY_NEEDS_NDEF_PAYLOAD"
                            } else {
                                copiedPayload = payload
                                writePayload = payload
                                NfcLabHceProfileStore.setPayload(context, payload)
                                "NDEF_PAYLOAD_COPIED_TO_LAB_BUFFER"
                            }
                        }
                        NfcLabMethod.WRITE -> "WRITE_ARMED_SCAN_WRITABLE_NDEF_TAG"
                        NfcLabMethod.EMUL -> {
                            if (!hceSupported) {
                                emulationEnabled = false
                                NfcLabHceProfileStore.setEnabled(context, false)
                                "HCE_NOT_SUPPORTED_ON_DEVICE"
                            } else {
                                val nextEnabled = !emulationEnabled
                                val normalizedPayload = NfcHceEmulator.normalizePayload(writePayload)
                                writePayload = normalizedPayload
                                emulationEnabled = nextEnabled
                                NfcLabHceProfileStore.setPayload(context, normalizedPayload)
                                NfcLabHceProfileStore.setEnabled(context, nextEnabled)
                                if (nextEnabled) "HCE_NDEF_TYPE4_PROFILE_ON" else "HCE_NDEF_TYPE4_PROFILE_OFF"
                            }
                        }
                    }
                },
                onWritePayloadChanged = {
                    writePayload = it
                    NfcLabHceProfileStore.setPayload(context, it)
                }
            )

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
private fun MethodPanel(
    selectedMethod: NfcLabMethod,
    snapshot: NfcTagSnapshot?,
    copiedPayload: String,
    writePayload: String,
    emulationEnabled: Boolean,
    hceSupported: Boolean,
    onMethodSelected: (NfcLabMethod) -> Unit,
    onWritePayloadChanged: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Panel),
        border = BorderStroke(1.dp, Color.DarkGray),
        shape = MaterialTheme.shapes.extraSmall,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("--- METHODS ---", color = Color.Cyan, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                NfcLabMethodCatalog.methods.forEach { method ->
                    Button(
                        onClick = { onMethodSelected(method) },
                        enabled = when (method) {
                            NfcLabMethod.COPY -> NfcLabMethodCatalog.canCopy(snapshot)
                            else -> true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedMethod == method) NeonGreen else Color(0xFF111111),
                            contentColor = if (selectedMethod == method) Color.Black else NeonGreen,
                            disabledContainerColor = Color(0xFF101010),
                            disabledContentColor = Color.DarkGray
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(method.label, fontFamily = FontFamily.Monospace, fontSize = 10.sp, maxLines = 1)
                    }
                }
            }
            OutlinedTextField(
                value = writePayload,
                onValueChange = onWritePayloadChanged,
                label = { Text("WRITE_PAYLOAD_NDEF_TEXT", fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, color = Color.White),
                modifier = Modifier.fillMaxWidth()
            )
            Field("COPY_BUFFER", copiedPayload.ifBlank { "EMPTY" })
            Field("WRITE_READY", NfcLabMethodCatalog.canWrite(snapshot, writePayload).toString())
            Field("HCE_SUPPORT", hceSupported.toString())
            Field("HCE_AID", "D2760000850101")
            Field("HCE_PAYLOAD", NfcHceEmulator.normalizePayload(writePayload))
            Field("EMUL_MODE", if (emulationEnabled) "HCE_TYPE4_NDEF_NO_UID_CLONE" else "OFF")
        }
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
                Field("NDEF_PAYLOAD", snapshot.ndefText ?: "N/A")
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
        isWritable = ndef?.isWritable,
        ndefText = ndef?.cachedNdefMessage?.toLabText()
    )
}

private fun Tag.writeLabPayload(payload: String): String {
    if (payload.isBlank()) return "WRITE_ABORTED_EMPTY_PAYLOAD"
    val ndef = Ndef.get(this) ?: return "WRITE_ABORTED_TAG_NOT_NDEF"
    val message = NdefMessage(arrayOf(NdefRecord.createTextRecord("en", payload)))
    return try {
        ndef.connect()
        when {
            !ndef.isWritable -> "WRITE_ABORTED_TAG_READ_ONLY"
            ndef.maxSize < message.toByteArray().size -> "WRITE_ABORTED_PAYLOAD_TOO_LARGE"
            else -> {
                ndef.writeNdefMessage(message)
                "WRITE_OK_NDEF_TEXT"
            }
        }
    } catch (e: Exception) {
        "WRITE_ERROR_${e.javaClass.simpleName}"
    } finally {
        runCatching { ndef.close() }
    }
}

private fun NdefMessage.toLabText(): String? {
    return records.firstNotNullOfOrNull { record -> record.toLabText() }
}

private fun NdefRecord.toLabText(): String? {
    return when {
        tnf == NdefRecord.TNF_WELL_KNOWN && type.contentEquals(NdefRecord.RTD_TEXT) -> decodeTextRecord(payload)
        tnf == NdefRecord.TNF_WELL_KNOWN && type.contentEquals(NdefRecord.RTD_URI) -> decodeUriRecord(payload)
        tnf == NdefRecord.TNF_MIME_MEDIA -> String(payload, Charsets.UTF_8)
        else -> null
    }?.takeIf { it.isNotBlank() }
}

private fun decodeTextRecord(payload: ByteArray): String? {
    if (payload.isEmpty()) return null
    val status = payload[0].toInt()
    val languageCodeLength = status and 0x3F
    val charset = if ((status and 0x80) == 0) Charsets.UTF_8 else Charset.forName("UTF-16")
    if (payload.size <= 1 + languageCodeLength) return null
    return String(payload, 1 + languageCodeLength, payload.size - 1 - languageCodeLength, charset)
}

private fun decodeUriRecord(payload: ByteArray): String? {
    if (payload.isEmpty()) return null
    val prefix = uriPrefixes.getOrElse(payload[0].toInt()) { "" }
    return prefix + String(payload, 1, payload.size - 1, Charsets.UTF_8)
}

private val uriPrefixes = listOf(
    "",
    "http://www.",
    "https://www.",
    "http://",
    "https://",
    "tel:",
    "mailto:",
    "ftp://anonymous:anonymous@",
    "ftp://ftp.",
    "ftps://",
    "sftp://",
    "smb://",
    "nfs://",
    "ftp://",
    "dav://",
    "news:",
    "telnet://",
    "imap:",
    "rtsp://",
    "urn:",
    "pop:",
    "sip:",
    "sips:",
    "tftp:",
    "btspp://",
    "btl2cap://",
    "btgoep://",
    "tcpobex://",
    "irdaobex://",
    "file://",
    "urn:epc:id:",
    "urn:epc:tag:",
    "urn:epc:pat:",
    "urn:epc:raw:",
    "urn:epc:",
    "urn:nfc:"
)
