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
        val logsByDate = mutableMapOf<String, MutableList<CareLog>>()
        for (log in logs) {
            val key = formatDateKey(log.timestamp)
            logsByDate.getOrPut(key) { mutableListOf() }.add(log)
        }

        val bounds = calculateHeatmapGridBounds(weeksCount)
        var runnerMillis = bounds.gridStartMillis
        val todayEndMillis = bounds.todayEndMillis

        val weeks = mutableListOf<List<HeatmapDay>>()
        var totalContributions = 0
        val dayActivityList = mutableListOf<Boolean>()

        for (w in 0 until weeksCount) {
            val weekDays = mutableListOf<HeatmapDay>()
            for (d in 0 until 7) {
                val key = formatDateKey(runnerMillis)
                val dayLogs = logsByDate[key] ?: emptyList()
                val count = dayLogs.size
                totalContributions += count

                val level = when {
                    count == 0 -> 0
                    count == 1 -> 1
                    count == 2 -> 2
                    count in 3..4 -> 3
                    else -> 4
                }

                val isFuture = runnerMillis > todayEndMillis
                val finalLevel = if (isFuture) 0 else level

                val heatmapDay = HeatmapDay(
                    dateKey = key,
                    dateMillis = runnerMillis,
                    displayDate = formatDisplayDate(runnerMillis),
                    dayOfWeek = d,
                    logCount = if (isFuture) 0 else count,
                    level = finalLevel,
                    careLogs = if (isFuture) emptyList() else dayLogs
                )
                weekDays.add(heatmapDay)

                if (!isFuture) {
                    dayActivityList.add(count > 0)
                }

                runnerMillis = addDaysMillis(runnerMillis, 1)
            }
            weeks.add(weekDays)
        }

        var currentStreak = 0
        var longestStreak = 0
        var tempStreak = 0

        for (i in dayActivityList.indices.reversed()) {
            if (dayActivityList[i]) {
                currentStreak++
            } else {
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
