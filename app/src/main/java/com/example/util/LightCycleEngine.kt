package com.example.util

import com.example.data.model.LightCycleStatus
import com.example.data.model.Plant
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * LightCycleEngine is the pure mathematical domain engine for computing photoperiod states.
 *
 * Responsibilities:
 * - Calculates active lighting phase (LIGHTS ON vs. LIGHTS OFF) based on start epoch and duration.
 * - Computes remaining countdown timers (hours, minutes, seconds) until next cycle transition.
 * - Determines continuous progress percentages (0.0 to 1.0) for visual gauge meters.
 * - Formats human-readable switchover timestamps (e.g. "8:00 PM").
 * - Handles state machine transitions: 24/0 continuous, waiting-to-start, paused, and maintenance mode.
 */
object LightCycleEngine {

    // Formatter for displaying switchover times in standard 12-hour AM/PM format
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.US)

    /**
     * Computes the real-time [LightCycleStatus] for the given [Plant].
     *
     * State evaluation precedence:
     * 1. 24/0 Continuous lighting (always ON, 100% progress).
     * 2. Maintenance Pause (cycle completed, awaiting user intervention).
     * 3. Waiting to Start (user has not yet activated the timer; full duration remains).
     * 4. Paused State (timer temporarily stopped with frozen remaining duration).
     * 5. Active Running State (computes current phase, elapsed progress, and switchover time).
     */
    fun calculateStatus(plant: Plant): LightCycleStatus {
        val onHours = plant.lightOnHours.coerceAtLeast(0)
        val offHours = plant.lightOffHours.coerceAtLeast(0)
        val totalOnSec = onHours * 3600L
        val totalOffSec = offHours * 3600L
        val totalCycleSec = totalOnSec + totalOffSec

        // TECHNICAL LOGIC: Continuous Lighting Edge Case
        // In 24/0 vegetative regimes or mother plant tents, dark period is 0 hours.
        // The lights never turn off, progress is pinned to 100%, and switchover is marked "Continuous".
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

        // DESIGN DECISION: Automated Maintenance Lockout Precedence
        // When a full photoperiod cycle (ON + OFF duration) has completed, the system enters a safety lockout.
        // This state prevents lights from turning back on unexpectedly while tent maintenance, watering,
        // or inspection is pending. It requires deliberate grower reactivation.
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

        // Case 1: Unstarted State (WAITING TO START)
        // If the grower has defined the preset but not tapped 'Start', the UI displays full duration
        // in standby mode with 0.0% progress and prompts activation.
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

        // Case 2: Frozen Paused State
        // When paused by the grower, [timerPausedRemainingSec] holds the frozen countdown.
        // Progress is computed against the full phase duration so the circular gauge stays frozen in place.
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

        // Case 3: Active Running Photoperiod (Mathematical Timeline Evaluation)
        // TECHNICAL LOGIC:
        // We compute elapsed seconds purely using system epoch offsets: (now - startedAt).
        // This is 100% resilient to device sleep, app termination, or device reboots.
        val startedAt = plant.timerStartedAt ?: System.currentTimeMillis()
        val now = System.currentTimeMillis()
        val elapsedSec = ((now - startedAt) / 1000L).coerceAtLeast(0L)

        // If elapsed duration has reached or exceeded the total cycle duration (ON + OFF),
        // trigger the maintenance lockout state.
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

        // TECHNICAL LOGIC: Circular 24-Hour Modulo Position
        // `cyclePosSec = elapsedSec % totalCycleSec` determines the exact second within the current 24-hour day.
        // If `cyclePosSec < totalOnSec`: Cultivar is currently in the LIGHT ON photosynthetic phase.
        // If `cyclePosSec >= totalOnSec`: Cultivar is currently in the LIGHT OFF photoperiodic rest phase.
        val cyclePosSec = if (totalCycleSec > 0) elapsedSec % totalCycleSec else 0L
        val isLightOn = cyclePosSec < totalOnSec

        // Calculate phase-specific remaining duration and normalized float progress (0.0 to 1.0)
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

        // Deconstruct remaining seconds into human-readable hours, minutes, and seconds
        val remHours = remainingSec / 3600
        val remMinutes = (remainingSec % 3600) / 60
        val remSeconds = remainingSec % 60

        // Calculate exact calendar timestamp when the lighting relay will switch state
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
