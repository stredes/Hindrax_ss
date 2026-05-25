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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HindraxLogisticsWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        refresh(context, manager, ids)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == HindraxWidgetActions.ACTION_REFRESH_LOGISTICS) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, HindraxLogisticsWidgetProvider::class.java))
            refresh(context, manager, ids)
        }
    }

    private fun refresh(context: Context, manager: AppWidgetManager, ids: IntArray) {
        if (ids.isEmpty()) return
        CoroutineScope(Dispatchers.IO).launch {
            val snapshot = runCatching { context.logisticsSnapshot() }
                .getOrElse { LogisticsSnapshot(error = "DB_ERROR") }
            withContext(Dispatchers.Main) {
                ids.forEach { id ->
                    manager.updateAppWidget(id, buildViews(context, snapshot))
                }
            }
        }
    }

    private fun buildViews(context: Context, snapshot: LogisticsSnapshot): RemoteViews {
        return RemoteViews(context.packageName, R.layout.widget_hindrax_logistics).apply {
            setOnClickPendingIntent(R.id.widget_logistics_root, openAppIntent(context))
            setOnClickPendingIntent(R.id.widget_logistics_refresh, refreshIntent(context))
            setTextViewText(R.id.widget_logistics_items, "${snapshot.items} PRODUCTOS")
            setTextViewText(R.id.widget_logistics_low, "${snapshot.lowStock} CRITICOS")
            setTextViewText(R.id.widget_logistics_units, "STOCK ${snapshot.totalUnits}")
            setTextViewText(R.id.widget_logistics_top, snapshot.topLowStockLabel)
            setTextViewText(R.id.widget_logistics_sync, snapshot.updatedLabel)
            setTextViewText(R.id.widget_logistics_error, snapshot.error.orEmpty())
        }
    }

    private fun openAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context,
            4200,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun refreshIntent(context: Context): PendingIntent {
        val intent = Intent(context, HindraxLogisticsWidgetProvider::class.java)
            .setAction(HindraxWidgetActions.ACTION_REFRESH_LOGISTICS)
        return PendingIntent.getBroadcast(
            context,
            4201,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

private data class LogisticsSnapshot(
    val items: Int = 0,
    val lowStock: Int = 0,
    val totalUnits: String = "0",
    val topLowStockLabel: String = "SIN_ALERTAS",
    val updatedLabel: String = "SYNC --:--",
    val error: String? = null
)

private suspend fun Context.logisticsSnapshot(): LogisticsSnapshot {
    val inventory = HindraxDatabase.getDatabase(applicationContext).inventoryDao().getAllInventorySync()
    val lowStockItems = inventory
        .filter { it.currentQuantity <= it.minQuantity }
        .sortedWith(compareBy({ it.currentQuantity }, { it.name.lowercase(Locale.ROOT) }))
    val totalUnits = inventory.sumOf { it.currentQuantity }
    return LogisticsSnapshot(
        items = inventory.size,
        lowStock = lowStockItems.size,
        totalUnits = if (totalUnits % 1.0 == 0.0) totalUnits.toInt().toString() else "%.1f".format(Locale.US, totalUnits),
        topLowStockLabel = lowStockItems.take(2).joinToString(" | ") { it.name.uppercase(Locale.ROOT) }
            .ifBlank { "SIN_ALERTAS" },
        updatedLabel = "SYNC ${SimpleDateFormat("HH:mm", Locale.US).format(Date())}"
    )
}
