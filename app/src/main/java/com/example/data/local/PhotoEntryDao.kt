package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.PhotoEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoEntryDao {
    @Query("SELECT * FROM photo_entries ORDER BY timestamp DESC")
    fun getAllPhotos(): Flow<List<PhotoEntry>>

    @Query("SELECT * FROM photo_entries ORDER BY timestamp DESC")
    suspend fun getAllPhotosOnce(): List<PhotoEntry>

    @Query("SELECT * FROM photo_entries WHERE plantId = :plantId ORDER BY timestamp ASC")
    fun getPhotosForPlant(plantId: Long): Flow<List<PhotoEntry>>

    @Query("SELECT * FROM photo_entries WHERE plantId = :plantId ORDER BY timestamp ASC")
    suspend fun getPhotosForPlantOnce(plantId: Long): List<PhotoEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: PhotoEntry): Long

    @Update
    suspend fun updatePhoto(photo: PhotoEntry)

    @Delete
    suspend fun deletePhoto(photo: PhotoEntry)

    @Query("DELETE FROM photo_entries")
    suspend fun deleteAllPhotos()
}
