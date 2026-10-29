package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Plant
import kotlinx.coroutines.flow.Flow

/**
 * PlantDao defines the data access contract for cultivar lifecycle and photoperiod configurations.
 *
 * DESIGN DECISIONS & TECHNICAL LOGIC:
 * 1. Dual Access Patterns (Flow vs Suspend):
 *    - Reactive [Flow] queries hook directly into Room's InvalidationTracker to automatically re-emit
 *      the cultivar collection whenever underlying tables mutate (e.g. stage transition or timer toggle),
 *      eliminating the need for manual cache-invalidation or pull-to-refresh.
 *    - One-shot [suspend] queries are provided for background services, automated backups, and seed checks
 *      where keeping persistent active SQLite cursor subscriptions would cause memory and thread leaks.
 * 2. Chronological Ordering (ORDER BY plantedDate DESC):
 *    - Places the newest active germinations and seedling batches at the top of the grower's garden screen,
 *      matching agronomic priority (younger plants need higher observation frequency than mature ones).
 * 3. REPLACE Conflict Strategy:
 *    - Simplifies full-fidelity offline backup restoration and JSON/CSV imports. If an imported plant
 *      shares a primary key ID, it replaces the existing entity idempotently without foreign key collisions.
 */
@Dao
interface PlantDao {
    /**
     * Continuous reactive stream of all registered plants, sorted newest first.
     * Triggers UI recomposition across HomeScreen and PlantList automatically on data mutation.
     */
    @Query("SELECT * FROM plants ORDER BY plantedDate DESC")
    fun getAllPlants(): Flow<List<Plant>>

    /**
     * One-shot fetch of all plants. Used by [PlantBackupManager] during serialization and export
     * to avoid holding long-lived SQLite cursor locks.
     */
    @Query("SELECT * FROM plants ORDER BY plantedDate DESC")
    suspend fun getAllPlantsOnce(): List<Plant>

    /**
     * Continuous reactive stream observing a specific cultivar by its primary key.
     * Drives the TimerScreen and PlantDetailScreen, instantly propagating timer starts, pauses,
     * and growth stage updates.
     */
    @Query("SELECT * FROM plants WHERE id = :id")
    fun getPlantById(id: Long): Flow<Plant?>

    /**
     * One-shot lookup for background notification scheduling and timer calculations.
     */
    @Query("SELECT * FROM plants WHERE id = :id LIMIT 1")
    suspend fun getPlantByIdOnce(id: Long): Plant?

    /**
     * Inserts or replaces a cultivar record.
     * OnConflictStrategy.REPLACE ensures deterministic idempotency during database imports.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlant(plant: Plant): Long

    /**
     * Updates modified cultivar properties (such as timer state, photoperiod preset, or notes).
     */
    @Update
    suspend fun updatePlant(plant: Plant)

    /**
     * Removes a cultivar from the database.
     * Note: Cascading foreign keys remove associated CareLogs and PhotoEntries automatically.
     */
    @Delete
    suspend fun deletePlant(plant: Plant)

    /**
     * Evaluates total plant count to determine whether initial starter seed data or backup recovery is needed.
     */
    @Query("SELECT COUNT(*) FROM plants")
    suspend fun countPlants(): Int

    /**
     * Purges all cultivar rows; primarily used during factory resets or test suite tear-downs.
     */
    @Query("DELETE FROM plants")
    suspend fun deleteAllPlants()
}

