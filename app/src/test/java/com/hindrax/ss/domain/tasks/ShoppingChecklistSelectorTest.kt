package com.hindrax.ss.domain.tasks

import com.hindrax.ss.domain.tasks.model.ShoppingChecklistSelector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShoppingChecklistSelectorTest {
    @Test
    fun parsesPositiveQuantityWithCommaOrDot() {
        assertEquals(1.5, ShoppingChecklistSelector.parseQuantity("1,5")!!, 0.0)
        assertEquals(2.25, ShoppingChecklistSelector.parseQuantity("2.25")!!, 0.0)
    }

    @Test
    fun rejectsBlankZeroAndNegativeQuantity() {
        assertNull(ShoppingChecklistSelector.parseQuantity(""))
        assertNull(ShoppingChecklistSelector.parseQuantity("0"))
        assertNull(ShoppingChecklistSelector.parseQuantity("-1"))
    }
}
