package com.hindrax.ss.domain.inventory

import com.hindrax.ss.domain.tasks.model.ChecklistItem
import com.hindrax.ss.domain.tasks.model.TaskType
import org.junit.Assert.assertEquals
import org.junit.Test

class InventoryApplicationKeyTest {
    @Test
    fun checklistLineKeyIgnoresVolatileChecklistIds() {
        val first = ChecklistItem(
            id = "local-id",
            text = "  cerveza  ",
            isChecked = true,
            quantity = 1.0,
            unit = "Unid"
        )
        val second = first.copy(id = "remote-id", text = "CERVEZA")

        assertEquals(
            InventoryApplicationKey.checklistLine("Compra Viernes", TaskType.SHOPPING, first),
            InventoryApplicationKey.checklistLine(" compra viernes ", TaskType.SHOPPING, second)
        )
    }
}
