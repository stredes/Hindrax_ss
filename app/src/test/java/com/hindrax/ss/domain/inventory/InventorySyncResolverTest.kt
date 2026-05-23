package com.hindrax.ss.domain.inventory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InventorySyncResolverTest {
    @Test
    fun newerRemoteQuantityIsAppliedAndReportsDiscount() {
        val existing = InventorySyncItem(
            name = "Tomate",
            currentQuantity = 10.0,
            updatedAt = 1000L
        )
        val incoming = InventorySyncItem(
            name = "Tomate",
            currentQuantity = 9.0,
            updatedAt = 2000L
        )

        val result = InventorySyncResolver.resolve(existing, incoming)

        assertTrue(result.shouldApply)
        assertEquals(-1.0, result.delta, 0.0)
        assertEquals("INVENTORY_DISCOUNTED: Tomate -1", result.changeMessage)
    }

    @Test
    fun olderRemoteQuantityIsIgnored() {
        val existing = InventorySyncItem(
            name = "Tomate",
            currentQuantity = 8.0,
            updatedAt = 2000L
        )
        val incoming = InventorySyncItem(
            name = "Tomate",
            currentQuantity = 9.0,
            updatedAt = 1000L
        )

        val result = InventorySyncResolver.resolve(existing, incoming)

        assertFalse(result.shouldApply)
        assertEquals(0.0, result.delta, 0.0)
    }
}
