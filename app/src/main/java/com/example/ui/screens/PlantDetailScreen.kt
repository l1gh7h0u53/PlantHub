@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.example.ui.components.PlantIconHelper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CareActionType
import com.example.data.model.CareLog
import com.example.data.model.Plant
import com.example.ui.components.AddCareLogDialog
import com.example.ui.components.EmergencyEditWarningDialog
import com.example.ui.components.EmergencyPlantEditDialog
import com.example.ui.components.GithubHeatmapGrid
import com.example.ui.components.LightCycleGauge
import com.example.ui.components.QuickCareBar
import com.example.ui.components.QuickCareBottomSheet
import com.example.ui.theme.LocalAccessibilitySuite
import com.example.ui.viewmodel.PlantViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PlantDetailScreen(
    viewModel: PlantViewModel,
    plants: List<Plant>,
    selectedPlant: Plant?,
    modifier: Modifier = Modifier
) {
    val plantLogs by viewModel.selectedPlantLogs.collectAsStateWithLifecycle()
    val heatmapData by viewModel.selectedPlantHeatmapData.collectAsStateWithLifecycle()
    val viewMode by viewModel.heatmapViewMode.collectAsStateWithLifecycle()

    var showCustomLogDialog by remember { mutableStateOf(false) }
    var showCareSheet by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showEmergencyWarningDialog by remember { mutableStateOf(false) }
    var showEmergencyEditDialog by remember { mutableStateOf(false) }
    val showOnlyToday by viewModel.timelineShowOnlyToday.collectAsStateWithLifecycle()

    val startOfToday = remember {
        java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val endOfToday = startOfToday + (24 * 60 * 60 * 1000L) - 1L

    val todayLogs = remember(plantLogs, startOfToday, endOfToday) {
        plantLogs.filter { it.timestamp in startOfToday..endOfToday }
    }
    val displayedLogs = if (showOnlyToday) todayLogs else plantLogs

    if (selectedPlant == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "No cultivar selected",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Add or open a cultivar in Plants to track light cycles and care history.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    val daysInGrow = remember(selectedPlant.id, selectedPlant.plantedDate) {
        ((System.currentTimeMillis() - selectedPlant.plantedDate) / (1000 * 60 * 60 * 24)).coerceAtLeast(0) + 1
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("plant_detail_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Plant Selector Filter Chips (M3 Expressive)
        if (plants.size > 1) {
            item(key = "plant_selector", contentType = "plant_selector") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    plants.forEach { p ->
                        FilterChip(
                            selected = p.id == selectedPlant.id,
                            onClick = { viewModel.selectPlant(p.id) },
                            shape = RoundedCornerShape(14.dp),
                            label = { Text(p.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                            leadingIcon = {
                                Icon(
                                    imageVector = PlantIconHelper.getPlantIcon(p.iconKey),
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }
        }

        // Header Card & Controls
        item(key = "plant_header_${selectedPlant.id}", contentType = "plant_header") {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Row(
                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = PlantIconHelper.getPlantIcon(selectedPlant.iconKey),
                                        contentDescription = selectedPlant.name,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = selectedPlant.name,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${selectedPlant.strain} • Day $daysInGrow • ${selectedPlant.stage.label}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Emergency Plant Edit Button (opens two-step confirmation conversation menu)
                            IconButton(
                                onClick = { showEmergencyWarningDialog = true },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("btn_edit_plant")
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.Edit,
                                            contentDescription = "Edit plant",
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            // Delete Plant
                            IconButton(
                                onClick = { showDeleteConfirm = true },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("btn_delete_plant")
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = "Delete plant",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Specs line using FlowRow with M3 Expressive pill shapes
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SpecPill(label = "Medium", value = selectedPlant.medium)
                        SpecPill(label = "Pot Size", value = "${selectedPlant.potSizeLiters} L")
                        SpecPill(label = "Photoperiod", value = "${selectedPlant.lightOnHours}h / ${selectedPlant.lightOffHours}h")
                    }

                    if (selectedPlant.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Notes: ${selectedPlant.notes}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Real-Time System Clock Photoperiod HUD (isolated into its own scope so 1s ticks never recompose the rest of PlantDetailScreen)
        item(key = "light_gauge_${selectedPlant.id}", contentType = "light_gauge") {
            LivePhotoperiodSection(
                viewModel = viewModel,
                plantId = selectedPlant.id
            )
        }

        // Quick Care Action Bar & Modal Sheet trigger
        item(key = "quick_care_${selectedPlant.id}", contentType = "quick_care") {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CARE LOGS & ACTIONS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )

                    TextButton(onClick = { showCareSheet = true }) {
                        Text("All Care Options", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                QuickCareBar(
                    onWaterClick = { ml -> viewModel.quickWater(selectedPlant.id, ml) },
                    onMistClick = { viewModel.quickMist(selectedPlant.id) },
                    onFertilizeClick = { viewModel.quickFertilize(selectedPlant.id) },
                    onOpenCustomDialog = { showCustomLogDialog = true }
                )
            }
        }

        // Plant-Specific GitHub Heatmap
        item(key = "heatmap_${selectedPlant.id}", contentType = "heatmap") {
            GithubHeatmapGrid(
                gridData = heatmapData,
                currentViewMode = viewMode,
                onViewModeChange = { viewModel.setHeatmapViewMode(it) },
                title = "${selectedPlant.name} Activity Matrix",
                isCollapsible = false
            )
        }

        // Care Timeline Header
        item(key = "care_timeline_header", contentType = "timeline_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = if (showOnlyToday) "Today's Care Actions (${todayLogs.size})" else "Care Timeline (${plantLogs.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (showOnlyToday) "Logged today • Historical ledger in activity matrix" else "Full historical care ledger",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FilterChip(
                    selected = showOnlyToday,
                    onClick = { viewModel.setTimelineShowOnlyToday(!showOnlyToday) },
                    label = {
                        Text(
                            text = if (showOnlyToday) "Today Only" else "All History",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    shape = CircleShape,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.testTag("filter_today_care_timeline")
                )
            }
        }

        // Care Logs List
        if (displayedLogs.isEmpty()) {
            item(key = "care_timeline_empty", contentType = "empty_state") {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (showOnlyToday) "No care activities logged today yet." else "No care logs recorded yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (showOnlyToday)
                                "Use the Quick Care bar above to commit water, foliar mist, or feeding for today. Past logs remain preserved in the Activity Matrix."
                            else
                                "Use the action buttons above to commit your first care event.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(
                items = displayedLogs,
                key = { it.id },
                contentType = { "care_log_item" }
            ) { log ->
                ExpressiveCareTimelineItem(log = log)
            }
        }
    }

    if (showCareSheet) {
        QuickCareBottomSheet(
            plant = selectedPlant,
            onDismiss = { showCareSheet = false },
            onWaterClick = { ml -> viewModel.quickWater(selectedPlant.id, ml) },
            onMistClick = { viewModel.quickMist(selectedPlant.id) },
            onFertilizeClick = { viewModel.quickFertilize(selectedPlant.id) },
            onOpenCustomDialog = { showCustomLogDialog = true }
        )
    }

    if (showCustomLogDialog) {
        AddCareLogDialog(
            plantName = selectedPlant.name,
            onDismiss = { showCustomLogDialog = false },
            onSave = { action, water, ph, fertName, fertDose, ec, notes ->
                viewModel.addCareLog(
                    plantId = selectedPlant.id,
                    actionType = action,
                    waterAmountMl = water,
                    phLevel = ph,
                    fertilizerName = fertName,
                    fertilizerDose = fertDose,
                    ecPpm = ec,
                    notes = notes
                )
                showCustomLogDialog = false
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete ${selectedPlant.name}?") },
            text = { Text("This will permanently delete this plant along with its care history and photo checkpoints.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePlant(selectedPlant)
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showEmergencyWarningDialog) {
        EmergencyEditWarningDialog(
            plantName = selectedPlant.name,
            onDismiss = { showEmergencyWarningDialog = false },
            onConfirm = {
                showEmergencyWarningDialog = false
                showEmergencyEditDialog = true
            }
        )
    }

    if (showEmergencyEditDialog) {
        EmergencyPlantEditDialog(
            plant = selectedPlant,
            currentDayAmount = daysInGrow.toInt(),
            onDismiss = { showEmergencyEditDialog = false },
            onSave = { newName, newStrain, newDayAmount ->
                viewModel.updatePlantEmergencyInfo(
                    plantId = selectedPlant.id,
                    name = newName,
                    strain = newStrain,
                    dayAmount = newDayAmount
                )
                showEmergencyEditDialog = false
            }
        )
    }
}

@Composable
private fun SpecPill(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "$label:",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun LivePhotoperiodSection(
    viewModel: PlantViewModel,
    plantId: Long
) {
    val lightStatus by viewModel.currentLightStatus.collectAsStateWithLifecycle()
    LightCycleGauge(
        status = lightStatus,
        onStartTimer = { viewModel.startLightTimer(plantId) },
        onPauseTimer = { viewModel.pauseLightTimer(plantId) },
        onResetTimer = { viewModel.resetLightTimer(plantId) }
    )
}

private val careTimelineDateFormat = object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue(): SimpleDateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.US)
}

@Composable
private fun ExpressiveCareTimelineItem(log: CareLog) {
    val formattedDate = remember(log.timestamp) {
        val fmt = careTimelineDateFormat.get() ?: SimpleDateFormat("MMM d, h:mm a", Locale.US)
        fmt.format(Date(log.timestamp))
    }
    val a11y = LocalAccessibilitySuite.current

    val (icon, iconTint, iconBg) = when (log.actionType) {
        CareActionType.WATER -> Triple(
            Icons.Filled.WaterDrop,
            a11y.waterAccent,
            a11y.waterBg
        )
        CareActionType.MIST -> Triple(
            Icons.Filled.Waves,
            a11y.mistAccent,
            a11y.mistBg
        )
        CareActionType.FERTILIZE -> Triple(
            Icons.Filled.Science,
            a11y.fertilizeAccent,
            a11y.fertilizeBg
        )
        CareActionType.DEFOLIATE -> Triple(
            Icons.Filled.ContentCut,
            a11y.defoliateAccent,
            a11y.defoliateBg
        )
        CareActionType.TRAIN -> Triple(
            Icons.Filled.FitnessCenter,
            a11y.trainAccent,
            a11y.trainBg
        )
        CareActionType.NOTE -> Triple(
            Icons.Filled.EditNote,
            MaterialTheme.colorScheme.onSurfaceVariant,
            MaterialTheme.colorScheme.surfaceVariant
        )
    }

    val detailText = buildString {
        if (log.waterAmountMl != null) append("${log.waterAmountMl} mL  ")
        if (log.phLevel != null) append("pH: ${log.phLevel}  ")
        if (log.fertilizerName != null) append("${log.fertilizerName} (${log.fertilizerDose ?: "Standard"})  ")
        if (log.ecPpm != null) append("EC: ${log.ecPpm}ppm  ")
        if (log.notes.isNotBlank()) append(log.notes)
    }

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(a11y.borderStrokeWidthDp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = buildString {
                    append("${log.actionType.displayName} logged on $formattedDate")
                    if (detailText.isNotBlank()) append(". $detailText")
                }
            }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Leading Circular Icon Node
            Surface(
                shape = CircleShape,
                color = iconBg,
                border = if (a11y.useNonColorPatterns || a11y.state.highContrastText) {
                    androidx.compose.foundation.BorderStroke(
                        a11y.borderStrokeWidthDp,
                        iconTint.copy(alpha = 0.7f)
                    )
                } else null,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = log.actionType.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (detailText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = detailText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
