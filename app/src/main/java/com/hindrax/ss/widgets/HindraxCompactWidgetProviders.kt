package com.hindrax.ss.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.RemoteViews
import com.hindrax.ss.BuildConfig
import com.hindrax.ss.MainActivity
import com.hindrax.ss.R
import com.hindrax.ss.data.db.HindraxDatabase
import com.hindrax.ss.data.entity.ChatMessageEntity
import com.hindrax.ss.data.entity.PeerEntity
import com.hindrax.ss.data.entity.TaskEntity
import com.hindrax.ss.data.remote.ApiHindraxConfigStore
import com.hindrax.ss.domain.tasks.model.TaskStatus
import com.hindrax.ss.domain.tasks.model.TaskType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

abstract class HindraxCompactWidgetProvider : AppWidgetProvider() {
    protected abstract val providerClass: Class<out HindraxCompactWidgetProvider>
    protected abstract val refreshAction: String
    protected abstract val openRoute: String
    protected abstract val requestCodeBase: Int

    protected abstract suspend fun snapshot(context: Context): CompactWidgetSnapshot

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        refresh(context, manager, ids)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == refreshAction) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, providerClass))
            refresh(context, manager, ids)
        }
    }

    private fun refresh(context: Context, manager: AppWidgetManager, ids: IntArray) {
        if (ids.isEmpty()) return
        CoroutineScope(Dispatchers.IO).launch {
            val data = runCatching { snapshot(context.applicationContext) }
                .getOrElse { CompactWidgetSnapshot(title = "HINDRAX", line1 = "ERROR", line2 = "DB_ERROR") }
            withContext(Dispatchers.Main) {
                ids.forEach { id -> manager.updateAppWidget(id, buildViews(context, data)) }
            }
        }
    }

    private fun buildViews(context: Context, snapshot: CompactWidgetSnapshot): RemoteViews {
        return RemoteViews(context.packageName, R.layout.widget_hindrax_compact).apply {
            setOnClickPendingIntent(R.id.widget_compact_root, openAppIntent(context))
            setOnClickPendingIntent(R.id.widget_compact_refresh, refreshIntent(context))
            setTextViewText(R.id.widget_compact_title, snapshot.title)
            setTextViewText(R.id.widget_compact_line1, snapshot.line1)
            setTextViewText(R.id.widget_compact_line2, snapshot.line2)
            setTextViewText(R.id.widget_compact_line3, snapshot.line3)
            setTextViewText(R.id.widget_compact_line4, snapshot.line4)
            setTextViewText(R.id.widget_compact_sync, "SYNC ${WidgetFormat.time()}")
        }
    }

    private fun openAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(HindraxWidgetActions.EXTRA_START_ROUTE, openRoute)
        return PendingIntent.getActivity(
            context,
            requestCodeBase,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun refreshIntent(context: Context): PendingIntent {
        val intent = Intent(context, providerClass).setAction(refreshAction)
        return PendingIntent.getBroadcast(
            context,
            requestCodeBase + 1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

data class CompactWidgetSnapshot(
    val title: String,
    val line1: String,
    val line2: String,
    val line3: String = "",
    val line4: String = ""
)

class HindraxTasksWidgetProvider : HindraxCompactWidgetProvider() {
    override val providerClass = HindraxTasksWidgetProvider::class.java
    override val refreshAction = HindraxWidgetActions.ACTION_REFRESH_TASKS
    override val openRoute = "tasks_list"
    override val requestCodeBase = 4300

    override suspend fun snapshot(context: Context): CompactWidgetSnapshot {
        val tasks = HindraxDatabase.getDatabase(context).taskDao().getAllTasksSync()
        val active = tasks.filter { it.status != TaskStatus.COMPLETADA && it.status != TaskStatus.CANCELADA }
        val events = tasks.count { it.type == TaskType.EVENT && it.scheduledTime != null }
        val next = active
            .sortedWith(
                compareBy<TaskEntity> { it.scheduledTime ?: Long.MAX_VALUE }
                    .thenByDescending { it.updatedAt }
            )
            .firstOrNull()
        return CompactWidgetSnapshot(
            title = "TASKS_CORE",
            line1 = "ABIERTAS ${active.size}/${tasks.size}",
            line2 = "EVENTOS $events",
            line3 = next?.title?.let { "NEXT ${WidgetFormat.clean(it, 22)}" } ?: "NEXT SIN_TAREAS",
            line4 = next?.scheduledTime?.let { "DATE ${WidgetFormat.dateTime(it)}" }.orEmpty()
        )
    }
}

class HindraxChatWidgetProvider : HindraxCompactWidgetProvider() {
    override val providerClass = HindraxChatWidgetProvider::class.java
    override val refreshAction = HindraxWidgetActions.ACTION_REFRESH_CHAT
    override val openRoute = "chat"
    override val requestCodeBase = 4400

    override suspend fun snapshot(context: Context): CompactWidgetSnapshot {
        val dao = HindraxDatabase.getDatabase(context).chatDao()
        val peers = dao.getAllPeersSync()
        val messages = dao.getAllMessagesSync()
        val latest = messages.maxByOrNull { it.timestamp }
        val peerById = peers.associateBy { it.id }
        return CompactWidgetSnapshot(
            title = "CHAT_LINK",
            line1 = "PEERS ${peers.count { it.isOnline }}/${peers.size} ONLINE",
            line2 = "MSGS ${messages.size}",
            line3 = latest?.let { "LAST ${WidgetFormat.peerName(peerById[it.peerId], it)}" } ?: "LAST SIN_MENSAJES",
            line4 = latest?.let { WidgetFormat.clean(it.message, 28) }.orEmpty()
        )
    }
}

class HindraxLocationWidgetProvider : HindraxCompactWidgetProvider() {
    override val providerClass = HindraxLocationWidgetProvider::class.java
    override val refreshAction = HindraxWidgetActions.ACTION_REFRESH_LOCATION
    override val openRoute = "live_location"
    override val requestCodeBase = 4500

    override suspend fun snapshot(context: Context): CompactWidgetSnapshot {
        val peers = HindraxDatabase.getDatabase(context).chatDao().getAllPeersSync()
        val located = peers.filter { it.hasLocation }
        val latest = located.maxByOrNull { it.locationUpdatedAt ?: 0L }
        val freshCount = located.count { max(0L, System.currentTimeMillis() - (it.locationUpdatedAt ?: 0L)) <= 30 * 60 * 1000L }
        return CompactWidgetSnapshot(
            title = "GEO_LIVE",
            line1 = "GPS_PEERS ${located.size}/${peers.size}",
            line2 = "FRESH_30M $freshCount",
            line3 = latest?.let { "LAST ${WidgetFormat.clean(it.displayName, 22)}" } ?: "LAST NO_GPS_FIX",
            line4 = latest?.locationUpdatedAt?.let { "TIME ${WidgetFormat.dateTime(it)}" }.orEmpty()
        )
    }
}

class HindraxTerminalWidgetProvider : HindraxCompactWidgetProvider() {
    override val providerClass = HindraxTerminalWidgetProvider::class.java
    override val refreshAction = HindraxWidgetActions.ACTION_REFRESH_TERMINAL
    override val openRoute = "cyd_terminal"
    override val requestCodeBase = 4600

    override suspend fun snapshot(context: Context): CompactWidgetSnapshot {
        val termuxInstalled = context.packageManager.isPackageInstalled("com.termux")
        return CompactWidgetSnapshot(
            title = "TERMINAL_HUB",
            line1 = "TERMUX ${if (termuxInstalled) "READY" else "OFFLINE"}",
            line2 = "CYD_TERMINAL TAP_OPEN",
            line3 = "TOOLS FLUJOS_CONECTADOS",
            line4 = "MODO ESPANOL"
        )
    }
}

class HindraxUtilsWidgetProvider : HindraxCompactWidgetProvider() {
    override val providerClass = HindraxUtilsWidgetProvider::class.java
    override val refreshAction = HindraxWidgetActions.ACTION_REFRESH_UTILS
    override val openRoute = "utils"
    override val requestCodeBase = 4700

    override suspend fun snapshot(context: Context): CompactWidgetSnapshot {
        return CompactWidgetSnapshot(
            title = "UTILS_PANEL",
            line1 = "UTILIDADES 15",
            line2 = "TIEMPO CALCULO TEXTO",
            line3 = "MEDICION AUDIO LISTAS",
            line4 = "TAP PARA_ABRIR"
        )
    }
}

class HindraxMusicWidgetProvider : HindraxCompactWidgetProvider() {
    override val providerClass = HindraxMusicWidgetProvider::class.java
    override val refreshAction = HindraxWidgetActions.ACTION_REFRESH_MUSIC
    override val openRoute = "offline_music"
    override val requestCodeBase = 4800

    override suspend fun snapshot(context: Context): CompactWidgetSnapshot {
        val spotifyInstalled = context.packageManager.isPackageInstalled("com.spotify.music")
        return CompactWidgetSnapshot(
            title = "MUSIC_NODE",
            line1 = "LOCAL_PLAYER READY",
            line2 = "SPOTIFY ${if (spotifyInstalled) "DETECTED" else "NO_APP"}",
            line3 = "AUDIO_LIBRARY TAP_OPEN",
            line4 = "CONTROL DESDE_APP"
        )
    }
}

class HindraxUpdatesWidgetProvider : HindraxCompactWidgetProvider() {
    override val providerClass = HindraxUpdatesWidgetProvider::class.java
    override val refreshAction = HindraxWidgetActions.ACTION_REFRESH_UPDATES
    override val openRoute = "settings"
    override val requestCodeBase = 4900

    override suspend fun snapshot(context: Context): CompactWidgetSnapshot {
        val config = ApiHindraxConfigStore(context).load()
        return CompactWidgetSnapshot(
            title = "UPDATE_CHANNEL",
            line1 = "VERSION ${BuildConfig.VERSION_NAME}",
            line2 = "API ${if (config.isReady) "ONLINE" else "DISABLED"}",
            line3 = WidgetFormat.clean(config.baseUrl.ifBlank { "NO_ENDPOINT" }, 28),
            line4 = "TAP SETTINGS"
        )
    }
}

private object WidgetFormat {
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.US)
    private val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.US)

    fun time(): String = timeFormat.format(Date())

    fun dateTime(timestamp: Long): String = dateFormat.format(Date(timestamp))

    fun clean(value: String, maxLength: Int): String {
        val normalized = value
            .replace(Regex("\\s+"), " ")
            .trim()
            .uppercase(Locale.ROOT)
        return if (normalized.length <= maxLength) normalized else normalized.take(maxLength - 1) + "."
    }

    fun peerName(peer: PeerEntity?, message: ChatMessageEntity): String {
        if (message.isFromMe) return "YO"
        return clean(peer?.displayName ?: message.peerId, 18)
    }
}

private fun PackageManager.isPackageInstalled(packageName: String): Boolean {
    return runCatching {
        getPackageInfo(packageName, 0)
        true
    }.getOrDefault(false)
}
