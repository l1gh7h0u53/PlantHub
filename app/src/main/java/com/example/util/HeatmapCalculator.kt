package com.example.util

import com.example.data.model.CareActionType
import com.example.data.model.CareLog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class HeatmapDay(
    val dateKey: String, // "yyyy-MM-dd"
    val dateMillis: Long,
    val displayDate: String,
    val dayOfWeek: Int, // 0 = Sunday, 6 = Saturday
    val logCount: Int,
    val level: Int, // 0..4 intensity
    val careLogs: List<CareLog>
)

data class HeatmapGridData(
    val weeks: List<List<HeatmapDay>>,
    val totalContributions: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val averagePerWeek: Double
)

/**
 * HeatmapCalculator transforms raw [CareLog] database records into structured 2D contribution matrices.
 *
 * Responsibilities:
 * - Aggregates care events into calendar day buckets ("yyyy-MM-dd").
 * - Builds 16-week or 52-week calendar grids (weeks as columns, days of week as rows 0..6).
 * - Assigns GitHub-style color intensity levels (0 = no activity, 1 = 1 log, 2 = 2 logs, 3 = 3 logs, 4 = 4+ logs).
 * - Computes cultivation engagement metrics: total logged actions, current streak, longest streak, and weekly average.
 */
object HeatmapCalculator {

    private val keyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val displayFormat = SimpleDateFormat("EEE, MMM d, yyyy", Locale.US)

    /**
     * Builds a standard 16-week matrix ending today.
     * Generates 16 columns (weeks), with each column containing 7 day slots (Sunday to Saturday).
     */
    fun calculate16Weeks(logs: List<CareLog>): HeatmapGridData {
        return calculateWeeksMatrix(logs, weeksCount = 16)
    }

    /**
     * Builds an expanded full-year (52-week) matrix ending today for comprehensive season review.
     */
    fun calculateYearly(logs: List<CareLog>): HeatmapGridData {
        return calculateWeeksMatrix(logs, weeksCount = 52)
    }

    private fun calculateWeeksMatrix(logs: List<CareLog>, weeksCount: Int): HeatmapGridData {
        // Group care logs by "yyyy-MM-dd"
        val logsByDate = mutableMapOf<String, MutableList<CareLog>>()
        for (log in logs) {
            val key = keyFormat.format(log.timestamp)
            logsByDate.getOrPut(key) { mutableListOf() }.add(log)
        }

        val todayCal = Calendar.getInstance()
        todayCal.set(Calendar.HOUR_OF_DAY, 23)
        todayCal.set(Calendar.MINUTE, 59)
        todayCal.set(Calendar.SECOND, 59)
        todayCal.set(Calendar.MILLISECOND, 999)

        // Find the most recent Saturday (end of current week in US calendar)
        val endCal = todayCal.clone() as Calendar
        while (endCal.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY) {
            endCal.add(Calendar.DAY_OF_MONTH, 1)
        }

        // Start calendar: go back (weeksCount) weeks to the prior Sunday
        val startCal = endCal.clone() as Calendar
        startCal.add(Calendar.DAY_OF_MONTH, -(weeksCount * 7 - 1))
        startCal.set(Calendar.HOUR_OF_DAY, 0)
        startCal.set(Calendar.MINUTE, 0)
        startCal.set(Calendar.SECOND, 0)
        startCal.set(Calendar.MILLISECOND, 0)

        val weeks = mutableListOf<List<HeatmapDay>>()
        val runnerCal = startCal.clone() as Calendar

        var totalContributions = 0
        val dayActivityList = mutableListOf<Boolean>()

        for (w in 0 until weeksCount) {
            val weekDays = mutableListOf<HeatmapDay>()
            for (d in 0 until 7) {
                val key = keyFormat.format(runnerCal.time)
                val dayLogs = logsByDate[key] ?: emptyList()
                val count = dayLogs.size
                totalContributions += count

                // Level intensity calculation (GitHub standard)
                val level = when {
                    count == 0 -> 0
                    count == 1 -> 1
                    count == 2 -> 2
                    count in 3..4 -> 3
                    else -> 4
                }

                // If date is after today, mark as future (level 0)
                val isFuture = runnerCal.timeInMillis > todayCal.timeInMillis
                val finalLevel = if (isFuture) 0 else level

                val heatmapDay = HeatmapDay(
                    dateKey = key,
                    dateMillis = runnerCal.timeInMillis,
                    displayDate = displayFormat.format(runnerCal.time),
                    dayOfWeek = d,
                    logCount = if (isFuture) 0 else count,
                    level = finalLevel,
                    careLogs = if (isFuture) emptyList() else dayLogs
                )
                weekDays.add(heatmapDay)

                if (!isFuture) {
                    dayActivityList.add(count > 0)
                }

                runnerCal.add(Calendar.DAY_OF_MONTH, 1)
            }
            weeks.add(weekDays)
        }

        // Compute streak
        var currentStreak = 0
        var longestStreak = 0
        var tempStreak = 0

        // Traverse backwards from last non-future day for current streak
        for (i in dayActivityList.indices.reversed()) {
            if (dayActivityList[i]) {
                currentStreak++
            } else {
                // If today is empty, check if yesterday was active
                if (i == dayActivityList.lastIndex) {
                    continue
                }
                break
            }
        }

        for (active in dayActivityList) {
            if (active) {
                tempStreak++
                if (tempStreak > longestStreak) longestStreak = tempStreak
            } else {
                tempStreak = 0
            }
        }

        val avgPerWeek = if (weeksCount > 0) totalContributions.toDouble() / weeksCount else 0.0

        return HeatmapGridData(
            weeks = weeks,
            totalContributions = totalContributions,
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            averagePerWeek = avgPerWeek
        )
    }

    fun describeAction(log: CareLog): String {
        return buildString {
            append(log.actionType.displayName)
            when (log.actionType) {
                CareActionType.WATER -> {
                    log.waterAmountMl?.let { append(" ($it mL)") }
                    log.phLevel?.let { append(" [pH: $it]") }
                }
                CareActionType.FERTILIZE -> {
                    log.fertilizerName?.let { append(" - $it") }
                    log.fertilizerDose?.let { append(" ($it)") }
                }
                else -> {}
            }
            if (log.notes.isNotBlank()) {
                append(": \"${log.notes}\"")
            }
        }
    }
}
