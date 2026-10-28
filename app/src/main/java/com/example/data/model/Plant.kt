package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

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

@Entity(tableName = "plants")
data class Plant(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val strain: String,
    val stage: GrowthStage = GrowthStage.VEGETATIVE,
    val lightCyclePreset: String = "18/6 Vegetative",
    val lightOnHours: Int = 18,
    val lightOffHours: Int = 6,
    val lightStartTimeMinute: Int = 360, // 06:00 AM (minutes from 00:00)
    val medium: String = "Living Soil",
    val potSizeLiters: Double = 12.0,
    val plantedDate: Long = System.currentTimeMillis(),
    val notes: String = "",
    val isTimerRunning: Boolean = false,
    val timerStartedAt: Long? = null,
    val timerPausedRemainingSec: Long? = null,
    val isPausedForMaintenance: Boolean = false,
    val iconKey: String = "cannabis"
)
