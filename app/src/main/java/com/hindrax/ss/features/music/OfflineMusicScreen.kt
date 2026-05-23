package com.hindrax.ss.features.music

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.hindrax.ss.termux.TermuxBridge
import org.json.JSONArray
import org.json.JSONObject

data class OfflineTrack(
    val uri: String,
    val title: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineMusicScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var tracks by remember { mutableStateOf(loadTracks(context)) }
    var currentIndex by remember { mutableStateOf(-1) }
    var isPlaying by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("READY_FOR_LOCAL_AUDIO") }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var spotdlQuery by remember { mutableStateOf("") }
    val spotdlOutputDir = "/storage/emulated/0/Music/HindraxOfflineMusic"

    fun persist(newTracks: List<OfflineTrack>) {
        tracks = newTracks.distinctBy { it.uri }
        saveTracks(context, tracks)
        if (tracks.isEmpty()) {
            currentIndex = -1
            isPlaying = false
            status = "LIBRARY_EMPTY"
        }
    }

    fun stopPlayer() {
        player?.runCatching {
            stop()
            release()
        }
        player = null
        isPlaying = false
    }

    fun playAt(index: Int) {
        val track = tracks.getOrNull(index) ?: return
        stopPlayer()
        runCatching {
            MediaPlayer().apply {
                setDataSource(context, Uri.parse(track.uri))
                setOnCompletionListener {
                    val next = if (tracks.isNotEmpty()) (index + 1) % tracks.size else -1
                    if (next >= 0) playAt(next)
                }
                prepare()
                start()
            }
        }.onSuccess { mediaPlayer ->
            player = mediaPlayer
            currentIndex = index
            isPlaying = true
            status = "PLAYING: ${track.title}"
        }.onFailure { error ->
            status = "PLAYBACK_ERROR: ${error.message ?: "UNKNOWN"}"
        }
    }

    fun togglePlayback() {
        val activePlayer = player
        when {
            activePlayer == null && tracks.isNotEmpty() -> playAt(if (currentIndex >= 0) currentIndex else 0)
            activePlayer != null && isPlaying -> {
                activePlayer.pause()
                isPlaying = false
                status = "PAUSED"
            }
            activePlayer != null -> {
                activePlayer.start()
                isPlaying = true
                status = tracks.getOrNull(currentIndex)?.let { "PLAYING: ${it.title}" } ?: "PLAYING"
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { stopPlayer() }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        val imported = uris.mapNotNull { uri ->
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            OfflineTrack(uri = uri.toString(), title = displayNameFor(context, uri))
        }
        persist(tracks + imported)
        status = "IMPORTED: ${imported.size}"
    }

    fun importSpotdlLibrary() {
        val imported = loadSpotdlTracks(context)
        persist(tracks + imported)
        status = "SPOTDL_LIBRARY_IMPORTED: ${imported.size}"
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            importSpotdlLibrary()
        } else {
            status = "AUDIO_PERMISSION_DENIED"
        }
    }

    fun refreshSpotdlLibrary() {
        val permission = audioReadPermission()
        if (permission == null || ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            importSpotdlLibrary()
        } else {
            audioPermissionLauncher.launch(permission)
        }
    }

    fun startSpotdlDownload() {
        val query = spotdlQuery.trim()
        when {
            query.isBlank() -> status = "SPOTDL_QUERY_EMPTY"
            !TermuxBridge.isTermuxInstalled(context) -> status = "TERMUX_NOT_INSTALLED"
            else -> {
                TermuxBridge.executeScript(
                    context = context,
                    scriptName = "spotdl_download.sh",
                    arguments = arrayOf(query, spotdlOutputDir)
                )
                status = "SPOTDL_DOWNLOAD_STARTED"
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("OFFLINE_MUSIC", fontFamily = FontFamily.Monospace) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Green)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF050505),
                    titleContentColor = Color.Green
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color(0xFF050505))
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF101010)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.DarkGray),
                    shape = MaterialTheme.shapes.extraSmall,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LibraryMusic, contentDescription = null, tint = Color.Cyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "LOCAL_AUDIO_LIBRARY",
                                color = Color.Cyan,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Importa archivos de audio locales que tengas derecho a usar. La app guarda permiso de lectura para escucharlos offline.",
                            color = Color.Gray,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { importLauncher.launch(arrayOf("audio/*")) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Green, contentColor = Color.Black),
                            shape = MaterialTheme.shapes.extraSmall,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("IMPORT_LOCAL_AUDIO", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF101010)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.DarkGray),
                    shape = MaterialTheme.shapes.extraSmall,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color.Green)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "SPOTDL_BRIDGE",
                                color = Color.Green,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Pega una URL de Spotify, YouTube o busqueda compatible con spotDL. Descarga solo contenido que tengas derecho a guardar offline.",
                            color = Color.Gray,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = spotdlQuery,
                            onValueChange = { spotdlQuery = it },
                            label = { Text("SPOTDL_URL_OR_QUERY", fontFamily = FontFamily.Monospace) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color.Green,
                                unfocusedBorderColor = Color.DarkGray,
                                focusedLabelColor = Color.Green,
                                unfocusedLabelColor = Color.Gray,
                                cursorColor = Color.Green
                            )
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { startSpotdlDownload() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Green, contentColor = Color.Black),
                                shape = MaterialTheme.shapes.extraSmall,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("DOWNLOAD", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { refreshSpotdlLibrary() },
                                shape = MaterialTheme.shapes.extraSmall,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Cyan)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("SCAN", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "OUTPUT: $spotdlOutputDir",
                            color = Color.DarkGray,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        enabled = tracks.isNotEmpty(),
                        onClick = {
                            val next = if (currentIndex <= 0) tracks.lastIndex else currentIndex - 1
                            playAt(next)
                        }
                    ) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = if (tracks.isNotEmpty()) Color.Green else Color.DarkGray)
                    }
                    FilledIconButton(
                        enabled = tracks.isNotEmpty(),
                        onClick = { togglePlayback() },
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.Green, contentColor = Color.Black)
                    ) {
                        Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = "Play")
                    }
                    IconButton(
                        enabled = tracks.isNotEmpty(),
                        onClick = {
                            val next = if (currentIndex >= tracks.lastIndex) 0 else currentIndex + 1
                            playAt(next)
                        }
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = if (tracks.isNotEmpty()) Color.Green else Color.DarkGray)
                    }
                }
            }

            item {
                Text("STATUS: $status", color = Color.Green, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
            }
            item {
                Text("--- TRACKS [${tracks.size}] ---", color = Color.Cyan, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }

            itemsIndexed(tracks) { index, track ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.DarkGray)
                        .background(Color(0xFF0A0A0A))
                        .clickable { playAt(index) }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (index == currentIndex) ">" else "${index + 1}.",
                        color = if (index == currentIndex) Color.Yellow else Color.Green,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.width(28.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                        Text(
                            text = Uri.parse(track.uri).scheme.orEmpty().uppercase(),
                            color = Color.Gray,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp
                        )
                    }
                    IconButton(
                        onClick = {
                            if (index == currentIndex) stopPlayer()
                            persist(tracks.filterIndexed { i, _ -> i != index })
                        }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.Red)
                    }
                }
            }
        }
    }
}

