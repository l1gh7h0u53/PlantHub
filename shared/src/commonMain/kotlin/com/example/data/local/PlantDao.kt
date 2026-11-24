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
 * PlantDao defines the multiplatform data access contract for cultivar lifecycle and photoperiod configurations.
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
 *      matching agronomic priority.
 * 3. REPLACE Conflict Strategy:
 *    - Simplifies full-fidelity offline backup restoration and JSON/CSV imports without foreign key collisions.
 */
@Dao
interface PlantDao {
    @Query("SELECT * FROM plants ORDER BY plantedDate DESC")
    fun getAllPlants(): Flow<List<Plant>>

    @Query("SELECT * FROM plants ORDER BY plantedDate DESC")
    suspend fun getAllPlantsOnce(): List<Plant>

    @Query("SELECT * FROM plants WHERE id = :id")
    fun getPlantById(id: Long): Flow<Plant?>

    @Query("SELECT * FROM plants WHERE id = :id LIMIT 1")
    suspend fun getPlantByIdOnce(id: Long): Plant?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlant(plant: Plant): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlants(plants: List<Plant>): List<Long>

    @Update
    suspend fun updatePlant(plant: Plant)

    @Delete
    suspend fun deletePlant(plant: Plant)

    @Query("SELECT COUNT(*) FROM plants")
    suspend fun countPlants(): Int

    @Query("DELETE FROM plants")
    suspend fun deleteAllPlants()
}
