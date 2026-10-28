package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.CareLog
import kotlinx.coroutines.flow.Flow

@Dao
interface CareLogDao {
    @Query("SELECT * FROM care_logs ORDER BY timestamp DESC")
    fun getAllCareLogs(): Flow<List<CareLog>>

    @Query("SELECT * FROM care_logs ORDER BY timestamp DESC")
    suspend fun getAllCareLogsOnce(): List<CareLog>

    @Query("SELECT * FROM care_logs WHERE plantId = :plantId ORDER BY timestamp DESC")
    fun getLogsForPlant(plantId: Long): Flow<List<CareLog>>

    @Query("SELECT * FROM care_logs WHERE plantId = :plantId ORDER BY timestamp DESC")
    suspend fun getLogsForPlantOnce(plantId: Long): List<CareLog>

    @Query("SELECT * FROM care_logs WHERE timestamp >= :startTimestamp AND timestamp <= :endTimestamp ORDER BY timestamp ASC")
    fun getLogsBetween(startTimestamp: Long, endTimestamp: Long): Flow<List<CareLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: CareLog): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<CareLog>)

    @Delete
    suspend fun deleteLog(log: CareLog)

    @Query("SELECT * FROM care_logs WHERE plantId = :plantId ORDER BY timestamp DESC LIMIT 1")
    fun getLatestLogForPlant(plantId: Long): Flow<CareLog?>

    @Query("DELETE FROM care_logs")
    suspend fun deleteAllCareLogs()
}