private fun loadTracks(context: Context): List<OfflineTrack> {
    val raw = context.getSharedPreferences("hindrax_music", Context.MODE_PRIVATE)
        .getString("tracks", "[]")
        .orEmpty()
    return runCatching {
        val array = JSONArray(raw)
        List(array.length()) { index ->
            val item = array.getJSONObject(index)
            OfflineTrack(
                uri = item.optString("uri"),
                title = item.optString("title", "TRACK_${index + 1}")
            )
        }.filter { it.uri.isNotBlank() }
    }.getOrDefault(emptyList())
}

private fun saveTracks(context: Context, tracks: List<OfflineTrack>) {
    val array = JSONArray()
    tracks.forEach { track ->
        array.put(JSONObject().put("uri", track.uri).put("title", track.title))
    }
    context.getSharedPreferences("hindrax_music", Context.MODE_PRIVATE)
        .edit()
        .putString("tracks", array.toString())
        .apply()
}

private fun audioReadPermission(): String? {
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> Manifest.permission.READ_MEDIA_AUDIO
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> Manifest.permission.READ_EXTERNAL_STORAGE
        else -> null
    }
}

private fun loadSpotdlTracks(context: Context): List<OfflineTrack> {
    val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    } else {
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    }
    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.DISPLAY_NAME
    )
    val selection: String
    val selectionArgs: Array<String>
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        selection = "${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ?"
        selectionArgs = arrayOf("%Music/HindraxOfflineMusic%")
    } else {
        @Suppress("DEPRECATION")
        selection = "${MediaStore.Audio.Media.DATA} LIKE ?"
        selectionArgs = arrayOf("%/Music/HindraxOfflineMusic/%")
    }

    return runCatching {
        context.contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Audio.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            buildList {
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val uri = ContentUris.withAppendedId(collection, id)
                    add(
                        OfflineTrack(
                            uri = uri.toString(),
                            title = cursor.getString(nameColumn) ?: "SPOTDL_TRACK_$id"
                        )
                    )
                }
            }
        }.orEmpty()
    }.getOrDefault(emptyList())
}

private fun displayNameFor(context: Context, uri: Uri): String {
    return runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                } else {
                    null
                }
            }
    }.getOrNull() ?: uri.lastPathSegment ?: "AUDIO_TRACK"
}
