package com.hindrax.ss.domain.inventory

import org.junit.Assert.assertEquals
import org.junit.Test

class ProductNameNormalizerTest {
    @Test
    fun normalizesProductNamesForMatchingAndDisplay() {
        assertEquals("cerveza austral lager", ProductNameNormalizer.key("  CERVEZA   Austral lager "))
        assertEquals("Cerveza Austral Lager", ProductNameNormalizer.displayName("  CERVEZA   Austral lager "))
    }
}
