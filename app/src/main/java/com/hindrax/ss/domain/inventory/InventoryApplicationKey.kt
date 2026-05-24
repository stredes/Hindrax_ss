package com.hindrax.ss.domain.inventory

import com.hindrax.ss.domain.tasks.model.ChecklistItem
import com.hindrax.ss.domain.tasks.model.TaskType
import java.util.Locale

object InventoryApplicationKey {
    fun checklistLine(taskTitle: String, taskType: TaskType, item: ChecklistItem): String {
        val name = ProductNameNormalizer.key(item.text)
        val quantity = item.quantity?.let(::formatQuantity) ?: "0"
        val unit = item.unit?.trim()?.lowercase(Locale.ROOT).orEmpty()
        return "checklist:${taskType.name}:${ProductNameNormalizer.key(taskTitle)}:$name:$quantity:$unit"
    }

    private fun formatQuantity(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toLong().toString()
        } else {
            String.format(Locale.US, "%.4f", value).trimEnd('0').trimEnd('.')
        }
    }
}
