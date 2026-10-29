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

/**
 * PlantViewModel is the central presentation and state management coordinator for PlantHub.
 *
 * Responsibilities:
 * - Exposes reactive UI [StateFlow] streams for plants, logs, photos, and lighting statuses.
 * - Manages cultivar lifecycle operations (create, edit, delete, stage transitions).
 * - Drives the real-time photoperiod calculation ticker and synchronizes Android 16 Live Updates.
 * - Orchestrates one-tap care logging (Water, Nutrients, Defoliation, LST, Flush, Misting).
 * - Manages the Milestone Photo Progression Vault and canopy growth analytics.
 * - Handles universal data backup and restore operations (Full ZIP bundles, multi-table CSV, and JSON).
 * - Persists and manages user preferences (theming, reminders, and automated weekly backups).
 */
class PlantViewModel(application: Application) : AndroidViewModel(application) {

    // Repository mediating access to on-device Room SQLite database
    private val repository: PlantRepository = PlantRepository(AppDatabase.getDatabase(application))

    // Reactive streams observing database changes
    val allPlants: StateFlow<List<Plant>> = repository.allPlants
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCareLogs: StateFlow<List<CareLog>> = repository.allCareLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPhotos: StateFlow<List<PhotoEntry>> = repository.allPhotos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Currently focused cultivar identifier
    private val _selectedPlantId = MutableStateFlow<Long?>(null)
    val selectedPlantId: StateFlow<Long?> = _selectedPlantId.asStateFlow()

    // Currently active bottom navigation tab
    private val _currentTab = MutableStateFlow(NavTab.HOME)
    val currentTab: StateFlow<NavTab> = _currentTab.asStateFlow()

    // Persistent on-device SharedPreferences for client preferences
    private val prefs = application.getSharedPreferences("planthub_prefs", Context.MODE_PRIVATE)

    // User selected theme mode (System, Dark, or Light)
    private val initialThemeMode: ThemeMode = try {
        val saved = prefs.getString("key_theme_mode", ThemeMode.DARK.name)
        ThemeMode.valueOf(saved ?: ThemeMode.DARK.name)
    } catch (_: Exception) {
        ThemeMode.DARK
    }
    private val _themeMode = MutableStateFlow(initialThemeMode)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    // Android 12+ Monet dynamic wallpaper color theming toggle
    private val initialDynamicColor = prefs.getBoolean("key_dynamic_color", false)
    private val _dynamicColor = MutableStateFlow(initialDynamicColor)
    val dynamicColor: StateFlow<Boolean> = _dynamicColor.asStateFlow()

    // Daily care reminder alarms toggle
    private val initialCareReminders = prefs.getBoolean("key_care_reminders_enabled", true)
    private val _careRemindersEnabled = MutableStateFlow(initialCareReminders)
    val careRemindersEnabled: StateFlow<Boolean> = _careRemindersEnabled.asStateFlow()

    // Timeline filtering preference (show only today vs. all historical care actions)
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

