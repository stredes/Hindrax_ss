package com.hindrax.ss.domain.tasks.model

import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class TaskStatus {
    PENDIENTE,
    EN_PROGRESO,
    PAUSADA,
    COMPLETADA,
    CANCELADA
}

enum class TaskType {
    GENERAL,
    SHOPPING,
    EVENT,
    INVENTORY,
    FERIA
}

@Serializable
data class ChecklistItem(
    val id: String,
    val text: String,
    val isChecked: Boolean = false,
    val quantity: Double? = null,
    val unit: String? = null
)

data class Task(
    val id: Long = 0,
    val title: String,
    val description: String,
    val status: TaskStatus,
    val type: TaskType = TaskType.GENERAL,
    val scheduledTime: Long? = null,
    val locationName: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val quantity: Double? = null,
    val unit: String? = null,
    val inventoryItemId: Long? = null,
    val assignedPeerId: String? = null,
    val checklist: List<ChecklistItem> = emptyList(),
    val createdAt: Long,
    val updatedAt: Long
)

data class TaskHistory(
    val id: Long = 0,
    val taskId: Long,
    val action: String,
    val detail: String,
    val createdAt: Long
)

data class InventoryItem(
    val id: Long = 0,
    val name: String,
    val category: String,
    val currentQuantity: Double,
    val minQuantity: Double,
    val unit: String,
    val updatedAt: Long
)

object ShoppingChecklistSelector {
    fun parseQuantity(value: String): Double? {
        return value.trim()
            .replace(",", ".")
            .toDoubleOrNull()
            ?.takeIf { it > 0.0 }
    }
}

object EventScheduleFormatter {
    fun format(timestamp: Long?, timeZone: TimeZone = TimeZone.getDefault()): String {
        if (timestamp == null) return "EVENT_DATE_PENDING"
        return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply {
            this.timeZone = timeZone
        }.format(Date(timestamp))
    }
}
