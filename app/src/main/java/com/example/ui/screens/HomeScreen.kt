@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.GrowthStage
import com.example.data.model.Plant
import com.example.ui.components.AddCareLogDialog
import com.example.ui.components.AddPlantDialog
import com.example.ui.components.GithubHeatmapGrid
import com.example.ui.components.PlantIconHelper
import com.example.ui.components.QuickCareBottomSheet
import com.example.ui.theme.GithubHeatmapColors
import com.example.ui.viewmodel.HeatmapViewMode
import com.example.ui.viewmodel.PlantViewModel
import com.example.util.LightCycleEngine

@Composable
fun HomeScreen(
    viewModel: PlantViewModel,
    plants: List<Plant>,
    onSelectPlant: (Plant) -> Unit,
    modifier: Modifier = Modifier
) {
    val fleetHeatmap by viewModel.fleetHeatmapData.collectAsStateWithLifecycle()
    val viewMode by viewModel.heatmapViewMode.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var bottomSheetPlant by remember { mutableStateOf<Plant?>(null) }
    var customLogPlant by remember { mutableStateOf<Plant?>(null) }

    val requestOpenAddPlant by viewModel.requestOpenAddPlant.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(requestOpenAddPlant) {
        if (requestOpenAddPlant) {
            showAddDialog = true
            viewModel.consumeAddPlantDialog()
        }
    }

    val lazyListState = rememberLazyListState()
    val isFabExpanded by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex == 0 && !lazyListState.isScrollInProgress
        }
    }

    // Dynamic banner icon: uses cannabis icon by default, or active growing plant's botanical icon
    val activeCultivarIcon = remember(plants) {
        val activePlant = plants.firstOrNull()
        if (activePlant != null) {
            PlantIconHelper.getPlantIcon(activePlant.iconKey)
        } else {
            PlantIconHelper.OfficialCannabisIcon
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .testTag("home_screen"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 200.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // App Header Banner
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = activeCultivarIcon,
                                    contentDescription = "Active Cultivar Icon",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Text(
                            text = "PlantHub",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Cultivation Fleet & Commits Ledger",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Fleet Heatmap Matrix (Collapsible Accordion: Collapsed by default so screen is calm and clean)
        item {
            GithubHeatmapGrid(
                gridData = fleetHeatmap,
                currentViewMode = viewMode,
                onViewModeChange = { viewModel.setHeatmapViewMode(it) },
                title = "Fleet Care Activity",
                isCollapsible = true,
                initiallyExpanded = false
            )
        }

        // Section Title: Active Plants
        item {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Active Plants (${plants.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Select a plant to inspect",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Plant Cards List
        if (plants.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(60.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Spa,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Your Garden is Ready",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "No cultivars added yet. Begin tracking your botanical canopy with custom photoperiod timers, care ledgers, and milestone photos.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Button(
                            onClick = { showAddDialog = true },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.testTag("btn_add_first_plant")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add First Plant", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            items(plants, key = { it.id }) { plant ->
                SpaciousPlantCard(
                    plant = plant,
                    onOpenTracker = { onSelectPlant(plant) },
                    onOpenCareSheet = { bottomSheetPlant = plant },
                    onStartTimer = { viewModel.startLightTimer(plant.id) },
                    onStageChange = { stage -> viewModel.updatePlantStage(plant.id, stage) },
                    onResumeMaintenance = { viewModel.resumeFromMaintenance(plant.id) }
                )
            }
        }
    }

    // Modal Bottom Sheet for Quick Care (Moves cluttered buttons off the card)
    bottomSheetPlant?.let { plant ->
        QuickCareBottomSheet(
            plant = plant,
            onDismiss = { bottomSheetPlant = null },
            onWaterClick = { ml -> viewModel.quickWater(plant.id, ml) },
            onMistClick = { viewModel.quickMist(plant.id) },
            onFertilizeClick = { viewModel.quickFertilize(plant.id) },
            onOpenCustomDialog = { customLogPlant = plant }
        )
    }

    // Custom Detailed Log Dialog
    customLogPlant?.let { plant ->
        AddCareLogDialog(
            plantName = plant.name,
            onDismiss = { customLogPlant = null },
            onSave = { action, water, ph, fertName, fertDose, ec, notes ->
                viewModel.addCareLog(
                    plantId = plant.id,
                    actionType = action,
                    waterAmountMl = water,
                    phLevel = ph,
                    fertilizerName = fertName,
                    fertilizerDose = fertDose,
                    ecPpm = ec,
                    notes = notes
                )
                customLogPlant = null
            }
        )
    }

        // Collapsible Floating Action Button above the floating bottom menu bar
        ExtendedFloatingActionButton(
            onClick = { showAddDialog = true },
            expanded = isFabExpanded,
            icon = {
                Icon(
                    imageVector = Icons.Filled.Eco,
                    contentDescription = "Add Cultivar",
                    modifier = Modifier.size(24.dp)
                )
            },
            text = {
                Text(
                    text = "Add Plant",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            },
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 20.dp, bottom = 100.dp)
                .testTag("fab_add_plant")
        )
    }

    if (showAddDialog) {
        AddPlantDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, strain, stage, preset, onHours, offHours, startMin, medium, potLiters, notes, iconKey ->
                viewModel.addPlant(
                    name = name,
                    strain = strain,
                    stage = stage,
                    preset = preset,
                    onHours = onHours,
                    offHours = offHours,
                    startMin = startMin,
                    medium = medium,
                    potLiters = potLiters,
                    notes = notes,
                    iconKey = iconKey
                )
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun SpaciousPlantCard(
    plant: Plant,
    onOpenTracker: () -> Unit,
    onOpenCareSheet: () -> Unit,
    onStartTimer: () -> Unit,
    onStageChange: (GrowthStage) -> Unit,
    onResumeMaintenance: () -> Unit
) {
    val lightStatus = remember(plant) { LightCycleEngine.calculateStatus(plant) }
    val daysInGrow = ((System.currentTimeMillis() - plant.plantedDate) / (1000 * 60 * 60 * 24)).coerceAtLeast(0) + 1

    val isWaiting = lightStatus.isWaitingToStart
    val isRunning = lightStatus.isTimerRunning
    val isMaintenancePaused = lightStatus.isPausedForMaintenance || plant.isPausedForMaintenance

    var showStageDialog by remember { mutableStateOf(false) }
    var showResumeConfirm by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("plant_card_${plant.id}")
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Plant Name, Strain, and Clickable Stage Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = PlantIconHelper.getPlantIcon(plant.iconKey),
                                contentDescription = plant.name,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = plant.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${plant.strain} • Day $daysInGrow",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Stage Badge - Tap to open detailed Stage Selection Dialog
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFA3F5B1), // Bright emerald filled container
                    modifier = Modifier
                        .clickable { showStageDialog = true }
                        .testTag("btn_stage_badge_${plant.id}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = plant.stage.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00210B) // Crisp deep forest dark text
                        )
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit stage",
                            tint = Color(0xFF00210B), // High-contrast dark edit icon
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Photoperiod Light Cycle HUD Pill
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = when {
                    isMaintenancePaused -> Color(0xFF332005)
                    isWaiting -> MaterialTheme.colorScheme.surfaceVariant
                    !isRunning -> Color(0xFF2E2214)
                    lightStatus.isLightOn -> Color(0xFF2E2400)
                    else -> Color(0xFF15222E)
                },
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    when {
                        isMaintenancePaused -> Color(0xFFFFB74D)
                        isWaiting -> MaterialTheme.colorScheme.outlineVariant
                        !isRunning -> Color(0xFFFFB74D).copy(alpha = 0.5f)
                        lightStatus.isLightOn -> GithubHeatmapColors.LightsOn.copy(alpha = 0.5f)
                        else -> Color(0xFF90CAF9).copy(alpha = 0.4f)
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                FlowRow(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isMaintenancePaused -> Color(0xFFFFB74D)
                                        isWaiting -> MaterialTheme.colorScheme.onSurfaceVariant
                                        !isRunning -> Color(0xFFFFB74D)
                                        lightStatus.isLightOn -> GithubHeatmapColors.LightsOn
                                        else -> Color(0xFF90CAF9)
                                    }
                                )
                        )
                        Text(
                            text = if (isMaintenancePaused) "PAUSED (MAINTENANCE)" else lightStatus.currentPhaseName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isMaintenancePaused -> Color(0xFFFFB74D)
                                isWaiting -> MaterialTheme.colorScheme.onSurfaceVariant
                                !isRunning -> Color(0xFFFFB74D)
                                lightStatus.isLightOn -> GithubHeatmapColors.LightsOn
                                else -> Color(0xFF90CAF9)
                            }
                        )
                    }

                    Text(
                        text = if (isMaintenancePaused) {
                            "Cycle complete • maintenance ready"
                        } else if (isWaiting) {
                            "${lightStatus.hoursRemainingInPhase}h ready"
                        } else {
                            "${lightStatus.hoursRemainingInPhase}h ${lightStatus.minutesRemainingInPhase}m left"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Maintenance Banner & Resume Light Cycle Button
            if (isMaintenancePaused) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF2B1D0E),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB74D).copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Build,
                                contentDescription = null,
                                tint = Color(0xFFFFB74D),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Off-Time Complete • Maintenance Window",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFB74D)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Inspect plants, train branches, and check runoff. Resume cycle when maintenance is done.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { showResumeConfirm = true },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFB74D),
                                contentColor = Color(0xFF201505)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_resume_maintenance_${plant.id}")
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Resume Light Cycle", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Quick Start Timer Button if Idle (Not started yet)
            if (isWaiting && !isMaintenancePaused) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onStartTimer,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_card_start_timer_${plant.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Start Light Cycle (${plant.lightOnHours}h/${plant.lightOffHours}h)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Two Clean, Primary Expressive Buttons with Fully Rounded Shape
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenCareSheet,
                    shape = CircleShape,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_care_options_${plant.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.WaterDrop,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Care Options", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Button(
                    onClick = onOpenTracker,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_open_tracker_${plant.id}")
                ) {
                    Text("Open Tracker", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }

    // Confirmation dialog before resuming light cycle from maintenance
    if (showResumeConfirm) {
        AlertDialog(
            onDismissRequest = { showResumeConfirm = false },
            title = {
                Text(
                    text = "Resume Light Cycle?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Starting the light cycle for \"${plant.name}\" will initiate today's ${plant.lightOnHours}h photoperiod ON phase.\n\n" +
                            "Confirm that maintenance, watering, or pruning is completed."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResumeMaintenance()
                        showResumeConfirm = false
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Start Next Cycle", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResumeConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Detailed Stage Selection Dialog
    if (showStageDialog) {
        AlertDialog(
            onDismissRequest = { showStageDialog = false },
            title = {
                Text(
                    text = "Change Growth Stage",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Select the current lifecycle phase for ${plant.name}:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    GrowthStage.entries.forEach { stage ->
                        val isSelected = plant.stage == stage
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) Color(0xFFA3F5B1) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(
                                if (isSelected) 1.5.dp else 0.8.dp,
                                if (isSelected) Color(0xFF005221) else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onStageChange(stage)
                                    showStageDialog = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isSelected) Color(0xFF00210B) else MaterialTheme.colorScheme.outlineVariant,
                                        modifier = Modifier.size(7.dp)
                                    ) {}
                                    Text(
                                        text = stage.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color(0xFF00210B) else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSelected) Color(0xFF00210B) else MaterialTheme.colorScheme.surfaceVariant,
                                    border = androidx.compose.foundation.BorderStroke(
                                        0.6.dp,
                                        if (isSelected) Color(0xFF00210B) else MaterialTheme.colorScheme.outlineVariant
                                    )
                                ) {
                                    Text(
                                        text = "${stage.defaultOnHours}h / ${stage.defaultOffHours}h",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color(0xFFA3F5B1) else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showStageDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
