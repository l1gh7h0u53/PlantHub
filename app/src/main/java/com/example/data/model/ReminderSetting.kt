package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reminder_settings",
    foreignKeys = [
        ForeignKey(
            entity = Plant::class,
            parentColumns = ["id"],
            childColumns = ["plantId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["plantId"])]
)
data class ReminderSetting(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val plantId: Long,
    val careType: CareActionType,
    val intervalHours: Int = 48,
    val lastTriggered: Long = System.currentTimeMillis(),
    val isEnabled: Boolean = true,
    val customNote: String = ""
)
