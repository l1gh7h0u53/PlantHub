package com.example.util

import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMinute
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitSecond
import platform.Foundation.NSCalendarUnitWeekday
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSDate
import platform.Foundation.NSDateComponents
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.timeIntervalSince1970

actual fun currentTimeMillis(): Long {
    return (NSDate().timeIntervalSince1970 * 1000.0).toLong()
}

private val iosTime12hFormatter = NSDateFormatter().apply {
    locale = NSLocale("en_US_POSIX")
    dateFormat = "h:mm a"
}

private val iosDateKeyFormatter = NSDateFormatter().apply {
    locale = NSLocale("en_US_POSIX")
    dateFormat = "yyyy-MM-dd"
}

private val iosDisplayDateFormatter = NSDateFormatter().apply {
    locale = NSLocale("en_US_POSIX")
    dateFormat = "EEE, MMM d, yyyy"
}

actual fun formatTime12h(epochMillis: Long): String {
    return iosTime12hFormatter.stringFromDate(NSDate.dateWithTimeIntervalSince1970(epochMillis / 1000.0))
}

actual fun formatDateKey(epochMillis: Long): String {
    return iosDateKeyFormatter.stringFromDate(NSDate.dateWithTimeIntervalSince1970(epochMillis / 1000.0))
}

actual fun formatDisplayDate(epochMillis: Long): String {
    return iosDisplayDateFormatter.stringFromDate(NSDate.dateWithTimeIntervalSince1970(epochMillis / 1000.0))
}

actual fun calculateHeatmapGridBounds(weeksCount: Int): HeatmapCalendarBounds {
    val calendar = NSCalendar.currentCalendar
    val now = NSDate()

    val startOfToday = calendar.startOfDayForDate(now)
    val endOfTodayComps = NSDateComponents().apply {
        hour = 23
        minute = 59
        second = 59
    }
    val todayEnd = calendar.dateByAddingComponents(endOfTodayComps, toDate = startOfToday, options = 0u) ?: now

    val weekday = calendar.component(NSCalendarUnitWeekday, fromDate = startOfToday)
    val daysToSaturday = (7L - weekday).coerceAtLeast(0L)
    val endSaturday = calendar.dateByAddingUnit(NSCalendarUnitDay, value = daysToSaturday, toDate = startOfToday, options = 0u) ?: startOfToday
    val startDate = calendar.dateByAddingUnit(
        NSCalendarUnitDay,
        value = -(weeksCount * 7L - 1L),
        toDate = endSaturday,
        options = 0u
    ) ?: startOfToday

    return HeatmapCalendarBounds(
        todayEndMillis = (todayEnd.timeIntervalSince1970 * 1000.0).toLong(),
        gridStartMillis = (startDate.timeIntervalSince1970 * 1000.0).toLong()
    )
}

actual fun addDaysMillis(epochMillis: Long, days: Int): Long {
    val calendar = NSCalendar.currentCalendar
    val date = NSDate.dateWithTimeIntervalSince1970(epochMillis / 1000.0)
    val shifted = calendar.dateByAddingUnit(NSCalendarUnitDay, value = days.toLong(), toDate = date, options = 0u) ?: date
    return (shifted.timeIntervalSince1970 * 1000.0).toLong()
}
