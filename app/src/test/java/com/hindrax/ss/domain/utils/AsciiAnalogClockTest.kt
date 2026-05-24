package com.hindrax.ss.domain.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AsciiAnalogClockTest {
    @Test
    fun rendersStableAnalogClockGrid() {
        val clock = AsciiAnalogClock.render(totalSeconds = 300, remainingSeconds = 150)
        val lines = clock.lines()

        assertEquals(13, lines.size)
        assertTrue(clock.contains('o'))
        assertTrue(clock.contains('*'))
        assertTrue(clock.contains('#'))
    }
}
