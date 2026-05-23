package com.hindrax.ss.domain.inventory

import java.util.Locale
import kotlin.math.abs

data class InventorySyncItem(
    val name: String,
    val currentQuantity: Double,
    val updatedAt: Long
)

data class InventorySyncResult(
    val shouldApply: Boolean,
    val delta: Double,
    val changeMessage: String
)

object InventorySyncResolver {
    fun resolve(existing: InventorySyncItem?, incoming: InventorySyncItem): InventorySyncResult {
        if (existing != null && incoming.updatedAt <= existing.updatedAt) {
            return InventorySyncResult(
                shouldApply = false,
                delta = 0.0,
                changeMessage = "INVENTORY_IGNORED_OLDER: ${incoming.name}"
            )
        }

        val delta = incoming.currentQuantity - (existing?.currentQuantity ?: 0.0)
        val action = when {
            delta < 0 -> "INVENTORY_DISCOUNTED"
            delta > 0 -> "INVENTORY_ADDED"
            else -> "INVENTORY_SYNCED"
        }
        val quantity = formatQuantity(delta)
        val message = if (delta == 0.0) {
            "$action: ${incoming.name}"
        } else {
            "$action: ${incoming.name} ${if (delta > 0) "+" else "-"}$quantity"
        }

        return InventorySyncResult(
            shouldApply = true,
            delta = delta,
            changeMessage = message
        )
    }

    private fun formatQuantity(value: Double): String {
        val normalized = abs(value)
        return if (normalized % 1.0 == 0.0) {
            normalized.toLong().toString()
        } else {
            String.format(Locale.US, "%.2f", normalized).trimEnd('0').trimEnd('.')
        }
    }
}
