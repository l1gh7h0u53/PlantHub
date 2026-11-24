package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val time12hFormatThreadLocal = object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue(): SimpleDateFormat = SimpleDateFormat("h:mm a", Locale.US)
}

private val dateKeyFormatThreadLocal = object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue(): SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
}

private val displayDateFormatThreadLocal = object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue(): SimpleDateFormat = SimpleDateFormat("EEE, MMM d, yyyy", Locale.US)
}

private val reusableDateThreadLocal = object : ThreadLocal<Date>() {
    override fun initialValue(): Date = Date()
}

private val reusableCalendarThreadLocal = object : ThreadLocal<Calendar>() {
    override fun initialValue(): Calendar = Calendar.getInstance()
}

private fun formatWithCached(
    formatterLocal: ThreadLocal<SimpleDateFormat>,
    epochMillis: Long
): String {
    val formatter = formatterLocal.get()!!
    val tz = TimeZone.getDefault()
    if (formatter.timeZone != tz) {
        formatter.timeZone = tz
    }
    val date = reusableDateThreadLocal.get()!!
    date.time = epochMillis
    return formatter.format(date)
}

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun formatTime12h(epochMillis: Long): String =
    formatWithCached(time12hFormatThreadLocal, epochMillis)

actual fun formatDateKey(epochMillis: Long): String =
    formatWithCached(dateKeyFormatThreadLocal, epochMillis)

actual fun formatDisplayDate(epochMillis: Long): String =
    formatWithCached(displayDateFormatThreadLocal, epochMillis)

actual fun calculateHeatmapGridBounds(weeksCount: Int): HeatmapCalendarBounds {
    val cal = reusableCalendarThreadLocal.get()!!.apply {
        timeZone = TimeZone.getDefault()
        timeInMillis = System.currentTimeMillis()
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }
    val todayEndMillis = cal.timeInMillis

    while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY) {
        cal.add(Calendar.DAY_OF_MONTH, 1)
    }

    cal.add(Calendar.DAY_OF_MONTH, -(weeksCount * 7 - 1))
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)

    return HeatmapCalendarBounds(
        todayEndMillis = todayEndMillis,
        gridStartMillis = cal.timeInMillis
    )
}

actual fun addDaysMillis(epochMillis: Long, days: Int): Long {
    val cal = reusableCalendarThreadLocal.get()!!.apply {
        timeZone = TimeZone.getDefault()
        timeInMillis = epochMillis
        add(Calendar.DAY_OF_MONTH, days)
    }
    return cal.timeInMillis
}
