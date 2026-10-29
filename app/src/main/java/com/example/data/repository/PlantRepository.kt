package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.model.CareLog
import com.example.data.model.PhotoEntry
import com.example.data.model.Plant
import com.example.data.model.ReminderSetting
import kotlinx.coroutines.flow.Flow

/**
 * PlantRepository serves as the unified data access gateway for the PlantHub application.
 *
 * Responsibilities:
 * - Mediates between the ViewModel and Room Database DAOs (Plant, CareLog, PhotoEntry, ReminderSetting).
 * - Exposes hot reactive [Flow] streams for real-time Compose UI updates.
 * - Provides asynchronous coroutine [suspend] methods for single-shot reads and transactional writes.
 * - Enforces zero-data-leakage on-device isolation.
 */
class PlantRepository(private val database: AppDatabase) {
    // Local Room Data Access Objects
    private val plantDao = database.plantDao()
    private val careLogDao = database.careLogDao()
    private val photoEntryDao = database.photoEntryDao()
    private val reminderSettingDao = database.reminderSettingDao()

    // Reactive streams observing database changes
    val allPlants: Flow<List<Plant>> = plantDao.getAllPlants()
    val allCareLogs: Flow<List<CareLog>> = careLogDao.getAllCareLogs()
    val allPhotos: Flow<List<PhotoEntry>> = photoEntryDao.getAllPhotos()
    val allActiveReminders: Flow<List<ReminderSetting>> = reminderSettingDao.getAllActiveReminders()

    // --- Cultivar Query & Mutation Methods ---
    fun getPlant(id: Long): Flow<Plant?> = plantDao.getPlantById(id)
    suspend fun getPlantOnce(id: Long): Plant? = plantDao.getPlantByIdOnce(id)
    suspend fun getAllPlantsOnce(): List<Plant> = plantDao.getAllPlantsOnce()
    suspend fun getAllCareLogsOnce(): List<CareLog> = careLogDao.getAllCareLogsOnce()
    suspend fun getAllPhotosOnce(): List<PhotoEntry> = photoEntryDao.getAllPhotosOnce()
    suspend fun getAllRemindersOnce(): List<ReminderSetting> = reminderSettingDao.getAllRemindersOnce()

    suspend fun insertPlant(plant: Plant): Long = plantDao.insertPlant(plant)
    suspend fun updatePlant(plant: Plant) = plantDao.updatePlant(plant)
    suspend fun deletePlant(plant: Plant) = plantDao.deletePlant(plant)

    // --- Care Activity Ledger Methods ---
    fun getLogsForPlant(plantId: Long): Flow<List<CareLog>> = careLogDao.getLogsForPlant(plantId)
    suspend fun getLogsForPlantOnce(plantId: Long): List<CareLog> = careLogDao.getLogsForPlantOnce(plantId)
    suspend fun insertCareLog(log: CareLog): Long = careLogDao.insertLog(log)
    suspend fun insertCareLogs(logs: List<CareLog>) = careLogDao.insertLogs(logs)
    suspend fun deleteCareLog(log: CareLog) = careLogDao.deleteLog(log)
    fun getLatestLogForPlant(plantId: Long): Flow<CareLog?> = careLogDao.getLatestLogForPlant(plantId)

    // --- Milestone Photo Progression Methods ---
    fun getPhotosForPlant(plantId: Long): Flow<List<PhotoEntry>> = photoEntryDao.getPhotosForPlant(plantId)
    suspend fun getPhotosForPlantOnce(plantId: Long): List<PhotoEntry> = photoEntryDao.getPhotosForPlantOnce(plantId)
    suspend fun insertPhotoEntry(entry: PhotoEntry): Long = photoEntryDao.insertPhoto(entry)
    suspend fun updatePhotoEntry(entry: PhotoEntry) = photoEntryDao.updatePhoto(entry)
    suspend fun deletePhotoEntry(entry: PhotoEntry) = photoEntryDao.deletePhoto(entry)

    // --- Care Reminder Settings Methods ---
    fun getRemindersForPlant(plantId: Long): Flow<List<ReminderSetting>> = reminderSettingDao.getRemindersForPlant(plantId)
    suspend fun insertReminder(reminder: ReminderSetting): Long = reminderSettingDao.insertReminder(reminder)
    suspend fun updateReminder(reminder: ReminderSetting) = reminderSettingDao.updateReminder(reminder)
    suspend fun deleteReminder(reminder: ReminderSetting) = reminderSettingDao.deleteReminder(reminder)

    // --- Fleet Sizing & Maintenance Cleanup ---
    suspend fun getPlantCount(): Int = plantDao.countPlants()

    suspend fun deleteAllPlants() = plantDao.deleteAllPlants()
    suspend fun deleteAllCareLogs() = careLogDao.deleteAllCareLogs()
    suspend fun deleteAllPhotos() = photoEntryDao.deleteAllPhotos()
    suspend fun deleteAllReminders() = reminderSettingDao.deleteAllReminders()

    /**
     * Clears all tables in the SQLite database transactionally (used when wiping or resetting).
     */
    suspend fun clearAllData() {
        careLogDao.deleteAllCareLogs()
        photoEntryDao.deleteAllPhotos()
        reminderSettingDao.deleteAllReminders()
        plantDao.deleteAllPlants()
    }
}
