package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class CareActionType(val displayName: String, val shortCode: String) {
    WATER("Watering", "H2O"),
    MIST("Misting & Foliar", "MIST"),
    FERTILIZE("Fertilizer / Feeding", "NPK"),
    DEFOLIATE("Defoliation & Pruning", "TRIM"),
    TRAIN("Low Stress Training", "LST"),
    NOTE("Observation Note", "LOG")
}

@Entity(
    tableName = "care_logs",
    foreignKeys = [
        ForeignKey(
            entity = Plant::class,
            parentColumns = ["id"],
            childColumns = ["plantId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["plantId"]), Index(value = ["timestamp"])]
)
data class CareLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val plantId: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val actionType: CareActionType,
    val waterAmountMl: Int? = null,
    val phLevel: Double? = null,
    val fertilizerName: String? = null,
    val fertilizerDose: String? = null,
    val ecPpm: Int? = null,
    val notes: String = ""
)
