package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.getDatabase
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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// NavTab, ThemeMode, and HeatmapViewMode are shared across KMP targets in com.example.ui.viewmodel.SharedEnums.kt

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
@OptIn(FlowPreview::class)
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

    // Key log/photo filtering strictly on the active plant ID so timer updates do not refilter or rebuild heatmaps
    private val selectedPlantIdFlow = selectedPlant
        .map { it?.id }
        .distinctUntilChanged()

    val selectedPlantLogs: StateFlow<List<CareLog>> = combine(allCareLogs, selectedPlantIdFlow) { logs, plantId ->
        if (plantId == null) emptyList() else logs.filter { it.plantId == plantId }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedPlantPhotos: StateFlow<List<PhotoEntry>> = combine(allPhotos, selectedPlantIdFlow) { photos, plantId ->
        if (plantId == null) emptyList() else photos.filter { it.plantId == plantId }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentLightStatus: StateFlow<LightCycleStatus?> = combine(selectedPlant, _clockTicker) { plant, _ ->
        plant?.let { LightCycleEngine.calculateStatus(it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedPlantHeatmapData: StateFlow<HeatmapGridData> = combine(selectedPlantLogs, _heatmapViewMode) { logs, mode ->
        when (mode) {
            HeatmapViewMode.WEEKS_16 -> HeatmapCalculator.calculate16Weeks(logs)
            HeatmapViewMode.YEARLY -> HeatmapCalculator.calculateYearly(logs)
            HeatmapViewMode.MONTHLY -> HeatmapCalculator.calculate16Weeks(logs)
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            HeatmapCalculator.calculate16Weeks(emptyList())
        )

    val fleetHeatmapData: StateFlow<HeatmapGridData> = allCareLogs
        .map { logs -> HeatmapCalculator.calculate16Weeks(logs) }
        .flowOn(Dispatchers.Default)
        .stateIn(
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
        // Persistent cultivar lifecycle preservation across app updates (offloaded to IO)
        viewModelScope.launch(Dispatchers.IO) {
            val count = repository.getPlantCount()
            if (count == 0 && PlantBackupManager.hasSavedBackup(application)) {
                PlantBackupManager.restoreBackup(application, repository)
            }
        }

        // Debounced background auto-save backup so rapid edits coalesce off the main thread
        viewModelScope.launch {
            combine(allPlants, allCareLogs, allPhotos) { p, l, ph ->
                Triple(p, l, ph)
            }
                .distinctUntilChanged()
                .debounce(400L)
                .flowOn(Dispatchers.Default)
                .collectLatest { (plants, logs, photos) ->
                    if (plants.isNotEmpty()) {
                        withContext(Dispatchers.IO) {
                            PlantBackupManager.saveBackup(application, plants, logs, photos)
                        }
                    }
                }
        }

        // TECHNICAL LOGIC & ARCHITECTURAL DESIGN: On-Demand Dual-Cadence Photoperiod Ticker
        // 1. Demand-Gated 1s UI Pulse (_clockTicker):
        //    Only runs while at least one cultivar has an active, non-maintenance photoperiod timer.
        //    When all timers are idle/paused, zero background wakeups or main-thread ticks occur.
        // 2. Three-Second In-Memory Evaluation Batching:
        //    Evaluates cycle completion against in-memory `allPlants.value` instead of polling SQLite,
        //    only persisting to Room when a 24h cycle actually transitions into maintenance lockout.
        viewModelScope.launch {
            var wasLiveNotificationActive = false
            var lastLiveNotifyPhase = ""
            var lastLiveNotifyMinute = -1

            allPlants
                .map { plants -> plants.any { it.isTimerRunning && !it.isPausedForMaintenance } }
                .distinctUntilChanged()
                .collectLatest { hasRunningTimer ->
                    if (!hasRunningTimer) {
                        _clockTicker.value = System.currentTimeMillis()
                        if (wasLiveNotificationActive) {
                            LightCycleTickerService.stop(application)
                            NotificationHelper.cancelLightCycleLiveUpdate(application)
                            wasLiveNotificationActive = false
                            lastLiveNotifyPhase = ""
                            lastLiveNotifyMinute = -1
                        }
                        return@collectLatest
                    }

                    var tickCount = 0
                    while (isActive) {
                        val now = System.currentTimeMillis()
                        _clockTicker.value = now

                        // Batch check every 3 seconds using in-memory cultivar state (zero redundant SQLite reads)
                        if (tickCount % 3 == 0) {
                            val currentPlants = allPlants.value
                            for (plant in currentPlants) {
                                val startedAt = plant.timerStartedAt
                                if (plant.isTimerRunning && startedAt != null) {
                                    val totalSec = (plant.lightOnHours + plant.lightOffHours) * 3600L
                                    if (totalSec > 0 && (now - startedAt) / 1000L >= totalSec) {
                                        // DESIGN DECISION: Safety Lockout on Cycle Completion
                                        val pausedPlant = plant.copy(
                                            isTimerRunning = false,
                                            isPausedForMaintenance = true,
                                            timerStartedAt = null,
                                            timerPausedRemainingSec = null
                                        )
                                        withContext(Dispatchers.IO) {
                                            repository.updatePlant(pausedPlant)
                                        }
                                        NotificationHelper.sendLightMaintenanceAlert(
                                            context = application,
                                            notificationId = (2000 + plant.id).toInt(),
                                            plantName = plant.name,
                                            strain = plant.strain
                                        )
                                    }
                                }
                            }

                            // Synchronize Android 16 Live Update Notification & Status Bar Chip
                            val isLiveEnabled = _liveTimerChipEnabled.value
                            val selectedId = _selectedPlantId.value
                            val activeRunningPlant = currentPlants.firstOrNull {
                                it.id == selectedId && it.isTimerRunning && !it.isPausedForMaintenance
                            } ?: currentPlants.firstOrNull { it.isTimerRunning && !it.isPausedForMaintenance }

                            if (activeRunningPlant != null && isLiveEnabled) {
                                val status = LightCycleEngine.calculateStatus(activeRunningPlant)
                                val currentPhase = status.currentPhaseName
                                val currentMinute = (status.hoursRemainingInPhase * 60 + status.minutesRemainingInPhase).toInt()

                                if (!wasLiveNotificationActive) {
                                    LightCycleTickerService.start(application, activeRunningPlant.id)
                                }

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

                        tickCount++
                        delay(1000)
                    }
                }
        }

        // Lightweight automated weekly backup scheduler (offloaded to IO, hourly cadence)
        viewModelScope.launch(Dispatchers.IO) {
            delay(2000)
            while (isActive) {
                checkAndTriggerAutomatedWeeklyBackup()
                delay(3_600_000L)
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
     * Emergency cultivar start-over override updating only the Plant Identifier, Strain,
     * and target elapsed Day count without altering care logs, Photo Vault entries, or timers.
     *
     * TECHNICAL LOGIC:
     * Screen day counters calculate `((now - plantedDate) / 86_400_000L).coerceAtLeast(0) + 1`.
     * Shifting `plantedDate` backward by `(dayAmount - 1)` 24-hour intervals guarantees all
     * Tracker, Home, and Vault badges immediately reflect the exact chosen Day number without
     * introducing a duplicate Room schema column.
     */
    fun updatePlantEmergencyInfo(
        plantId: Long,
        name: String,
        strain: String,
        dayAmount: Int
    ) {
        viewModelScope.launch {
            val plant = repository.getPlantOnce(plantId) ?: return@launch
            val cleanName = name.trim().ifEmpty { plant.name }
            val cleanStrain = strain.trim()
            val targetDay = dayAmount.coerceAtLeast(1)
            val oneDayMillis = 86_400_000L
            val adjustedPlantedDate = System.currentTimeMillis() - ((targetDay - 1L) * oneDayMillis)
            repository.updatePlant(
                plant.copy(
                    name = cleanName,
                    strain = cleanStrain,
                    plantedDate = adjustedPlantedDate
                )
            )
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
            val pausedRemaining = plant.timerPausedRemainingSec
            val adjustedStartedAt = if (pausedRemaining != null && !plant.isPausedForMaintenance) {
                val totalOnSec = plant.lightOnHours * 3600L
                val elapsedSec = (totalOnSec - pausedRemaining).coerceAtLeast(0L)
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
        viewModelScope.launch(Dispatchers.IO) {
            if (!_careRemindersEnabled.value) return@launch
            val plantsList = repository.getAllPlantsOnce()
            val logsList = repository.getAllCareLogsOnce()
            val lastWaterByPlant = HashMap<Long, Long>((plantsList.size * 2).coerceAtLeast(8))
            for (log in logsList) {
                if (log.actionType == CareActionType.WATER || log.actionType == CareActionType.FERTILIZE) {
                    val prev = lastWaterByPlant[log.plantId]
                    if (prev == null || log.timestamp > prev) {
                        lastWaterByPlant[log.plantId] = log.timestamp
                    }
                }
            }
            val now = System.currentTimeMillis()
            val twoDaysMillis = 48 * 3600 * 1000L

            for (plant in plantsList) {
                val lastWaterTime = lastWaterByPlant[plant.id] ?: plant.plantedDate
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
        viewModelScope.launch(Dispatchers.IO) {
            val plants = repository.getAllPlantsOnce()
            val selectedId = _selectedPlantId.value
            val activeRunning = plants.firstOrNull { it.id == selectedId && it.isTimerRunning && !it.isPausedForMaintenance }
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
        viewModelScope.launch(Dispatchers.IO) {
            val uriStr = _backupFolderUri.value
            if (uriStr.isNullOrBlank()) {
                withContext(Dispatchers.Main) { onResult(false, null) }
                return@launch
            }
            val treeUri = Uri.parse(uriStr)
            val (success, fileName) = PlantBackupManager.exportToFolderUri(getApplication(), treeUri, repository)
            if (success) {
                val now = System.currentTimeMillis()
                _lastBackupTimestamp.value = now
                prefs.edit().putLong("key_last_backup_timestamp", now).apply()
            }
            withContext(Dispatchers.Main) {
                onResult(success, fileName)
            }
        }
    }

    private suspend fun checkAndTriggerAutomatedWeeklyBackup() {
        if (!_weeklyBackupEnabled.value) return
        val uriStr = _backupFolderUri.value ?: return
        val lastTime = _lastBackupTimestamp.value
        val sevenDaysMillis = 7 * 24 * 3600 * 1000L
        if (System.currentTimeMillis() - lastTime >= sevenDaysMillis) {
            val treeUri = Uri.parse(uriStr)
            val (success, _) = PlantBackupManager.exportToFolderUri(getApplication(), treeUri, repository)
            if (success) {
                val now = System.currentTimeMillis()
                _lastBackupTimestamp.value = now
                prefs.edit().putLong("key_last_backup_timestamp", now).apply()
            }
        }
    }

    private val calendarThreadLocal = object : ThreadLocal<java.util.Calendar>() {
        override fun initialValue(): java.util.Calendar = java.util.Calendar.getInstance()
    }

    /**
     * Computes the 1-indexed biological calendar day for a cultivar from its planted timestamp.
     */
    fun calculateCalendarDay(plantedDate: Long, targetDate: Long = System.currentTimeMillis()): Int {
        val cal = calendarThreadLocal.get() ?: java.util.Calendar.getInstance()
        val tz = java.util.TimeZone.getDefault()
        if (cal.timeZone != tz) {
            cal.timeZone = tz
        }
        cal.timeInMillis = plantedDate
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        val startMillis = cal.timeInMillis

        cal.timeInMillis = targetDate
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        val endMillis = cal.timeInMillis

        val diffMillis = endMillis - startMillis
        val days = (diffMillis / (1000 * 60 * 60 * 24)).toInt()
        return (days.coerceAtLeast(0) + 1)
    }

    /**
     * Exports SQLite database tables to a user-selected file URI in JSON or CSV format.
     */
    fun exportDatabaseToUri(uri: Uri, isJson: Boolean, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = PlantBackupManager.exportToFileUri(getApplication(), uri, isJson, repository)
            withContext(Dispatchers.Main) {
                onResult(success)
            }
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
        viewModelScope.launch(Dispatchers.IO) {
            // Unpack ZIP archive or insert parsed entities into Room tables
            val success = if (data.sourceFormat == "ZIP Bundle" && data.sourceUri != null) {
                PlantBackupManager.restoreDatabaseFromZip(getApplication(), repository, data, data.sourceUri)
            } else {
                PlantBackupManager.restoreDatabaseFromData(getApplication(), repository, data)
            }
            if (success) {
                val freshPlants = repository.getAllPlantsOnce()
                val firstId = freshPlants.firstOrNull()?.id
                _timelineShowOnlyToday.value = false
                prefs.edit().putBoolean("key_timeline_show_only_today", false).apply()

                val photoDetail = if (data.bundledImagesCount > 0) {
                    "${data.bundledImagesCount} full-resolution photos"
                } else {
                    "${data.photos.size} vault photos"
                }

                withContext(Dispatchers.Main) {
                    firstId?.let { selectPlant(it) }
                    // Re-arm local Android alarms for watering and light cycles
                    NotificationHelper.scheduleDailyCareAlerts(getApplication())
                    onResult(true, "Successfully restored ${data.plants.size} cultivars, ${data.logs.size} care logs, and $photoDetail.")
                }
            } else {
                withContext(Dispatchers.Main) {
                    onResult(false, "Failed to restore database from backup file.")
                }
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
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllData()
            SeedData.populateIfEmpty(repository)
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
