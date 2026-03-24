package com.example.babyneedscounter

import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

/**
 * Server timestamps may come back either as UTC ISO instants or as sheet-local wall-clock strings.
 */
object ServerDateTimes {
    private val localMinuteFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.US)
    private val localSecondFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.US)

    fun parseMillis(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        val text = raw.trim()
        val zone = ZoneId.systemDefault()

        return runCatching { Instant.parse(text).toEpochMilli() }.getOrNull()
            ?: runCatching { OffsetDateTime.parse(text).toInstant().toEpochMilli() }.getOrNull()
            ?: runCatching {
                LocalDateTime.parse(text, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    .atZone(zone)
                    .toInstant()
                    .toEpochMilli()
            }.getOrNull()
            ?: runCatching {
                LocalDateTime.parse(text, localMinuteFormatter)
                    .atZone(zone)
                    .toInstant()
                    .toEpochMilli()
            }.getOrNull()
            ?: runCatching {
                LocalDateTime.parse(text, localSecondFormatter)
                    .atZone(zone)
                    .toInstant()
                    .toEpochMilli()
            }.getOrNull()
    }

    fun parseDate(raw: String?): Date? = parseMillis(raw)?.let(::Date)

    fun formatClock(raw: String?): String {
        val date = parseDate(raw) ?: return "—"
        return SimpleDateFormat("HH:mm", Locale.US).format(date)
    }

    fun formatDayLabel(raw: String?): String {
        val date = parseDate(raw) ?: return ""
        return SimpleDateFormat("MMM d", Locale.US).format(date)
    }
}
