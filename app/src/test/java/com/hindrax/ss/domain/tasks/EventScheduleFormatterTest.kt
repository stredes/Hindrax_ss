package com.hindrax.ss.domain.tasks

import com.hindrax.ss.domain.tasks.model.EventScheduleFormatter
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.TimeZone

class EventScheduleFormatterTest {
    @Test
    fun formatsEventDateForLabel() {
        val timestamp = 1_767_268_800_000L // 2026-01-01 12:00 UTC

        val label = EventScheduleFormatter.format(timestamp, TimeZone.getTimeZone("UTC"))

        assertEquals("2026-01-01 12:00", label)
    }

    @Test
    fun nullDateShowsPendingLabel() {
        assertEquals("EVENT_DATE_PENDING", EventScheduleFormatter.format(null, TimeZone.getTimeZone("UTC")))
    }
}
