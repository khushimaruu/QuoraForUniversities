package com.MADproject.quoraforuniversities.utils

import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import java.util.Locale

/** Formats the database instant in the viewer's timezone. Never invents a creation time. */
object TimestampFormatter {
    fun format(
        createdAt: String?,
        now: Instant = Instant.now(),
        zone: ZoneId = ZoneId.systemDefault()
    ): String? {
        if (createdAt.isNullOrBlank()) return null
        val instant = try {
            OffsetDateTime.parse(createdAt.trim().replace(' ', 'T')).toInstant()
        } catch (_: DateTimeParseException) {
            return null
        }
        val elapsed = Duration.between(instant, now).seconds.coerceAtLeast(0)
        if (elapsed < 60) return "Just now"
        if (elapsed < 3_600) return "${elapsed / 60}m ago"
        if (elapsed < 86_400) return "${elapsed / 3_600}h ago"

        val date = instant.atZone(zone).toLocalDate()
        val today = now.atZone(zone).toLocalDate()
        val days = ChronoUnit.DAYS.between(date, today)
        if (days == 1L) return "Yesterday"
        if (days in 2L..6L) return "${days}d ago"
        val pattern = if (date.year == today.year) "d MMM" else "d MMM uuuu"
        return date.format(DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH))
    }
}
