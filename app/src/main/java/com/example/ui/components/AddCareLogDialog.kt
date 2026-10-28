package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CareActionType

@Composable
fun AddCareLogDialog(
    plantName: String,
    onDismiss: () -> Unit,
    onSave: (
        actionType: CareActionType,
        waterMl: Int?,
        ph: Double?,
        fertName: String?,
        fertDose: String?,
        ecPpm: Int?,
        notes: String
    ) -> Unit
) {
    var selectedAction by remember { mutableStateOf(CareActionType.WATER) }
    var waterAmountInput by remember { mutableStateOf("1000") }
    var phInput by remember { mutableStateOf("6.4") }
    var fertNameInput by remember { mutableStateOf("BioBizz Bloom") }
    var fertDoseInput by remember { mutableStateOf("2.5 mL/L") }
    var ecPpmInput by remember { mutableStateOf("750") }
    var notesInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = "Log Cultivation Care",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "CARE ACTION FOR $plantName",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )

                // Action chips with fully rounded CircleShape
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CareActionType.entries.take(3).forEach { action ->
                        FilterChip(
                            selected = selectedAction == action,
                            onClick = { selectedAction = action },
                            shape = CircleShape,
                            label = { Text(action.displayName, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CareActionType.entries.drop(3).forEach { action ->
                        FilterChip(
                            selected = selectedAction == action,
                            onClick = { selectedAction = action },
                            shape = CircleShape,
                            label = { Text(action.displayName, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                when (selectedAction) {
                    CareActionType.WATER -> {
                        OutlinedTextField(
                            value = waterAmountInput,
                            onValueChange = { waterAmountInput = it },
                            label = { Text("Water Volume (mL)") },
                            singleLine = true,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_water_ml")
                        )
                        OutlinedTextField(
                            value = phInput,
                            onValueChange = { phInput = it },
                            label = { Text("Water pH (e.g., 6.2 - 6.8)") },
                            singleLine = true,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_water_ph")
                        )
                    }
                    CareActionType.FERTILIZE -> {
                        OutlinedTextField(
                            value = fertNameInput,
                            onValueChange = { fertNameInput = it },
                            label = { Text("Fertilizer / Nutrient Formula") },
                            singleLine = true,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_fert_name")
                        )
                        OutlinedTextField(
                            value = fertDoseInput,
                            onValueChange = { fertDoseInput = it },
                            label = { Text("Dosage (e.g. 2mL / Liter)") },
                            singleLine = true,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_fert_dose")
                        )
                        OutlinedTextField(
                            value = ecPpmInput,
                            onValueChange = { ecPpmInput = it },
                            label = { Text("EC / PPM (optional)") },
                            singleLine = true,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_fert_ec")
                        )
                    }
                    else -> {}
                }

                OutlinedTextField(
                    value = notesInput,
                    onValueChange = { notesInput = it },
                    label = { Text("Observation Notes") },
                    minLines = 2,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_care_notes")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val water = waterAmountInput.toIntOrNull()
                    val ph = phInput.toDoubleOrNull()
                    val ec = ecPpmInput.toIntOrNull()
                    onSave(
                        selectedAction,
                        if (selectedAction == CareActionType.WATER) water else null,
                        if (selectedAction == CareActionType.WATER) ph else null,
                        if (selectedAction == CareActionType.FERTILIZE) fertNameInput else null,
                        if (selectedAction == CareActionType.FERTILIZE) fertDoseInput else null,
                        if (selectedAction == CareActionType.FERTILIZE) ec else null,
                        notesInput
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = CircleShape,
                modifier = Modifier.testTag("btn_save_care_log")
            ) {
                Text("Save Log", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = CircleShape
            ) {
                Text("Cancel")
            }
        }
    )
}
