package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.model.CareLog
import com.example.data.model.PhotoEntry
import com.example.data.model.Plant
import com.example.data.model.ReminderSetting
import kotlinx.coroutines.flow.Flow

/**
 * PlantRepository serves as the unified multiplatform data access gateway for PlantHub.
 *
 * Responsibilities:
 * - Mediates between platform presenters/ViewModels and Room KMP DAOs (Plant, CareLog, PhotoEntry, ReminderSetting).
 * - Exposes hot reactive [Flow] streams for real-time Compose Multiplatform UI updates.
 * - Provides asynchronous coroutine [suspend] methods for single-shot reads and transactional writes.
 */
class PlantRepository(private val database: AppDatabase) {
    private val plantDao = database.plantDao()
    private val careLogDao = database.careLogDao()
    private val photoEntryDao = database.photoEntryDao()
    private val reminderSettingDao = database.reminderSettingDao()

    val allPlants: Flow<List<Plant>> = plantDao.getAllPlants()
    val allCareLogs: Flow<List<CareLog>> = careLogDao.getAllCareLogs()
    val allPhotos: Flow<List<PhotoEntry>> = photoEntryDao.getAllPhotos()
    val allActiveReminders: Flow<List<ReminderSetting>> = reminderSettingDao.getAllActiveReminders()

    fun getPlant(id: Long): Flow<Plant?> = plantDao.getPlantById(id)
    suspend fun getPlantOnce(id: Long): Plant? = plantDao.getPlantByIdOnce(id)
    suspend fun getAllPlantsOnce(): List<Plant> = plantDao.getAllPlantsOnce()
    suspend fun getAllCareLogsOnce(): List<CareLog> = careLogDao.getAllCareLogsOnce()
    suspend fun getAllPhotosOnce(): List<PhotoEntry> = photoEntryDao.getAllPhotosOnce()
    suspend fun getAllRemindersOnce(): List<ReminderSetting> = reminderSettingDao.getAllRemindersOnce()

    suspend fun insertPlant(plant: Plant): Long = plantDao.insertPlant(plant)
    suspend fun insertPlants(plants: List<Plant>): List<Long> = plantDao.insertPlants(plants)
    suspend fun updatePlant(plant: Plant) = plantDao.updatePlant(plant)
    suspend fun deletePlant(plant: Plant) = plantDao.deletePlant(plant)

    fun getLogsForPlant(plantId: Long): Flow<List<CareLog>> = careLogDao.getLogsForPlant(plantId)
    suspend fun getLogsForPlantOnce(plantId: Long): List<CareLog> = careLogDao.getLogsForPlantOnce(plantId)
    suspend fun insertCareLog(log: CareLog): Long = careLogDao.insertLog(log)
    suspend fun insertCareLogs(logs: List<CareLog>) = careLogDao.insertLogs(logs)
    suspend fun deleteCareLog(log: CareLog) = careLogDao.deleteLog(log)
    fun getLatestLogForPlant(plantId: Long): Flow<CareLog?> = careLogDao.getLatestLogForPlant(plantId)

    fun getPhotosForPlant(plantId: Long): Flow<List<PhotoEntry>> = photoEntryDao.getPhotosForPlant(plantId)
    suspend fun getPhotosForPlantOnce(plantId: Long): List<PhotoEntry> = photoEntryDao.getPhotosForPlantOnce(plantId)
    suspend fun insertPhotoEntry(entry: PhotoEntry): Long = photoEntryDao.insertPhoto(entry)
    suspend fun insertPhotos(entries: List<PhotoEntry>) = photoEntryDao.insertPhotos(entries)
    suspend fun updatePhotoEntry(entry: PhotoEntry) = photoEntryDao.updatePhoto(entry)
    suspend fun deletePhotoEntry(entry: PhotoEntry) = photoEntryDao.deletePhoto(entry)

    fun getRemindersForPlant(plantId: Long): Flow<List<ReminderSetting>> = reminderSettingDao.getRemindersForPlant(plantId)
    suspend fun insertReminder(reminder: ReminderSetting): Long = reminderSettingDao.insertReminder(reminder)
    suspend fun insertReminders(reminders: List<ReminderSetting>) = reminderSettingDao.insertReminders(reminders)
    suspend fun updateReminder(reminder: ReminderSetting) = reminderSettingDao.updateReminder(reminder)
    suspend fun deleteReminder(reminder: ReminderSetting) = reminderSettingDao.deleteReminder(reminder)

    suspend fun getPlantCount(): Int = plantDao.countPlants()

    suspend fun deleteAllPlants() = plantDao.deleteAllPlants()
    suspend fun deleteAllCareLogs() = careLogDao.deleteAllCareLogs()
    suspend fun deleteAllPhotos() = photoEntryDao.deleteAllPhotos()
    suspend fun deleteAllReminders() = reminderSettingDao.deleteAllReminders()

    /**
     * Clears all tables in the SQLite database (respecting child-before-parent order).
     */
    suspend fun clearAllData() {
        careLogDao.deleteAllCareLogs()
        photoEntryDao.deleteAllPhotos()
        reminderSettingDao.deleteAllReminders()
        plantDao.deleteAllPlants()
    }
}
