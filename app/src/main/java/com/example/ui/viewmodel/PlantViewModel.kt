package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.CareActionType
import com.example.data.model.CareLog
import com.example.data.model.GrowthStage
import com.example.data.model.LightCycleStatus
import com.example.data.model.PhotoEntry
import com.example.data.model.Plant
import com.example.data.repository.PlantRepository
import com.example.util.HeatmapCalculator
import com.example.util.HeatmapGridData
import com.example.util.LightCycleEngine
import com.example.util.NotificationHelper
import com.example.service.LightCycleTickerService
import com.example.util.PlantBackupManager
import com.example.util.SeedData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class NavTab(val title: String) {
    HOME("Plants"),
    DETAIL("Tracker"),
    VAULT("Vault"),
    SETTINGS("Settings")
}

enum class ThemeMode {
    SYSTEM, DARK, LIGHT
}

enum class HeatmapViewMode {
    WEEKS_16, YEARLY, MONTHLY
}

class PlantViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PlantRepository = PlantRepository(AppDatabase.getDatabase(application))

    val allPlants: StateFlow<List<Plant>> = repository.allPlants
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCareLogs: StateFlow<List<CareLog>> = repository.allCareLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPhotos: StateFlow<List<PhotoEntry>> = repository.allPhotos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedPlantId = MutableStateFlow<Long?>(null)
    val selectedPlantId: StateFlow<Long?> = _selectedPlantId.asStateFlow()

    private val _currentTab = MutableStateFlow(NavTab.HOME)
    val currentTab: StateFlow<NavTab> = _currentTab.asStateFlow()

    private val prefs = application.getSharedPreferences("planthub_prefs", Context.MODE_PRIVATE)

    private val initialThemeMode: ThemeMode = try {
        val saved = prefs.getString("key_theme_mode", ThemeMode.DARK.name)
        ThemeMode.valueOf(saved ?: ThemeMode.DARK.name)
    } catch (_: Exception) {
        ThemeMode.DARK
    }
    private val _themeMode = MutableStateFlow(initialThemeMode)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val initialDynamicColor = prefs.getBoolean("key_dynamic_color", false)
    private val _dynamicColor = MutableStateFlow(initialDynamicColor)
    val dynamicColor: StateFlow<Boolean> = _dynamicColor.asStateFlow()

    private val initialCareReminders = prefs.getBoolean("key_care_reminders_enabled", true)
    private val _careRemindersEnabled = MutableStateFlow(initialCareReminders)
    val careRemindersEnabled: StateFlow<Boolean> = _careRemindersEnabled.asStateFlow()

    private val initialTimelineShowOnlyToday = prefs.getBoolean("key_timeline_show_only_today", true)
    private val _timelineShowOnlyToday = MutableStateFlow(initialTimelineShowOnlyToday)
    val timelineShowOnlyToday: StateFlow<Boolean> = _timelineShowOnlyToday.asStateFlow()

    private val initialVaultAnalyticsExpanded = prefs.getBoolean("key_vault_analytics_expanded", true)
    private val _vaultAnalyticsExpanded = MutableStateFlow(initialVaultAnalyticsExpanded)
    val vaultAnalyticsExpanded: StateFlow<Boolean> = _vaultAnalyticsExpanded.asStateFlow()

    private val initialChangelogExpanded = prefs.getBoolean("key_settings_changelog_expanded", false)
    private val _settingsChangelogExpanded = MutableStateFlow(initialChangelogExpanded)
    val settingsChangelogExpanded: StateFlow<Boolean> = _settingsChangelogExpanded.asStateFlow()

    private val initialPrivacyExpanded = prefs.getBoolean("key_settings_privacy_expanded", false)
    private val _settingsPrivacyExpanded = MutableStateFlow(initialPrivacyExpanded)
    val settingsPrivacyExpanded: StateFlow<Boolean> = _settingsPrivacyExpanded.asStateFlow()

    // Live Timer Status Bar Chip user preference (persisted)
    private val initialLiveTimerChip = prefs.getBoolean("key_live_timer_chip_enabled", true)
    private val _liveTimerChipEnabled = MutableStateFlow(initialLiveTimerChip)
    val liveTimerChipEnabled: StateFlow<Boolean> = _liveTimerChipEnabled.asStateFlow()

    // Automated weekly database backups to user-defined folder
    private val initialWeeklyBackup = prefs.getBoolean("key_weekly_backup_enabled", false)
    private val _weeklyBackupEnabled = MutableStateFlow(initialWeeklyBackup)
    val weeklyBackupEnabled: StateFlow<Boolean> = _weeklyBackupEnabled.asStateFlow()

    private val _backupFolderUri = MutableStateFlow<String?>(prefs.getString("key_backup_folder_uri", null))
    val backupFolderUri: StateFlow<String?> = _backupFolderUri.asStateFlow()

    private val _backupFolderName = MutableStateFlow<String?>(prefs.getString("key_backup_folder_name", null))
    val backupFolderName: StateFlow<String?> = _backupFolderName.asStateFlow()

    private val _lastBackupTimestamp = MutableStateFlow(prefs.getLong("key_last_backup_timestamp", 0L))
    val lastBackupTimestamp: StateFlow<Long> = _lastBackupTimestamp.asStateFlow()

    private val _heatmapViewMode = MutableStateFlow(HeatmapViewMode.WEEKS_16)
    val heatmapViewMode: StateFlow<HeatmapViewMode> = _heatmapViewMode.asStateFlow()

    // Clock ticker to update photoperiod real-time timer
    private val _clockTicker = MutableStateFlow(System.currentTimeMillis())

    val selectedPlant: StateFlow<Plant?> = combine(allPlants, _selectedPlantId) { plants, id ->
        if (id != null) plants.find { it.id == id } else plants.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedPlantLogs: StateFlow<List<CareLog>> = combine(allCareLogs, selectedPlant) { logs, plant ->
        if (plant == null) emptyList() else logs.filter { it.plantId == plant.id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedPlantPhotos: StateFlow<List<PhotoEntry>> = combine(allPhotos, selectedPlant) { photos, plant ->
        if (plant == null) emptyList() else photos.filter { it.plantId == plant.id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentLightStatus: StateFlow<LightCycleStatus?> = combine(selectedPlant, _clockTicker) { plant, _ ->
        plant?.let { LightCycleEngine.calculateStatus(it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedPlantHeatmapData: StateFlow<HeatmapGridData> = combine(selectedPlantLogs, _heatmapViewMode) { logs, mode ->
        when (mode) {
            HeatmapViewMode.WEEKS_16 -> HeatmapCalculator.calculate16Weeks(logs)
            HeatmapViewMode.YEARLY -> HeatmapCalculator.calculateYearly(logs)
            HeatmapViewMode.MONTHLY -> HeatmapCalculator.calculate16Weeks(logs)
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        HeatmapCalculator.calculate16Weeks(emptyList())
    )

    val fleetHeatmapData: StateFlow<HeatmapGridData> = combine(allCareLogs, _heatmapViewMode) { logs, _ ->
        HeatmapCalculator.calculate16Weeks(logs)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        HeatmapCalculator.calculate16Weeks(emptyList())
    )

    private val _hasSeenOnboarding = MutableStateFlow(prefs.getBoolean("has_seen_onboarding_v1_7", false))
    val hasSeenOnboarding: StateFlow<Boolean> = _hasSeenOnboarding.asStateFlow()

    private val _requestOpenAddPlant = MutableStateFlow(false)
    val requestOpenAddPlant: StateFlow<Boolean> = _requestOpenAddPlant.asStateFlow()

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean("has_seen_onboarding_v1_7", completed).apply()
        _hasSeenOnboarding.value = completed
    }

    fun triggerAddPlantDialog() {
        _requestOpenAddPlant.value = true
    }

    fun consumeAddPlantDialog() {
        _requestOpenAddPlant.value = false
    }

    init {
        // Persistent cultivar lifecycle preservation across app updates
        viewModelScope.launch {
            val count = repository.getPlantCount()
            if (count == 0) {
                // If the user previously had custom plants or updated the app, restore from backup
                if (PlantBackupManager.hasSavedBackup(application)) {
                    PlantBackupManager.restoreBackup(application, repository)
                }
            }
        }

        // Auto-save backup continuously whenever plants, logs, or photos change
        viewModelScope.launch {
            combine(allPlants, allCareLogs, allPhotos) { p, l, ph ->
                Triple(p, l, ph)
            }.collect { (plants, logs, photos) ->
                if (plants.isNotEmpty()) {
                    PlantBackupManager.saveBackup(application, plants, logs, photos)
                }
            }
        }

        // Timer loop to update photoperiod countdown every second and check cycle maintenance completion
        viewModelScope.launch {
            var tickCount = 0
            var wasLiveNotificationActive = false
            var lastLiveNotifyPhase = ""
            var lastLiveNotifyMinute = -1

            while (isActive) {
                delay(1000)
                _clockTicker.value = System.currentTimeMillis()
                tickCount++

                // Every 3 seconds, evaluate if any actively running plant reached the end of its off-cycle and sync Live Update
                if (tickCount % 3 == 0) {
                    val currentPlants = repository.getAllPlantsOnce()
                    val now = System.currentTimeMillis()
                    for (plant in currentPlants) {
                        if (plant.isTimerRunning && plant.timerStartedAt != null) {
                            val totalSec = (plant.lightOnHours + plant.lightOffHours) * 3600L
                            if (totalSec > 0) {
                                val elapsedSec = (now - plant.timerStartedAt) / 1000L
                                if (elapsedSec >= totalSec) {
                                    // Full cycle is complete! Automatically pause for plant maintenance
                                    val pausedPlant = plant.copy(
                                        isTimerRunning = false,
                                        isPausedForMaintenance = true,
                                        timerStartedAt = null,
                                        timerPausedRemainingSec = null
                                    )
                                    repository.updatePlant(pausedPlant)
                                    NotificationHelper.sendLightMaintenanceAlert(
                                        context = application,
                                        notificationId = (2000 + plant.id).toInt(),
                                        plantName = plant.name,
                                        strain = plant.strain
                                    )
                                }
                            }
                        }
                    }

                    // Synchronize Android 16 Live Update Notification & Status Bar Chip
                    // Respect user's toggle setting: if disabled, do not show and cancel if active
                    val isLiveEnabled = _liveTimerChipEnabled.value
                    val activeRunningPlant = currentPlants.firstOrNull { it.id == _selectedPlantId.value && it.isTimerRunning && !it.isPausedForMaintenance }
                        ?: currentPlants.firstOrNull { it.isTimerRunning && !it.isPausedForMaintenance }

                    if (activeRunningPlant != null && isLiveEnabled) {
                        val status = LightCycleEngine.calculateStatus(activeRunningPlant)
                        val currentPhase = status.currentPhaseName

                        val currentMinute = (status.hoursRemainingInPhase * 60 + status.minutesRemainingInPhase).toInt()

                        // Ensure continuous background ticker service is active
                        if (!wasLiveNotificationActive) {
                            LightCycleTickerService.start(application, activeRunningPlant.id)
                        }

                        // Update when timer starts/resumes, when phase changes (e.g. ON -> OFF),
                        // or every minute to keep progress bar and status chip countdown text in sync
                        if (!wasLiveNotificationActive || currentPhase != lastLiveNotifyPhase || currentMinute != lastLiveNotifyMinute) {
                            NotificationHelper.updateLightCycleLiveUpdate(
                                context = application,
                                status = status,
                                plantName = activeRunningPlant.name,
                                plantId = activeRunningPlant.id
                            )
                            wasLiveNotificationActive = true
                            lastLiveNotifyPhase = currentPhase
                            lastLiveNotifyMinute = currentMinute
                        }
                    } else if (wasLiveNotificationActive) {
                        LightCycleTickerService.stop(application)
                        NotificationHelper.cancelLightCycleLiveUpdate(application)
                        wasLiveNotificationActive = false
                        lastLiveNotifyPhase = ""
                        lastLiveNotifyMinute = -1
                    }
                }

                // Check automated weekly backup schedule every 60 seconds
                if (tickCount % 60 == 0) {
                    checkAndTriggerAutomatedWeeklyBackup()
                }
            }
        }
    }

    fun selectTab(tab: NavTab) {
        _currentTab.value = tab
    }

    fun selectPlant(plantId: Long) {
        _selectedPlantId.value = plantId
    }

    fun setHeatmapViewMode(mode: HeatmapViewMode) {
        _heatmapViewMode.value = mode
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString("key_theme_mode", mode.name).apply()
    }

    fun setDynamicColor(enabled: Boolean) {
        _dynamicColor.value = enabled
        prefs.edit().putBoolean("key_dynamic_color", enabled).apply()
    }

    fun quickWater(plantId: Long, amountMl: Int = 500) {
        viewModelScope.launch {
            val log = CareLog(
                plantId = plantId,
                timestamp = System.currentTimeMillis(),
                actionType = CareActionType.WATER,
                waterAmountMl = amountMl,
                notes = "Quick water +$amountMl mL"
            )
            repository.insertCareLog(log)
        }
    }

    fun quickMist(plantId: Long) {
        viewModelScope.launch {
            val log = CareLog(
                plantId = plantId,
                timestamp = System.currentTimeMillis(),
                actionType = CareActionType.MIST,
                notes = "Foliar misting applied"
            )
            repository.insertCareLog(log)
        }
    }

    fun quickFertilize(plantId: Long, name: String = "Balanced Organic NPK", dose: String = "2mL/L") {
        viewModelScope.launch {
            val log = CareLog(
                plantId = plantId,
                timestamp = System.currentTimeMillis(),
                actionType = CareActionType.FERTILIZE,
                fertilizerName = name,
                fertilizerDose = dose,
                notes = "Routine feeding cycle"
            )
            repository.insertCareLog(log)
        }
    }

    fun addCareLog(
        plantId: Long,
        actionType: CareActionType,
        waterAmountMl: Int?,
        phLevel: Double?,
        fertilizerName: String?,
        fertilizerDose: String?,
        ecPpm: Int?,
        notes: String
    ) {
        viewModelScope.launch {
            val log = CareLog(
                plantId = plantId,
                timestamp = System.currentTimeMillis(),
                actionType = actionType,
                waterAmountMl = waterAmountMl,
                phLevel = phLevel,
                fertilizerName = fertilizerName,
                fertilizerDose = fertilizerDose,
                ecPpm = ecPpm,
                notes = notes
            )
            repository.insertCareLog(log)
        }
    }

    fun addPlant(
        name: String,
        strain: String,
        stage: GrowthStage,
        preset: String,
        onHours: Int,
        offHours: Int,
        startMin: Int,
        medium: String,
        potLiters: Double,
        notes: String,
        iconKey: String = "cannabis"
    ) {
        viewModelScope.launch {
            val plant = Plant(
                name = name,
                strain = strain,
                stage = stage,
                lightCyclePreset = preset,
                lightOnHours = onHours,
                lightOffHours = offHours,
                lightStartTimeMinute = startMin,
                medium = medium,
                potSizeLiters = potLiters,
                plantedDate = System.currentTimeMillis(),
                notes = notes,
                iconKey = iconKey
            )
            val newId = repository.insertPlant(plant)
            _selectedPlantId.value = newId
        }
    }

    fun updatePlant(plant: Plant) {
        viewModelScope.launch {
            repository.updatePlant(plant)
        }
    }

    fun deletePlant(plant: Plant) {
        viewModelScope.launch {
            repository.deletePlant(plant)
            if (_selectedPlantId.value == plant.id) {
                _selectedPlantId.value = null
            }
        }
    }

    fun updatePlantStage(plantId: Long, stage: GrowthStage) {
        viewModelScope.launch {
            val plant = repository.getPlantOnce(plantId) ?: return@launch
            val updated = plant.copy(stage = stage)
            repository.updatePlant(updated)
        }
    }

    fun startLightTimer(plantId: Long) {
        viewModelScope.launch {
            val plant = repository.getPlantOnce(plantId) ?: return@launch
            val now = System.currentTimeMillis()
            val adjustedStartedAt = if (plant.timerPausedRemainingSec != null && !plant.isPausedForMaintenance) {
                val totalOnSec = plant.lightOnHours * 3600L
                val elapsedSec = (totalOnSec - plant.timerPausedRemainingSec).coerceAtLeast(0L)
                now - (elapsedSec * 1000L)
            } else {
                now
            }
            val updated = plant.copy(
                isTimerRunning = true,
                timerStartedAt = adjustedStartedAt,
                timerPausedRemainingSec = null,
                isPausedForMaintenance = false
            )
            repository.updatePlant(updated)
            if (_liveTimerChipEnabled.value) {
                LightCycleTickerService.start(getApplication(), updated.id)
            }
        }
    }

    fun resumeFromMaintenance(plantId: Long) {
        startLightTimer(plantId)
    }

    fun pauseLightTimer(plantId: Long) {
        viewModelScope.launch {
            val plant = repository.getPlantOnce(plantId) ?: return@launch
            val status = LightCycleEngine.calculateStatus(plant)
            val totalRemainingSec = status.hoursRemainingInPhase * 3600L + status.minutesRemainingInPhase * 60L + status.secondsRemainingInPhase
            val updated = plant.copy(
                isTimerRunning = false,
                timerPausedRemainingSec = totalRemainingSec,
                isPausedForMaintenance = false
            )
            repository.updatePlant(updated)
            LightCycleTickerService.stop(getApplication())
            NotificationHelper.cancelLightCycleLiveUpdate(getApplication())
        }
    }

    fun resetLightTimer(plantId: Long) {
        viewModelScope.launch {
            val plant = repository.getPlantOnce(plantId) ?: return@launch
            val updated = plant.copy(
                isTimerRunning = false,
                timerStartedAt = null,
                timerPausedRemainingSec = null,
                isPausedForMaintenance = false
            )
            repository.updatePlant(updated)
            LightCycleTickerService.stop(getApplication())
            NotificationHelper.cancelLightCycleLiveUpdate(getApplication())
        }
    }

    fun setCareRemindersEnabled(enabled: Boolean) {
        _careRemindersEnabled.value = enabled
        prefs.edit().putBoolean("key_care_reminders_enabled", enabled).apply()
    }

    fun setTimelineShowOnlyToday(showOnlyToday: Boolean) {
        _timelineShowOnlyToday.value = showOnlyToday
        prefs.edit().putBoolean("key_timeline_show_only_today", showOnlyToday).apply()
    }

    fun setVaultAnalyticsExpanded(expanded: Boolean) {
        _vaultAnalyticsExpanded.value = expanded
        prefs.edit().putBoolean("key_vault_analytics_expanded", expanded).apply()
    }

    fun setSettingsChangelogExpanded(expanded: Boolean) {
        _settingsChangelogExpanded.value = expanded
        prefs.edit().putBoolean("key_settings_changelog_expanded", expanded).apply()
    }

    fun setSettingsPrivacyExpanded(expanded: Boolean) {
        _settingsPrivacyExpanded.value = expanded
        prefs.edit().putBoolean("key_settings_privacy_expanded", expanded).apply()
    }

    fun checkAndTriggerWateringReminders() {
        viewModelScope.launch {
            if (!_careRemindersEnabled.value) return@launch
            val plantsList = repository.getAllPlantsOnce()
            val logsList = repository.getAllCareLogsOnce()
            val now = System.currentTimeMillis()
            val twoDaysMillis = 48 * 3600 * 1000L

            for (plant in plantsList) {
                val plantWaterLogs = logsList.filter { 
                    it.plantId == plant.id && (it.actionType == CareActionType.WATER || it.actionType == CareActionType.FERTILIZE)
                }
                val lastWaterTime = plantWaterLogs.maxOfOrNull { it.timestamp } ?: plant.plantedDate
                if (now - lastWaterTime >= twoDaysMillis) {
                    NotificationHelper.sendWateringReminder(
                        context = getApplication(),
                        notificationId = (3000 + plant.id).toInt(),
                        plantName = plant.name,
                        strain = plant.strain
                    )
                }
            }
        }
    }

    fun setLiveTimerChipEnabled(enabled: Boolean) {
        _liveTimerChipEnabled.value = enabled
        prefs.edit().putBoolean("key_live_timer_chip_enabled", enabled).apply()
        if (!enabled) {
            LightCycleTickerService.stop(getApplication())
            NotificationHelper.cancelLightCycleLiveUpdate(getApplication())
        } else {
            val plant = selectedPlant.value
            if (plant != null && plant.isTimerRunning && !plant.isPausedForMaintenance) {
                LightCycleTickerService.start(getApplication(), plant.id)
                val status = LightCycleEngine.calculateStatus(plant)
                NotificationHelper.updateLightCycleLiveUpdate(
                    context = getApplication(),
                    status = status,
                    plantName = plant.name,
                    plantId = plant.id
                )
            }
        }
    }

    /**
     * Instantly refreshes the timer state against the system clock upon resuming the app.
     */
    fun refreshTimerState() {
        viewModelScope.launch {
            val plants = repository.getAllPlantsOnce()
            val activeRunning = plants.firstOrNull { it.id == _selectedPlantId.value && it.isTimerRunning && !it.isPausedForMaintenance }
                ?: plants.firstOrNull { it.isTimerRunning && !it.isPausedForMaintenance }
            if (activeRunning != null && _liveTimerChipEnabled.value) {
                val status = LightCycleEngine.calculateStatus(activeRunning)
                NotificationHelper.updateLightCycleLiveUpdate(
                    context = getApplication(),
                    status = status,
                    plantName = activeRunning.name,
                    plantId = activeRunning.id
                )
                LightCycleTickerService.start(getApplication(), activeRunning.id)
            }
        }
    }

    fun setWeeklyBackupEnabled(enabled: Boolean) {
        _weeklyBackupEnabled.value = enabled
        prefs.edit().putBoolean("key_weekly_backup_enabled", enabled).apply()
        if (enabled && _backupFolderUri.value != null && _lastBackupTimestamp.value == 0L) {
            // Run initial backup immediately if never run
            triggerBackupNow { _, _ -> }
        }
    }

    fun setBackupFolderUri(uri: Uri, displayName: String) {
        val uriStr = uri.toString()
        _backupFolderUri.value = uriStr
        _backupFolderName.value = displayName
        prefs.edit()
            .putString("key_backup_folder_uri", uriStr)
            .putString("key_backup_folder_name", displayName)
            .apply()
    }

    fun triggerBackupNow(onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val uriStr = _backupFolderUri.value
            if (uriStr.isNullOrBlank()) {
                onResult(false, null)
                return@launch
            }
            val treeUri = Uri.parse(uriStr)
            val (success, fileName) = PlantBackupManager.exportToFolderUri(getApplication(), treeUri, repository)
            if (success) {
                val now = System.currentTimeMillis()
                _lastBackupTimestamp.value = now
                prefs.edit().putLong("key_last_backup_timestamp", now).apply()
            }
            onResult(success, fileName)
        }
    }

    private fun checkAndTriggerAutomatedWeeklyBackup() {
        if (!_weeklyBackupEnabled.value) return
        val uriStr = _backupFolderUri.value ?: return
        val lastTime = _lastBackupTimestamp.value
        val sevenDaysMillis = 7 * 24 * 3600 * 1000L
        if (System.currentTimeMillis() - lastTime >= sevenDaysMillis) {
            viewModelScope.launch {
                val treeUri = Uri.parse(uriStr)
                val (success, _) = PlantBackupManager.exportToFolderUri(getApplication(), treeUri, repository)
                if (success) {
                    val now = System.currentTimeMillis()
                    _lastBackupTimestamp.value = now
                    prefs.edit().putLong("key_last_backup_timestamp", now).apply()
                }
            }
        }
    }

    fun calculateCalendarDay(plantedDate: Long, targetDate: Long = System.currentTimeMillis()): Int {
        val calStart = java.util.Calendar.getInstance().apply {
            timeInMillis = plantedDate
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val calEnd = java.util.Calendar.getInstance().apply {
            timeInMillis = targetDate
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val diffMillis = calEnd.timeInMillis - calStart.timeInMillis
        val days = (diffMillis / (1000 * 60 * 60 * 24)).toInt()
        return (days.coerceAtLeast(0) + 1)
    }

    fun exportDatabaseToUri(uri: Uri, isJson: Boolean, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = PlantBackupManager.exportToFileUri(getApplication(), uri, isJson, repository)
            onResult(success)
        }
    }

    fun exportCompleteZipBackup(uri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = PlantBackupManager.exportCompleteZipBackup(getApplication(), uri, repository)
            withContext(Dispatchers.Main) {
                onResult(success)
            }
        }
    }

    fun parseImportFile(
        uri: Uri,
        isJson: Boolean,
        onResult: (PlantBackupManager.ImportPreviewData?, String?) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val (data, error) = PlantBackupManager.parseImportFile(getApplication(), uri, isJson)
            withContext(Dispatchers.Main) {
                onResult(data, error)
            }
        }
    }

    fun restoreDatabaseFromImport(
        data: PlantBackupManager.ImportPreviewData,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val success = if (data.sourceFormat == "ZIP Bundle" && data.sourceUri != null) {
                PlantBackupManager.restoreDatabaseFromZip(getApplication(), repository, data, data.sourceUri)
            } else {
                PlantBackupManager.restoreDatabaseFromData(getApplication(), repository, data)
            }
            if (success) {
                val freshPlants = repository.getAllPlantsOnce()
                freshPlants.firstOrNull()?.let { selectPlant(it.id) }
                _timelineShowOnlyToday.value = false
                prefs.edit().putBoolean("key_timeline_show_only_today", false).apply()
                NotificationHelper.scheduleDailyCareAlerts(getApplication())
                val photoDetail = if (data.bundledImagesCount > 0) {
                    "${data.bundledImagesCount} full-resolution photos"
                } else {
                    "${data.photos.size} vault photos"
                }
                onResult(true, "Successfully restored ${data.plants.size} cultivars, ${data.logs.size} care logs, and $photoDetail.")
            } else {
                onResult(false, "Failed to restore database from backup file.")
            }
        }
    }

    fun addPhotoEntry(
        plantId: Long,
        imageUri: String,
        stage: GrowthStage,
        title: String,
        observation: String,
        canopyHeightCm: Double?,
        customDayNumber: Int? = null
    ) {
        viewModelScope.launch {
            val plant = repository.getPlantOnce(plantId)
            val dayNumber = customDayNumber ?: if (plant != null) {
                calculateCalendarDay(plant.plantedDate)
            } else 1

            val entry = PhotoEntry(
                plantId = plantId,
                timestamp = System.currentTimeMillis(),
                imageUri = imageUri,
                stage = stage,
                dayNumber = dayNumber,
                title = title.ifBlank { "Day $dayNumber Photo Log" },
                observation = observation,
                canopyHeightCm = canopyHeightCm
            )
            repository.insertPhotoEntry(entry)
        }
    }

    fun updatePhotoEntry(entry: PhotoEntry) {
        viewModelScope.launch {
            repository.updatePhotoEntry(entry)
        }
    }

    fun deletePhotoEntry(entry: PhotoEntry) {
        viewModelScope.launch {
            repository.deletePhotoEntry(entry)
        }
    }

    fun resetDatabaseWithSampleData() {
        viewModelScope.launch {
            val plants = repository.getAllPlantsOnce()
            plants.forEach { repository.deletePlant(it) }
            SeedData.populateIfEmpty(repository)
            delay(100)
            val freshPlants = repository.getAllPlantsOnce()
            val freshLogs = repository.getAllCareLogsOnce()
            val freshPhotos = repository.getAllPhotosOnce()
            PlantBackupManager.saveBackup(getApplication(), freshPlants, freshLogs, freshPhotos)
        }
    }

    fun triggerTestNotification(context: Context) {
        val currentPlant = selectedPlant.value ?: allPlants.value.firstOrNull()
        val name = currentPlant?.name ?: "Cultivar"
        NotificationHelper.sendCareAlert(
            context = context,
            notificationId = 101,
            plantName = name,
            actionType = "Watering",
            message = "Hydration window open (+500mL)"
        )
    }

    fun triggerTestMaintenanceAlert(context: Context) {
        val currentPlant = selectedPlant.value ?: allPlants.value.firstOrNull()
        val name = currentPlant?.name ?: "Cultivar"
        val strain = currentPlant?.strain ?: "Hybrid"
        NotificationHelper.sendLightMaintenanceAlert(
            context = context,
            notificationId = 102,
            plantName = name,
            strain = strain
        )
    }

    fun triggerTestWateringDueAlert(context: Context) {
        val currentPlant = selectedPlant.value ?: allPlants.value.firstOrNull()
        val name = currentPlant?.name ?: "Cultivar"
        val strain = currentPlant?.strain ?: "Indica Dominant"
        NotificationHelper.sendWateringReminder(
            context = context,
            notificationId = 103,
            plantName = name,
            strain = strain
        )
    }

    fun triggerTestLiveUpdateAlert(context: Context) {
        // Use currently selected plant, fallback to first active running plant, or first plant in list
        val targetPlant = selectedPlant.value
            ?: allPlants.value.firstOrNull { it.isTimerRunning && !it.isPausedForMaintenance }
            ?: allPlants.value.firstOrNull()

        if (targetPlant != null) {
            val status = LightCycleEngine.calculateStatus(targetPlant)
            val effectiveStatus = if (targetPlant.isTimerRunning && !targetPlant.isPausedForMaintenance && status.nextSwitchEpochMillis > 0) {
                status
            } else {
                val nextSwitchMillis = System.currentTimeMillis() + ((targetPlant.lightOnHours / 2).coerceAtLeast(1) * 3600_000L)
                status.copy(
                    isLightOn = true,
                    currentPhaseName = "LIGHTS ON",
                    hoursRemainingInPhase = (targetPlant.lightOnHours / 2).coerceAtLeast(1).toLong(),
                    minutesRemainingInPhase = 30L,
                    secondsRemainingInPhase = 0L,
                    phaseProgressPercent = 0.5f,
                    presetName = "${targetPlant.lightOnHours}/${targetPlant.lightOffHours} ${targetPlant.stage.label}",
                    isTimerRunning = true,
                    isWaitingToStart = false,
                    isPausedForMaintenance = false,
                    nextSwitchEpochMillis = nextSwitchMillis
                )
            }
            NotificationHelper.triggerTestLiveUpdateAlert(
                context = context,
                plantName = targetPlant.name,
                status = effectiveStatus,
                plantId = targetPlant.id
            )
        } else {
            NotificationHelper.triggerTestLiveUpdateAlert(context)
        }
    }
}