        // TECHNICAL LOGIC & ARCHITECTURAL DESIGN: Dual-Cadence Photoperiod Ticker
        // 1. One-Second UI Pulse (_clockTicker):
        //    Updates the in-memory StateFlow timestamp every 1000ms. UI Composables (TimerScreen and gauges)
        //    observe this ticker to compute smooth, jitter-free countdown text without writing to SQLite.
        // 2. Three-Second Evaluation Batching:
        //    Evaluating database state every second causes unnecessary SQLite I/O and battery drain.
        //    Instead, we inspect active cultivars every 3 seconds to check if a 24h photoperiod has concluded,
        //    transitioning the plant to `isPausedForMaintenance = true` and dispatching dark cycle alerts.
        viewModelScope.launch {
            var tickCount = 0
            var wasLiveNotificationActive = false
            var lastLiveNotifyPhase = ""
            var lastLiveNotifyMinute = -1

            while (isActive) {
                delay(1000)
                _clockTicker.value = System.currentTimeMillis()
                tickCount++

                // Batch check: evaluate 24-hour cycle completion and live notification sync every 3 seconds
                if (tickCount % 3 == 0) {
                    val currentPlants = repository.getAllPlantsOnce()
                    val now = System.currentTimeMillis()
                    for (plant in currentPlants) {
                        if (plant.isTimerRunning && plant.timerStartedAt != null) {
                            val totalSec = (plant.lightOnHours + plant.lightOffHours) * 3600L
                            if (totalSec > 0) {
                                val elapsedSec = (now - plant.timerStartedAt) / 1000L
                                if (elapsedSec >= totalSec) {
                                    // DESIGN DECISION: Safety Lockout on Cycle Completion
                                    // Cultivar has completed both ON and OFF durations. Set `isPausedForMaintenance = true`
                                    // so lights do not unexpectedly turn on while tent is sealed or dark.
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

    /**
     * Records a one-tap quick watering event for the specified plant.
     */
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

    /**
     * Records a one-tap foliar misting event for canopy humidity maintenance.
     */
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

    /**
     * Records a standard routine organic/mineral nutrient feeding event.
     */
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

    /**
     * Records a comprehensive care log with telemetry (pH, EC/PPM, nutrients, and notes).
     */
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

    /**
     * Creates and persists a new cultivar profile in local SQLite storage.
     */
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

    /**
     * Updates an existing cultivar profile with modified parameters.
     */
    fun updatePlant(plant: Plant) {
        viewModelScope.launch {
            repository.updatePlant(plant)
        }
    }

    /**
     * Deletes a plant and deselects it if currently active.
     */
    fun deletePlant(plant: Plant) {
        viewModelScope.launch {
            repository.deletePlant(plant)
            if (_selectedPlantId.value == plant.id) {
                _selectedPlantId.value = null
            }
        }
    }

    /**
     * Updates the growth stage for a cultivar (e.g. Vegetative to Flowering).
     */
    fun updatePlantStage(plantId: Long, stage: GrowthStage) {
        viewModelScope.launch {
            val plant = repository.getPlantOnce(plantId) ?: return@launch
            val updated = plant.copy(stage = stage)
            repository.updatePlant(updated)
        }
    }

    /**
     * Starts or resumes the photoperiod light timer for a cultivar.
     *
     * TECHNICAL LOGIC: Clock-Drift Prevention
     * When resuming from a paused state ([timerPausedRemainingSec] != null):
     * - We derive `elapsedSec = totalOnSec - timerPausedRemainingSec`.
     * - We compute `adjustedStartedAt = now - (elapsedSec * 1000L)`.
     * - By shifting the simulated start epoch into the past by the previously elapsed duration,
     *   the timer resumes seamlessly with ZERO clock drift and exact sub-second precision.
     * Also initiates the foreground ticker service when live updates are enabled.
     */
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

    /**
     * Resumes timer from cycle-complete maintenance mode.
     */
    fun resumeFromMaintenance(plantId: Long) {
        startLightTimer(plantId)
    }

    /**
     * Pauses the photoperiod timer.
     *
     * DESIGN DECISION: Preserving Exact Countdown State Across Process Restarts
     * - Captures remaining seconds in active phase into [timerPausedRemainingSec].
     * - Nullifies active execution so the UI renders the frozen gauge.
     * - Stops the live notification ticker service to prevent background battery draw while paused.
     */
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

    /**
     * Resets the photoperiod timer back to unstarted standby state ("Waiting to Start").
     * Clears all start epoch timestamps and pause buffers, canceling active live notifications.
     */
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

    /**
     * Evaluates root zone dry-back intervals and dispatches watering reminders.
     *
     * TECHNICAL LOGIC:
     * - Filters care logs for previous hydration events ([CareActionType.WATER] or [CareActionType.FERTILIZE]).
     * - If elapsed duration since last watering exceeds the 48-hour agronomic threshold (or 48h since planting
     *   if never watered), dispatches a localized notification to prevent root desiccation.
     */
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

    /**
     * Computes the 1-indexed biological calendar day for a cultivar from its planted timestamp.
     */
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

    /**
     * Exports SQLite database tables to a user-selected file URI in JSON or CSV format.
     */
    fun exportDatabaseToUri(uri: Uri, isJson: Boolean, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = PlantBackupManager.exportToFileUri(getApplication(), uri, isJson, repository)
            onResult(success)
        }
    }

    /**
     * Packages the complete SQLite database and all original-resolution milestone photos into a portable .zip bundle.
     */
    fun exportCompleteZipBackup(uri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = PlantBackupManager.exportCompleteZipBackup(getApplication(), uri, repository)
            withContext(Dispatchers.Main) {
                onResult(success)
            }
        }
    }

    /**
     * Parses and inspects an external backup file (ZIP, CSV, or JSON) to generate an import preview.
     */
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

    /**
     * Restores the complete application state from an imported backup dataset.
     * Supports both portable ZIP archives (containing images and metadata) and raw JSON/CSV imports.
     * Resets local preferences, reactivates the primary plant, and reschedules system alarms.
     *
     * @param data Parsed backup payload containing plants, care logs, and photo vault entries.
     * @param onResult Callback delivering success flag and localized summary status message.
     */
    fun restoreDatabaseFromImport(
        data: PlantBackupManager.ImportPreviewData,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            // Unpack ZIP archive or insert parsed entities into Room tables
            val success = if (data.sourceFormat == "ZIP Bundle" && data.sourceUri != null) {
                PlantBackupManager.restoreDatabaseFromZip(getApplication(), repository, data, data.sourceUri)
            } else {
                PlantBackupManager.restoreDatabaseFromData(getApplication(), repository, data)
            }
            if (success) {
                // Refresh local state: select the first restored cultivar and reset timeline filter
                val freshPlants = repository.getAllPlantsOnce()
                freshPlants.firstOrNull()?.let { selectPlant(it.id) }
                _timelineShowOnlyToday.value = false
                prefs.edit().putBoolean("key_timeline_show_only_today", false).apply()

                // Re-arm local Android alarms for watering and light cycles
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

    /**
     * Persists a visual milestone entry with associated botanical stage, canopy height, and notes.
     * Automatically calculates the calendar day number relative to the plant's inception if not specified.
     *
     * @param plantId Unique database identifier of the cultivar.
     * @param imageUri File URI pointing to the sandbox-stored photo asset.
     * @param stage Phenological growth stage active when photo was captured.
     * @param title Short descriptive headline for the gallery card.
     * @param observation Qualitative agronomic notes (e.g. trichome coloration, pest check).
     * @param canopyHeightCm Measured vertical height of the apex canopy in centimeters.
     * @param customDayNumber Optional overridden day count; defaults to elapsed days since planting.
     */
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

    /**
     * Updates metadata or notes on an existing photo vault record.
     */
    fun updatePhotoEntry(entry: PhotoEntry) {
        viewModelScope.launch {
            repository.updatePhotoEntry(entry)
        }
    }

    /**
     * Permanently deletes a photo vault entry from the local database.
     */
    fun deletePhotoEntry(entry: PhotoEntry) {
        viewModelScope.launch {
            repository.deletePhotoEntry(entry)
        }
    }

    /**
     * Resets the entire Room database with fresh starter cultivars, logs, and photoperiod schedules.
     * Clears all existing user records and writes an initial offline backup snapshot.
     */
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

    /**
     * Dispatches an immediate test care notification for diagnostic and notification channel verification.
     */
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

    /**
     * Dispatches a test maintenance alert warning the grower of active dark cycle or timer maintenance.
     */
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

    /**
     * Dispatches a test watering reminder alert notifying the grower that root zone dry-back has completed.
     */
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

    /**
     * Tests Android 16 ongoing Live Update notification capabilities with simulated or live photoperiod progress.
     */
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
