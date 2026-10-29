@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)

package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GrowthStage
import java.util.Locale

/**
 * AddPlantDialog displays a Material 3 dialog form for onboarding a new cultivar.
 *
 * Features:
 * - Cultivar name and strain genetics inputs.
 * - Alphabetized botanical icon selector with preview badge.
 * - Compact growth stage selector chips (Seedling, Vegetative, Flowering, Late Bloom, Flush, Curing).
 * - Synchronized Photoperiod presets (18/6 Vegetative, 12/12 Flowering, 24/0 Continuous, 20/4 Autoflower, Custom).
 * - Native Material 3 TimePicker modal dialog for exact light turn-on scheduling.
 * - M3 segmented group substrate selection menu (Organic Soil, Coco Coir, DWC Hydroponics, Rockwool, Aeroponics).
 * - Container volume in liters and agronomic cultivation notes.
 * - Distinctive red pill-shaped Cancel button and primary Save CTA.
 */
@Composable
fun AddPlantDialog(
    onDismiss: () -> Unit,
    onSave: (
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
        iconKey: String
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var strain by remember { mutableStateOf("") }
    var selectedIconKey by remember { mutableStateOf("cannabis") }
    var stage by remember { mutableStateOf(GrowthStage.VEGETATIVE) }
    var preset by remember { mutableStateOf("18/6 Vegetative") }
    var onHours by remember { mutableIntStateOf(18) }
    var offHours by remember { mutableIntStateOf(6) }

    // Time picker states (adaptive Material 3 TimePicker)
    var selectedHour by remember { mutableIntStateOf(6) }
    var selectedMinute by remember { mutableIntStateOf(0) }
    var showTimePickerModal by remember { mutableStateOf(false) }

    // Substrate hybrid (editable text + popup menu)
    data class SubstrateGroupItem(
        val name: String,
        val category: String,
        val description: String
    )

    var medium by remember { mutableStateOf("Living Organic Soil") }
    var isSubstrateExpanded by remember { mutableStateOf(false) }
    val commonSubstrates = remember {
        listOf(
            SubstrateGroupItem("Living Organic Soil", "Living Soil & Compost", "Active biological buffer with beneficial mycorrhizae"),
            SubstrateGroupItem("Coco Coir / Perlite (70/30)", "Inert Hydro Medium", "High aeration, fast drainage, and vigorous root expansion"),
            SubstrateGroupItem("Hydroponics / DWC (Deep Water Culture)", "Aqueous Culture", "Direct root immersion in bubbled oxygenated nutrient bath"),
            SubstrateGroupItem("Peat Moss / Vermiculite Mix", "Soilless Peat Blend", "High moisture holding capacity with steady buffer"),
            SubstrateGroupItem("Rockwool Cubes", "Inert Mineral Matrix", "Sterile spun basalt plugs for clones and vegetative drip"),
            SubstrateGroupItem("Super Soil (SubCool Recipe)", "Rich Organic Compost", "Fully amended water-only soil for dense cannabinoid profiles"),
            SubstrateGroupItem("Aeroponics", "Misted Root Chamber", "Suspended root architecture fed with pressurized aerosol mist")
        )
    }

    var potLitersInput by remember { mutableStateOf("15.0") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = "New Cultivar Profile",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Plant Identifier (e.g. Gorilla Glue #4)") },
                    shape = RoundedCornerShape(16.dp),
                    minLines = 1,
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_plant_name")
                )

                OutlinedTextField(
                    value = strain,
                    onValueChange = { strain = it },
                    label = { Text("Strain / Cultivar Lineage") },
                    shape = RoundedCornerShape(16.dp),
                    minLines = 1,
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_plant_strain")
                )

                Text(
                    text = "BOTANICAL ICON",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PlantIconHelper.options.forEach { option ->
                        val isSelected = selectedIconKey == option.key
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .clickable { selectedIconKey = option.key }
                                .padding(vertical = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = option.icon,
                                    contentDescription = option.label,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = option.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Growth Stage: Compact, snug pills with no redundant empty whitespace
                Text(
                    text = "CURRENT GROWTH STAGE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    GrowthStage.entries.forEach { s ->
                        val isSelected = stage == s
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .clickable {
                                    stage = s
                                    when (s) {
                                        GrowthStage.SEEDLING, GrowthStage.VEGETATIVE -> {
                                            preset = "18/6 Vegetative"
                                            onHours = 18
                                            offHours = 6
                                        }
                                        GrowthStage.FLOWERING -> {
                                            preset = "12/12 Flowering"
                                            onHours = 12
                                            offHours = 12
                                        }
                                        GrowthStage.AUTOFLOWER -> {
                                            preset = "20/4 Autoflower"
                                            onHours = 20
                                            offHours = 4
                                        }
                                        else -> {}
                                    }
                                }
                        ) {
                            Text(
                                text = s.label,
                                fontSize = 10.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "LIGHT CYCLE PHOTOPERIOD",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("18/6 Veg", "12/12 Flower", "20/4 Auto").forEach { p ->
                        FilterChip(
                            selected = preset.startsWith(p.take(4)),
                            onClick = {
                                if (p.startsWith("18/6")) {
                                    preset = "18/6 Vegetative"; onHours = 18; offHours = 6
                                } else if (p.startsWith("12/12")) {
                                    preset = "12/12 Flowering"; onHours = 12; offHours = 12
                                } else {
                                    preset = "20/4 Autoflower"; onHours = 20; offHours = 4
                                }
                            },
                            shape = CircleShape,
                            label = { Text(p, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                        )
                    }
                }

                // Adaptive Material 3 Light Turn-On Time Picker
                Text(
                    text = "LIGHT TURN-ON TIME",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val formattedTime = formatTime12h(selectedHour, selectedMinute)
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTimePickerModal = true }
                        .testTag("btn_select_turn_on_time")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.AccessTime,
                                        contentDescription = "Turn-on time",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Turn-On Schedule",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formattedTime,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "Select Time",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Hybrid Substrate Input: Text field + popup dropdown menu
                Text(
                    text = "GROW MEDIUM / SUBSTRATE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ExposedDropdownMenuBox(
                    expanded = isSubstrateExpanded,
                    onExpandedChange = { isSubstrateExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = medium,
                        onValueChange = { medium = it },
                        label = { Text("Substrate (Select or type custom)") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isSubstrateExpanded)
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .testTag("input_plant_medium")
                    )
                    ExposedDropdownMenu(
                        expanded = isSubstrateExpanded,
                        onDismissRequest = { isSubstrateExpanded = false },
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // M3 List Group Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "SELECT SUBSTRATE MEDIUM",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${commonSubstrates.size} Standard Mixes",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                )
                            }

                            commonSubstrates.forEach { option ->
                                val isSelected = medium == option.name
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                    border = BorderStroke(
                                        width = if (isSelected) 1.5.dp else 0.8.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            medium = option.name
                                            isSubstrateExpanded = false
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = option.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${option.category} • ${option.description}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                                            )
                                        }
                                        if (isSelected) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Check,
                                                        contentDescription = "Selected",
                                                        tint = MaterialTheme.colorScheme.onPrimary,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = potLitersInput,
                    onValueChange = { potLitersInput = it },
                    label = { Text("Container Volume (Liters)") },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_plant_pot_size")
                )

                // Notes and Strategy with multi-line capability
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes and Strategy") },
                    shape = RoundedCornerShape(16.dp),
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_plant_notes")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val startMin = (selectedHour * 60 + selectedMinute).coerceIn(0, 1439)
                        val potL = potLitersInput.toDoubleOrNull() ?: 15.0
                        onSave(
                            name,
                            strain.ifBlank { "Unknown Cultivar" },
                            stage,
                            preset,
                            onHours,
                            offHours,
                            startMin,
                            medium,
                            potL,
                            notes,
                            selectedIconKey
                        )
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = CircleShape,
                modifier = Modifier.testTag("btn_save_plant")
            ) {
                Text("Create Plant", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFDC2626),
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                modifier = Modifier.testTag("btn_cancel_plant")
            ) {
                Text("Cancel", fontWeight = FontWeight.Bold)
            }
        }
    )

    // Material 3 Adaptive Time Picker Dialog
    if (showTimePickerModal) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedHour,
            initialMinute = selectedMinute,
            is24Hour = false
        )

        AlertDialog(
            onDismissRequest = { showTimePickerModal = false },
            shape = RoundedCornerShape(28.dp),
            title = {
                Text(
                    text = "Select Turn-On Time",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedHour = timePickerState.hour
                        selectedMinute = timePickerState.minute
                        showTimePickerModal = false
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Confirm Time", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showTimePickerModal = false },
                    shape = CircleShape
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun formatTime12h(hour: Int, minute: Int): String {
    val amPm = if (hour >= 12) "PM" else "AM"
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return String.format(Locale.US, "%02d:%02d %s", displayHour, minute, amPm)
}
