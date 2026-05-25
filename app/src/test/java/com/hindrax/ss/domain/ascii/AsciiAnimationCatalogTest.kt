package com.hindrax.ss.domain.ascii

import org.junit.Assert.assertTrue
import org.junit.Test

class AsciiAnimationCatalogTest {
    @Test
    fun everyContextHasReadableFrames() {
        AsciiAnimationContext.entries.forEach { context ->
            val spec = AsciiAnimationCatalog.forContext(context)

            assertTrue("${context.name} should have frames", spec.frames.isNotEmpty())
            assertTrue("${context.name} should not run too fast", spec.frameMillis >= 80L)
            assertTrue("${context.name} frames should contain visible text", spec.frames.all { it.isNotBlank() })
        }
    }
}
