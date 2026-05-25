package com.hindrax.ss.domain.root

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RootAccessPolicyTest {
    @Test
    fun acceptsConfiguredRootKey() {
        assertTrue(RootAccessPolicy.isValid("19921351-2"))
    }

    @Test
    fun rejectsWrongRootKey() {
        assertFalse(RootAccessPolicy.isValid("19921351"))
        assertFalse(RootAccessPolicy.isValid(""))
    }
}
