package com.MADproject.quoraforuniversities.utils

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimestampFormatterTest {
    private val now = Instant.parse("2026-10-20T12:00:00Z")
    private val zone = ZoneId.of("Asia/Kolkata")

    @Test fun formatsRelativeTimeAndCalendarDates() {
        val cases = mapOf(
            "2026-10-20T11:59:30Z" to "Just now",
            "2026-10-20T11:59:00Z" to "1m ago",
            "2026-10-20T11:45:00Z" to "15m ago",
            "2026-10-20T10:00:00Z" to "2h ago",
            "2026-10-19T12:00:00Z" to "Yesterday",
            "2026-10-17T12:00:00Z" to "3d ago",
            "2026-10-13T12:00:00Z" to "13 Oct",
            "2026-09-19T12:00:00Z" to "19 Sep",
            "2025-09-19T12:00:00Z" to "19 Sep 2025"
        )
        cases.forEach { (timestamp, expected) ->
            assertEquals(timestamp, expected, TimestampFormatter.format(timestamp, now, zone))
        }
    }

    @Test fun handlesSupabaseOffsetsAndFractionalSeconds() {
        val timestamps = listOf(
            "2026-10-20T11:45:00.000000+00:00",
            "2026-10-20T17:15:00+05:30",
            "2026-10-20 11:45:00+00:00"
        )
        timestamps.forEach { assertEquals("15m ago", TimestampFormatter.format(it, now, zone)) }
    }

    @Test fun usesLocalCalendarForYearAndDayBoundaries() {
        val newYear = Instant.parse("2027-01-01T00:15:00Z")
        val timestamp = "2026-12-20T20:00:00Z"
        assertEquals("21 Dec 2026", TimestampFormatter.format(timestamp, newYear, zone))
        assertEquals("20 Dec", TimestampFormatter.format(timestamp, newYear, ZoneId.of("America/Los_Angeles")))

        val afterDst = Instant.parse("2026-11-02T05:30:00Z")
        assertEquals("Yesterday", TimestampFormatter.format("2026-11-01T04:45:00Z", afterDst, ZoneId.of("America/New_York")))
    }

    @Test fun doesNotInventMissingTimestampsOrShowNegativeAges() {
        listOf(null, "", "invalid", "2026-10-20T12:00:00").forEach {
            assertNull(TimestampFormatter.format(it, now, zone))
        }
        assertEquals("Just now", TimestampFormatter.format("2026-10-20T12:00:10Z", now, zone))
    }
}
