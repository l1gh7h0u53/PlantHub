package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ReminderSetting
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderSettingDao {
    @Query("SELECT * FROM reminder_settings WHERE plantId = :plantId")
    fun getRemindersForPlant(plantId: Long): Flow<List<ReminderSetting>>

    @Query("SELECT * FROM reminder_settings WHERE isEnabled = 1")
    fun getAllActiveReminders(): Flow<List<ReminderSetting>>

    @Query("SELECT * FROM reminder_settings")
    suspend fun getAllRemindersOnce(): List<ReminderSetting>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderSetting): Long

    @Update
    suspend fun updateReminder(reminder: ReminderSetting)

    @Delete
    suspend fun deleteReminder(reminder: ReminderSetting)

    @Query("DELETE FROM reminder_settings")
    suspend fun deleteAllReminders()
}
