package com.hindrax.ss.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.hindrax.ss.MainActivity
import com.hindrax.ss.R
import com.hindrax.ss.data.db.HindraxDatabase
import com.hindrax.ss.data.remote.ApiHindraxConfigStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HindraxStatusWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        refresh(context, manager, ids)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == HindraxWidgetActions.ACTION_REFRESH_STATUS) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, HindraxStatusWidgetProvider::class.java))
            refresh(context, manager, ids)
        }
    }

    private fun refresh(context: Context, manager: AppWidgetManager, ids: IntArray) {
        if (ids.isEmpty()) return
        CoroutineScope(Dispatchers.IO).launch {
            val snapshot = runCatching { context.statusSnapshot() }
                .getOrElse { StatusSnapshot(error = "DB_ERROR") }
            withContext(Dispatchers.Main) {
                ids.forEach { id ->
                    manager.updateAppWidget(id, buildViews(context, snapshot))
                }
            }
        }
    }

    private fun buildViews(context: Context, snapshot: StatusSnapshot): RemoteViews {
        return RemoteViews(context.packageName, R.layout.widget_hindrax_status).apply {
            setOnClickPendingIntent(R.id.widget_status_root, openAppIntent(context))
            setOnClickPendingIntent(R.id.widget_status_refresh, refreshIntent(context))
            setTextViewText(R.id.widget_status_api, snapshot.apiLabel)
            setTextViewText(R.id.widget_status_tasks, "TAREAS ${snapshot.openTasks}/${snapshot.totalTasks}")
            setTextViewText(R.id.widget_status_inventory, "ITEMS ${snapshot.inventoryItems}")
            setTextViewText(R.id.widget_status_sync, snapshot.updatedLabel)
            setTextViewText(R.id.widget_status_error, snapshot.error.orEmpty())
        }
    }

    private fun openAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(HindraxWidgetActions.EXTRA_START_ROUTE, "dashboard")
        return PendingIntent.getActivity(
            context,
            4100,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun refreshIntent(context: Context): PendingIntent {
        val intent = Intent(context, HindraxStatusWidgetProvider::class.java)
            .setAction(HindraxWidgetActions.ACTION_REFRESH_STATUS)
        return PendingIntent.getBroadcast(
            context,
            4101,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

private data class StatusSnapshot(
    val totalTasks: Int = 0,
    val openTasks: Int = 0,
    val inventoryItems: Int = 0,
    val apiLabel: String = "API: OFFLINE",
    val updatedLabel: String = "SYNC --:--",
    val error: String? = null
)

private suspend fun Context.statusSnapshot(): StatusSnapshot {
    val database = HindraxDatabase.getDatabase(applicationContext)
    val tasks = database.taskDao().getAllTasksSync()
    val inventory = database.inventoryDao().getAllInventorySync()
    val apiConfig = ApiHindraxConfigStore(applicationContext).load()
    return StatusSnapshot(
        totalTasks = tasks.size,
        openTasks = tasks.count { !it.isDeleted && it.status.name != "COMPLETADA" && it.status.name != "CANCELADA" },
        inventoryItems = inventory.size,
        apiLabel = if (apiConfig.isReady) "API: ONLINE" else "API: DISABLED",
        updatedLabel = "SYNC ${SimpleDateFormat("HH:mm", Locale.US).format(Date())}"
    )
}
