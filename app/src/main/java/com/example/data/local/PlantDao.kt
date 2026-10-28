package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Plant
import kotlinx.coroutines.flow.Flow

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

    @Update
    suspend fun updatePlant(plant: Plant)

    @Delete
    suspend fun deletePlant(plant: Plant)

    @Query("SELECT COUNT(*) FROM plants")
    suspend fun countPlants(): Int

    @Query("DELETE FROM plants")
    suspend fun deleteAllPlants()
}
