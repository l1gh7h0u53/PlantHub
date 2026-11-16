package com.example.util

/**
 * Multiplatform calendar bounds describing the end of today and the starting Sunday
 * of a GitHub-style contribution heatmap matrix.
 */
data class HeatmapCalendarBounds(
    val todayEndMillis: Long,
    val gridStartMillis: Long
)

/** Returns current system epoch timestamp in milliseconds across all KMP targets. */
expect fun currentTimeMillis(): Long

/** Formats an epoch millisecond timestamp in 12-hour "h:mm a" clock notation (e.g. "8:00 PM"). */
expect fun formatTime12h(epochMillis: Long): String

/** Formats an epoch millisecond timestamp into a sortable "yyyy-MM-dd" bucket key. */
expect fun formatDateKey(epochMillis: Long): String

/** Formats an epoch millisecond timestamp for user-facing tooltips (e.g. "Sun, Oct 25, 2026"). */
expect fun formatDisplayDate(epochMillis: Long): String

/** Computes local-timezone start and today-cutoff epoch timestamps for a [weeksCount]-week grid. */
expect fun calculateHeatmapGridBounds(weeksCount: Int): HeatmapCalendarBounds

/** Advances an epoch timestamp by [days] whole calendar days in local timezone (DST-safe). */
expect fun addDaysMillis(epochMillis: Long, days: Int): Long

/** Formats two-digit zero-padded numbers without relying on JVM String.format. */
fun formatTwoDigits(value: Long): String = if (value in 0..9) "0$value" else value.toString()

/** Formats hours, minutes, and seconds into "00h 00m 00s" multiplatform countdown text. */
fun formatHms(hours: Long, minutes: Long, seconds: Long): String {
    return "${formatTwoDigits(hours)}h ${formatTwoDigits(minutes)}m ${formatTwoDigits(seconds)}s"
}
