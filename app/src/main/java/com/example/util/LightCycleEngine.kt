package com.example.util

import com.example.data.model.LightCycleStatus
import com.example.data.model.Plant
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LightCycleEngine {

    private val timeFormat = SimpleDateFormat("h:mm a", Locale.US)

    /**
     * Calculates the real-time photoperiod status.
     * If the user has not started the timer, the timer stays in "WAITING TO START" with the full phase duration remaining.
     */
    fun calculateStatus(plant: Plant): LightCycleStatus {
        val onHours = plant.lightOnHours.coerceAtLeast(0)
        val offHours = plant.lightOffHours.coerceAtLeast(0)
        val totalOnSec = onHours * 3600L
        val totalOffSec = offHours * 3600L
        val totalCycleSec = totalOnSec + totalOffSec

        // Handle 24/0 continuous lights or 0/24 complete dark
        if (offHours == 0) {
            return LightCycleStatus(
                isLightOn = true,
                currentPhaseName = "LIGHTS ON (24/0)",
                hoursRemainingInPhase = 24,
                minutesRemainingInPhase = 0,
                secondsRemainingInPhase = 0,
                phaseProgressPercent = 1.0f,
                startTimeFormatted = "Continuous",
                nextSwitchTimeFormatted = "Continuous",
                totalOnHours = 24,
                totalOffHours = 0,
                presetName = plant.lightCyclePreset,
                isTimerRunning = true,
                isWaitingToStart = false,
                isPausedForMaintenance = false
            )
        }

        // Case 0: Automatic Maintenance Pause (Cycle completed)
        if (plant.isPausedForMaintenance) {
            return LightCycleStatus(
                isLightOn = false,
                currentPhaseName = "PAUSED (MAINTENANCE)",
                hoursRemainingInPhase = 0,
                minutesRemainingInPhase = 0,
                secondsRemainingInPhase = 0,
                phaseProgressPercent = 1.0f,
                startTimeFormatted = "Off-cycle complete",
                nextSwitchTimeFormatted = "Awaiting maintenance",
                totalOnHours = onHours,
                totalOffHours = offHours,
                presetName = plant.lightCyclePreset,
                isTimerRunning = false,
                isWaitingToStart = false,
                isPausedForMaintenance = true
            )
        }

        // Case 1: Timer not running and no paused time -> WAITING TO START (Full duration remaining)
        if (!plant.isTimerRunning && plant.timerPausedRemainingSec == null) {
            val remHours = totalOnSec / 3600
            val remMinutes = (totalOnSec % 3600) / 60
            val remSeconds = totalOnSec % 60

            return LightCycleStatus(
                isLightOn = false,
                currentPhaseName = "WAITING TO START",
                hoursRemainingInPhase = remHours,
                minutesRemainingInPhase = remMinutes,
                secondsRemainingInPhase = remSeconds,
                phaseProgressPercent = 0.0f,
                startTimeFormatted = "Not started yet",
                nextSwitchTimeFormatted = "Starts on activation",
                totalOnHours = onHours,
                totalOffHours = offHours,
                presetName = plant.lightCyclePreset,
                isTimerRunning = false,
                isWaitingToStart = true,
                isPausedForMaintenance = false
            )
        }

        // Case 2: Timer paused with preserved remaining seconds
        if (!plant.isTimerRunning && plant.timerPausedRemainingSec != null) {
            val remSec = plant.timerPausedRemainingSec.coerceAtLeast(0L)
            val remHours = remSec / 3600
            val remMinutes = (remSec % 3600) / 60
            val remSeconds = remSec % 60
            val progress = if (totalOnSec > 0) {
                ((totalOnSec - remSec).toFloat() / totalOnSec).coerceIn(0f, 1f)
            } else 0f

            return LightCycleStatus(
                isLightOn = false,
                currentPhaseName = "PAUSED",
                hoursRemainingInPhase = remHours,
                minutesRemainingInPhase = remMinutes,
                secondsRemainingInPhase = remSeconds,
                phaseProgressPercent = progress,
                startTimeFormatted = "Paused",
                nextSwitchTimeFormatted = "Paused",
                totalOnHours = onHours,
                totalOffHours = offHours,
                presetName = plant.lightCyclePreset,
                isTimerRunning = false,
                isWaitingToStart = false,
                isPausedForMaintenance = false
            )
        }

        // Case 3: Timer is RUNNING
        val startedAt = plant.timerStartedAt ?: System.currentTimeMillis()
        val now = System.currentTimeMillis()
        val elapsedSec = ((now - startedAt) / 1000L).coerceAtLeast(0L)

        // If the elapsed time has exceeded a full cycle duration (ON + OFF), cycle has completed!
        if (totalCycleSec > 0 && elapsedSec >= totalCycleSec) {
            return LightCycleStatus(
                isLightOn = false,
                currentPhaseName = "PAUSED (MAINTENANCE)",
                hoursRemainingInPhase = 0,
                minutesRemainingInPhase = 0,
                secondsRemainingInPhase = 0,
                phaseProgressPercent = 1.0f,
                startTimeFormatted = timeFormat.format(Date(startedAt)),
                nextSwitchTimeFormatted = "Awaiting maintenance",
                totalOnHours = onHours,
                totalOffHours = offHours,
                presetName = plant.lightCyclePreset,
                isTimerRunning = false,
                isWaitingToStart = false,
                isPausedForMaintenance = true
            )
        }

        val cyclePosSec = if (totalCycleSec > 0) elapsedSec % totalCycleSec else 0L
        val isLightOn = cyclePosSec < totalOnSec

        val (remainingSec, progress) = if (isLightOn) {
            val rem = (totalOnSec - cyclePosSec).coerceAtLeast(0L)
            val prog = if (totalOnSec > 0) (cyclePosSec.toFloat() / totalOnSec).coerceIn(0f, 1f) else 1f
            Pair(rem, prog)
        } else {
            val offElapsed = cyclePosSec - totalOnSec
            val rem = (totalOffSec - offElapsed).coerceAtLeast(0L)
            val prog = if (totalOffSec > 0) (offElapsed.toFloat() / totalOffSec).coerceIn(0f, 1f) else 1f
            Pair(rem, prog)
        }

        val remHours = remainingSec / 3600
        val remMinutes = (remainingSec % 3600) / 60
        val remSeconds = remainingSec % 60

        val nextSwitchMillis = now + (remainingSec * 1000L)
        val nextSwitchFormatted = timeFormat.format(Date(nextSwitchMillis))
        val startFormatted = timeFormat.format(Date(startedAt))

        return LightCycleStatus(
            isLightOn = isLightOn,
            currentPhaseName = if (isLightOn) "LIGHTS ON" else "LIGHTS OFF",
            hoursRemainingInPhase = remHours,
            minutesRemainingInPhase = remMinutes,
            secondsRemainingInPhase = remSeconds,
            phaseProgressPercent = progress,
            startTimeFormatted = startFormatted,
            nextSwitchTimeFormatted = nextSwitchFormatted,
            totalOnHours = onHours,
            totalOffHours = offHours,
            presetName = plant.lightCyclePreset,
            isTimerRunning = true,
            isWaitingToStart = false,
            isPausedForMaintenance = false,
            nextSwitchEpochMillis = nextSwitchMillis
        )
    }

    fun formatMinutes(minuteOfDay: Int): String {
        val h = minuteOfDay / 60
        val m = minuteOfDay % 60
        val amPm = if (h >= 12) "PM" else "AM"
        val displayHour = when {
            h == 0 -> 12
            h > 12 -> h - 12
            else -> h
        }
        return String.format(Locale.US, "%02d:%02d %s", displayHour, m, amPm)
    }
}
