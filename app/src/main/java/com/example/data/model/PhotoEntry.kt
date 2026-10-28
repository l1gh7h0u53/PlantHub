package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "photo_entries",
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
data class PhotoEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val plantId: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val imageUri: String,
    val stage: GrowthStage = GrowthStage.VEGETATIVE,
    val dayNumber: Int = 1,
    val title: String = "",
    val observation: String = "",
    val canopyHeightCm: Double? = null
)
