package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun formatTime12h(epochMillis: Long): String {
    val formatter = SimpleDateFormat("h:mm a", Locale.US)
    return formatter.format(Date(epochMillis))
}

actual fun formatDateKey(epochMillis: Long): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    return formatter.format(Date(epochMillis))
}

actual fun formatDisplayDate(epochMillis: Long): String {
    val formatter = SimpleDateFormat("EEE, MMM d, yyyy", Locale.US)
    return formatter.format(Date(epochMillis))
}

actual fun calculateHeatmapGridBounds(weeksCount: Int): HeatmapCalendarBounds {
    val todayCal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }

    val endCal = (todayCal.clone() as Calendar)
    while (endCal.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY) {
        endCal.add(Calendar.DAY_OF_MONTH, 1)
    }

    val startCal = (endCal.clone() as Calendar).apply {
        add(Calendar.DAY_OF_MONTH, -(weeksCount * 7 - 1))
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    return HeatmapCalendarBounds(
        todayEndMillis = todayCal.timeInMillis,
        gridStartMillis = startCal.timeInMillis
    )
}

actual fun addDaysMillis(epochMillis: Long, days: Int): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = epochMillis
        add(Calendar.DAY_OF_MONTH, days)
    }
    return cal.timeInMillis
}
