package com.example.data.model

data class LightCycleStatus(
    val isLightOn: Boolean,
    val currentPhaseName: String, // "LIGHTS ON", "LIGHTS OFF", "WAITING TO START", "PAUSED"
    val hoursRemainingInPhase: Long,
    val minutesRemainingInPhase: Long,
    val secondsRemainingInPhase: Long,
    val phaseProgressPercent: Float,
    val startTimeFormatted: String,
    val nextSwitchTimeFormatted: String,
    val totalOnHours: Int,
    val totalOffHours: Int,
    val presetName: String,
    val isTimerRunning: Boolean,
    val isWaitingToStart: Boolean,
    val isPausedForMaintenance: Boolean = false,
    val nextSwitchEpochMillis: Long = 0L
)
