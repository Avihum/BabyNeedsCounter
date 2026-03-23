package com.example.babyneedscounter

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Single definition of a "day" for this app: **7:00 → 7:00** in the device default timezone
 * (same rule as [BackendService.fetchTodayStats] historically used via [java.util.Calendar]).
 *
 * A baby day is **[babyDayStart, nextBabyDayStart)** in epoch millis.
 */
object BabyDay {

    fun babyDayStartContaining(nowMillis: Long, zone: ZoneId): Long {
        val zdt = Instant.ofEpochMilli(nowMillis).atZone(zone)
        val date = zdt.toLocalDate()
        var start = date.atTime(7, 0).atZone(zone).toInstant().toEpochMilli()
        if (nowMillis < start) {
            start = date.minusDays(1).atTime(7, 0).atZone(zone).toInstant().toEpochMilli()
        }
        return start
    }

    fun nextBabyDayStartMs(babyDayStartMillis: Long, zone: ZoneId): Long {
        val zdt = Instant.ofEpochMilli(babyDayStartMillis).atZone(zone)
        val d = zdt.toLocalDate()
        return d.plusDays(1).atTime(7, 0).atZone(zone).toInstant().toEpochMilli()
    }

    fun previousBabyDayStartMs(babyDayStartMillis: Long, zone: ZoneId): Long {
        val zdt = Instant.ofEpochMilli(babyDayStartMillis).atZone(zone)
        val d = zdt.toLocalDate()
        return d.minusDays(1).atTime(7, 0).atZone(zone).toInstant().toEpochMilli()
    }

    /**
     * Walk by whole local calendar days from [fromBabyDayStartMillis] (which must be exactly 7:00 local).
     */
    fun offsetBabyDayStartMs(fromBabyDayStartMillis: Long, zone: ZoneId, offsetDays: Int): Long {
        val zdt = Instant.ofEpochMilli(fromBabyDayStartMillis).atZone(zone)
        val d = zdt.toLocalDate()
        return d.plusDays(offsetDays.toLong()).atTime(7, 0).atZone(zone).toInstant().toEpochMilli()
    }

    /** Same format the sheet / Apps Script expect for `startTime` query params. */
    fun formatSheetStartTime(millis: Long, zone: ZoneId): String =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.US)
            .format(Instant.ofEpochMilli(millis).atZone(zone))
}
