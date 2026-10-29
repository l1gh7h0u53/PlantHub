package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * GrowthStage represents the botanical phenological phase of the cultivar.
 *
 * DESIGN DECISIONS:
 * - Each stage encapsulates default photoperiod ratios (e.g. 18h light for Vegetative, 12h for Flowering, 0h for Drying).
 * - Changing growth stage automatically synchronizes recommended lighting presets and schedules.
 */
enum class GrowthStage(val label: String, val defaultOnHours: Int, val defaultOffHours: Int) {
    GERMINATION("Germination", 18, 6),
    SEEDLING("Seedling", 18, 6),
    VEGETATIVE("Vegetative", 18, 6),
    FLOWERING("Flowering", 12, 12),
    AUTOFLOWER("Autoflower", 20, 4),
    FLUSHING("Flushing", 12, 12),
    DRYING_CURING("Drying & Curing", 0, 24),
    HARVESTED("Harvested", 0, 24)
}

/**
 * Plant entity represents a distinct cultivar in the grower's garden.
 *
 * TECHNICAL LOGIC & PHOTOPERIOD TIMER STATE MANAGEMENT:
 * 1. Mathematical Time Derivation vs. Battery Drain:
 *    - Instead of running a battery-draining background thread that ticks every second in persistent storage,
 *      the timer's active state is represented purely as an epoch reference: [timerStartedAt].
 *    - When the app is active, [LightCycleEngine] calculates current phase and remaining seconds on-the-fly
 *      by computing: `(currentTimeMillis - timerStartedAt) % (lightOnHours + lightOffHours)`.
 * 2. Pause & Resume Drift Protection:
 *    - When the grower pauses the timer, the exact remaining phase duration is captured into [timerPausedRemainingSec].
 *    - When resumed, [PlantViewModel] adjusts [timerStartedAt] backwards so the resumed timer continues from the
 *      exact second it was paused, completely preventing time loss or clock drift.
 * 3. Maintenance Lockout ([isPausedForMaintenance]):
 *    - When a full 24-hour cycle completes (both ON and OFF intervals elapsed), the system sets
 *      `isPausedForMaintenance = true`. This prevents the light from turning back on unexpectedly while
 *      the grower is inspecting the tent, inspecting root dry-back, or performing night-cycle maintenance.
 */
@Entity(tableName = "plants")
data class Plant(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    // Cultivar identity and genetics
    val name: String,
    val strain: String,
    val stage: GrowthStage = GrowthStage.VEGETATIVE,

    // Photoperiod lighting schedule parameters
    val lightCyclePreset: String = "18/6 Vegetative",
    val lightOnHours: Int = 18,
    val lightOffHours: Int = 6,
    val lightStartTimeMinute: Int = 360, // 06:00 AM (minutes from 00:00)

    // Horticultural specifications
    val medium: String = "Living Soil",
    val potSizeLiters: Double = 12.0,
    val plantedDate: Long = System.currentTimeMillis(),
    val notes: String = "",

    // Photoperiod timer state tracking
    val isTimerRunning: Boolean = false,
    val timerStartedAt: Long? = null,
    val timerPausedRemainingSec: Long? = null,
    val isPausedForMaintenance: Boolean = false,

    // UI representation
    val iconKey: String = "cannabis"
)

