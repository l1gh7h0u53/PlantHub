package com.example.util

import com.example.data.model.CareActionType
import com.example.data.model.CareLog

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
 * HeatmapCalculator transforms raw [CareLog] database records into structured 2D contribution matrices
 * in pure Kotlin Multiplatform commonMain code.
 *
 * Responsibilities:
 * - Aggregates care events into calendar day buckets ("yyyy-MM-dd").
 * - Builds 16-week or 52-week calendar grids (weeks as columns, days of week as rows 0..6).
 * - Assigns GitHub-style color intensity levels (0 = no activity, 1 = 1 log, 2 = 2 logs, 3 = 3 logs, 4 = 4+ logs).
 * - Computes cultivation engagement metrics: total logged actions, current streak, longest streak, and weekly average.
 */
object HeatmapCalculator {

    /**
     * Builds a standard 16-week matrix ending today.
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
        val bounds = calculateHeatmapGridBounds(weeksCount)
        val gridStartMillis = bounds.gridStartMillis
        val todayEndMillis = bounds.todayEndMillis

        // Only bucket logs falling inside the rendered calendar window to skip needless formatting
        val logsByDate = HashMap<String, MutableList<CareLog>>((weeksCount * 3).coerceAtLeast(16))
        for (log in logs) {
            val ts = log.timestamp
            if (ts in gridStartMillis..todayEndMillis) {
                val key = formatDateKey(ts)
                logsByDate.getOrPut(key) { ArrayList(4) }.add(log)
            }
        }

        var runnerMillis = gridStartMillis
        val weeks = ArrayList<List<HeatmapDay>>(weeksCount)
        var totalContributions = 0
        val dayActivity = BooleanArray(weeksCount * 7)
        var dayActivityCount = 0

        for (w in 0 until weeksCount) {
            val weekDays = ArrayList<HeatmapDay>(7)
            for (d in 0 until 7) {
                val isFuture = runnerMillis > todayEndMillis
                val key = formatDateKey(runnerMillis)
                val dayLogs = if (isFuture) emptyList() else (logsByDate[key] ?: emptyList())
                val count = dayLogs.size
                if (!isFuture) {
                    totalContributions += count
                    dayActivity[dayActivityCount++] = count > 0
                }

                val finalLevel = if (isFuture) {
                    0
                } else {
                    when {
                        count == 0 -> 0
                        count == 1 -> 1
                        count == 2 -> 2
                        count in 3..4 -> 3
                        else -> 4
                    }
                }

                weekDays.add(
                    HeatmapDay(
                        dateKey = key,
                        dateMillis = runnerMillis,
                        displayDate = formatDisplayDate(runnerMillis),
                        dayOfWeek = d,
                        logCount = count,
                        level = finalLevel,
                        careLogs = dayLogs
                    )
                )

                runnerMillis = addDaysMillis(runnerMillis, 1)
            }
            weeks.add(weekDays)
        }

        var currentStreak = 0
        var longestStreak = 0
        var tempStreak = 0

        for (i in (dayActivityCount - 1) downTo 0) {
            if (dayActivity[i]) {
                currentStreak++
            } else {
                if (i == dayActivityCount - 1) {
                    continue
                }
                break
            }
        }

        for (i in 0 until dayActivityCount) {
            if (dayActivity[i]) {
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
