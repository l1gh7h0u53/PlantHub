package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.CareLog
import kotlinx.coroutines.flow.Flow

/**
 * CareLogDao manages the agronomic telemetry and maintenance event ledger in Room KMP.
 *
 * DESIGN DECISIONS & TECHNICAL LOGIC:
 * 1. Chronological Sorting Strategies:
 *    - Feed queries use `ORDER BY timestamp DESC` so the most recent irrigation, nutrient mixing,
 *      pruning, or IPM spray appears immediately at the top of the event timeline.
 *    - Range-based heatmap queries use `ORDER BY timestamp ASC` so the HeatmapCalculator can iterate
 *      sequentially forward through 16-week or 52-week time buckets without index backtracking.
 * 2. Plant Isolation & Foreign Key Coupling:
 *    - `WHERE plantId = :plantId` scopes maintenance history strictly to the selected cultivar.
 * 3. Latest-Log Optimization:
 *    - `getLatestLogForPlant(plantId)` executes `ORDER BY timestamp DESC LIMIT 1` for O(1) index-backed
 *      dry-back checks without materializing older rows.
 */
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
