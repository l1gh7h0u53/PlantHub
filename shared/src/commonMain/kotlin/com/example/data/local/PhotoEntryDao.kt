package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.PhotoEntry
import kotlinx.coroutines.flow.Flow

/**
 * PhotoEntryDao manages the visual milestone gallery and canopy height progression in Room KMP.
 *
 * DESIGN DECISIONS & TECHNICAL LOGIC:
 * 1. Progression Sorting (`ORDER BY timestamp ASC` for individual plants):
 *    - Allows timeline comparison views to render Day 1 -> Day 7 -> Day 14 chronologically.
 * 2. SQLite CursorWindow Protection:
 *    - Stores private sandbox URI paths instead of binary image BLOBs to avoid 2MB CursorWindow limits.
 */
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotos(photos: List<PhotoEntry>)

    @Update
    suspend fun updatePhoto(photo: PhotoEntry)

    @Delete
    suspend fun deletePhoto(photo: PhotoEntry)

    @Query("DELETE FROM photo_entries")
    suspend fun deleteAllPhotos()
}
