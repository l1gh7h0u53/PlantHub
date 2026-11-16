package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.AppDatabase
import com.example.data.model.CareActionType
import com.example.data.model.CareLog
import com.example.data.model.GrowthStage
import com.example.data.model.Plant
import com.example.data.repository.PlantRepository
import com.example.ui.components.FloatingBottomBar
import com.example.ui.components.GithubHeatmapGrid
import com.example.ui.components.LightCycleGauge
import com.example.ui.components.PlantIconHelper
import com.example.ui.components.PrivacyPolicyDialog
import com.example.ui.components.QuickCareBar
import com.example.ui.theme.PlantHubTheme
import com.example.ui.viewmodel.HeatmapViewMode
import com.example.ui.viewmodel.NavTab
import com.example.util.HeatmapCalculator
import com.example.util.LightCycleEngine
import com.example.util.currentTimeMillis
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Shared Compose Multiplatform root shell consumed by Desktop JVM and iOS hosts.
 * Demonstrates full Room KMP persistence, LightCycleEngine real-time updates,
 * HeatmapCalculator grids, and the redesigned Material 3 Expressive Tour.
 */
@Composable
fun PlantHubSharedHost(
    database: AppDatabase,
    platformLabel: String
) {
    val repository = remember(database) { PlantRepository(database) }
    val plants by repository.allPlants.collectAsState(initial = emptyList())
    val logs by repository.allCareLogs.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var showTour by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(NavTab.HOME) }
    var heatmapViewMode by remember { mutableStateOf(HeatmapViewMode.WEEKS_16) }
    var selectedPlantId by remember { mutableStateOf<Long?>(null) }
    var clockMillis by remember { mutableLongStateOf(currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (isActive) {
            delay(1000)
            clockMillis = currentTimeMillis()
        }
    }

    val activePlant = plants.find { it.id == selectedPlantId } ?: plants.firstOrNull()
    val fleetHeatmap = remember(logs, heatmapViewMode) {
        when (heatmapViewMode) {
            HeatmapViewMode.YEARLY -> HeatmapCalculator.calculateYearly(logs)
            else -> HeatmapCalculator.calculate16Weeks(logs)
        }
    }

    PlantHubTheme(darkTheme = true, dynamicColor = false) {
        if (showTour) {
            OnboardingScreen(
                onComplete = { openAdd ->
                    showTour = false
                    if (openAdd) {
                        scope.launch {
                            val newId = repository.insertPlant(
                                Plant(
                                    name = "Northern Lights #1",
                                    strain = "Indica Dominant",
                                    stage = GrowthStage.VEGETATIVE,
                                    lightCyclePreset = "Vegetative (18/6)",
                                    lightOnHours = 18,
                                    lightOffHours = 6,
                                    lightStartTimeMinute = 480,
                                    medium = "Coco Coir / Perlite",
                                    potSizeLiters = 15.0,
                                    plantedDate = currentTimeMillis(),
                                    notes = "Added on $platformLabel"
                                )
                            )
                            selectedPlantId = newId
                        }
                    }
                }
            )
        } else {
            Scaffold(
                bottomBar = {
                    FloatingBottomBar(
                        currentTab = selectedTab,
                        onTabSelected = { selectedTab = it }
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.TopCenter
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .widthIn(max = 680.dp)
                            .fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(20.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = PlantIconHelper.OfficialCannabisIcon,
                                                contentDescription = "PlantHub",
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "PlantHub ($platformLabel)",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Compose Multiplatform & Room KMP Engine • v1.8.0",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        if (selectedTab == NavTab.SETTINGS) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(22.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(18.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = "Tour & Philosophy",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        OutlinedButton(
                                            onClick = { showTour = true },
                                            shape = CircleShape,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Filled.Spa, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.size(6.dp))
                                            Text("Replay Feature & Function Tour", fontWeight = FontWeight.Bold)
                                        }
                                        OutlinedButton(
                                            onClick = { showPrivacy = true },
                                            shape = CircleShape,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Filled.Shield, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.size(6.dp))
                                            Text("Privacy & Philosophy", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        } else {
                            item {
                                GithubHeatmapGrid(
                                    gridData = fleetHeatmap,
                                    currentViewMode = heatmapViewMode,
                                    onViewModeChange = { heatmapViewMode = it },
                                    title = "Cultivation Activity"
                                )
                            }

                            if (activePlant != null) {
                                item {
                                    val status = remember(activePlant, clockMillis) {
                                        LightCycleEngine.calculateStatus(activePlant, clockMillis)
                                    }
                                    LightCycleGauge(
                                        status = status,
                                        onStartTimer = {
                                            scope.launch {
                                                repository.updatePlant(
                                                    activePlant.copy(
                                                        isTimerRunning = true,
                                                        timerStartedAt = currentTimeMillis(),
                                                        timerPausedRemainingSec = null,
                                                        isPausedForMaintenance = false
                                                    )
                                                )
                                            }
                                        },
                                        onPauseTimer = {
                                            scope.launch {
                                                val rem = status.hoursRemainingInPhase * 3600L +
                                                    status.minutesRemainingInPhase * 60L +
                                                    status.secondsRemainingInPhase
                                                repository.updatePlant(
                                                    activePlant.copy(
                                                        isTimerRunning = false,
                                                        timerPausedRemainingSec = rem,
                                                        isPausedForMaintenance = false
                                                    )
                                                )
                                            }
                                        },
                                        onResetTimer = {
                                            scope.launch {
                                                repository.updatePlant(
                                                    activePlant.copy(
                                                        isTimerRunning = false,
                                                        timerStartedAt = null,
                                                        timerPausedRemainingSec = null,
                                                        isPausedForMaintenance = false
                                                    )
                                                )
                                            }
                                        }
                                    )
                                }

                                item {
                                    QuickCareBar(
                                        onWaterClick = { ml ->
                                            scope.launch {
                                                repository.insertCareLog(
                                                    CareLog(
                                                        plantId = activePlant.id,
                                                        timestamp = currentTimeMillis(),
                                                        actionType = CareActionType.WATER,
                                                        waterAmountMl = ml,
                                                        notes = "Quick water +$ml mL"
                                                    )
                                                )
                                            }
                                        },
                                        onMistClick = {
                                            scope.launch {
                                                repository.insertCareLog(
                                                    CareLog(
                                                        plantId = activePlant.id,
                                                        timestamp = currentTimeMillis(),
                                                        actionType = CareActionType.MIST,
                                                        notes = "Foliar mist"
                                                    )
                                                )
                                            }
                                        },
                                        onFertilizeClick = {
                                            scope.launch {
                                                repository.insertCareLog(
                                                    CareLog(
                                                        plantId = activePlant.id,
                                                        timestamp = currentTimeMillis(),
                                                        actionType = CareActionType.FERTILIZE,
                                                        notes = "Balanced Organic NPK"
                                                    )
                                                )
                                            }
                                        },
                                        onOpenCustomDialog = {}
                                    )
                                }
                            } else {
                                item {
                                    Surface(
                                        shape = RoundedCornerShape(22.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(24.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Eco,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(36.dp)
                                            )
                                            Text(
                                                text = "Your Garden is Ready",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                OutlinedButton(
                                                    onClick = { showTour = true },
                                                    shape = CircleShape
                                                ) {
                                                    Text("Feature Tour", fontWeight = FontWeight.Bold)
                                                }
                                                Button(
                                                    onClick = {
                                                        scope.launch {
                                                            val newId = repository.insertPlant(
                                                                Plant(
                                                                    name = "Sample Cultivar",
                                                                    strain = "Hybrid",
                                                                    stage = GrowthStage.VEGETATIVE,
                                                                    lightCyclePreset = "Vegetative (18/6)",
                                                                    lightOnHours = 18,
                                                                    lightOffHours = 6,
                                                                    lightStartTimeMinute = 480,
                                                                    medium = "Living Soil",
                                                                    potSizeLiters = 11.0,
                                                                    plantedDate = currentTimeMillis()
                                                                )
                                                            )
                                                            selectedPlantId = newId
                                                        }
                                                    },
                                                    shape = CircleShape
                                                ) {
                                                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(Modifier.size(4.dp))
                                                    Text("Add Cultivar", fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            items(plants, key = { it.id }) { plant ->
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = plant.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${plant.strain} • ${plant.stage.label}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        OutlinedButton(
                                            onClick = { selectedPlantId = plant.id },
                                            shape = CircleShape
                                        ) {
                                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.size(4.dp))
                                            Text("Focus", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showPrivacy) {
            PrivacyPolicyDialog(onDismiss = { showPrivacy = false })
        }
    }
}
