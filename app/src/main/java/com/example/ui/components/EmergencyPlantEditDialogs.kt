package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Plant
import com.example.ui.theme.LocalAccessibilitySuite

/**
 * Step 1 of the emergency start-over flow in Tracker: a short, compact Material 3 Expressive
 * confirmation dialog verifying the user truly intends to override the plant's core record.
 *
 * Uses contrasting shapes (Extra-Large 28.dp card paired with CircleShape pill CTAs),
 * high-contrast amber warning accents for dark/Monet readability, and 48.dp+ touch targets.
 */
@Composable
fun EmergencyEditWarningDialog(
    plantName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val a11y = LocalAccessibilitySuite.current
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            border = BorderStroke(a11y.borderStrokeWidthDp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 400.dp)
                .testTag("dialog_emergency_edit_warning")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Calibrated warning badge adapts cleanly under OS color correction & high contrast
                    Surface(
                        shape = CircleShape,
                        color = a11y.milestoneBadgeBg,
                        border = if (a11y.isColorCorrectionActive || a11y.state.highContrastText) {
                            BorderStroke(a11y.borderStrokeWidthDp, a11y.milestoneBorderColor)
                        } else null,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = "Emergency override warning",
                                tint = a11y.milestoneBadgeContent,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Emergency Plant Edit",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Start-over confirmation",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "Are you sure you want to edit $plantName? Only use this if you had to start over or fix its identifier, strain, or day count. Existing care logs and photos stay saved.",
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = CircleShape,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("btn_emergency_cancel_warning")
                    ) {
                        Text(
                            text = "Keep As Is",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Button(
                        onClick = onConfirm,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = a11y.milestoneBadgeBg,
                            contentColor = a11y.milestoneBadgeContent
                        ),
                        modifier = Modifier
                            .weight(1.1f)
                            .heightIn(min = 48.dp)
                            .testTag("btn_emergency_confirm_warning")
                    ) {
                        Text(
                            text = "Yes, Edit Plant",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Step 2 of the emergency start-over flow: a compact Material 3 Expressive three-field dialog
 * allowing direct updates to the Plant Identifier, Strain, and current Day Amount.
 *
 * Includes a one-tap "Reset Day 1" pill action so growers starting over can reset the day counter
 * cleanly while leaving existing Care Logs, Photo Vault checkpoints, and timers intact.
 */
@Composable
fun EmergencyPlantEditDialog(
    plant: Plant,
    currentDayAmount: Int,
    onDismiss: () -> Unit,
    onSave: (name: String, strain: String, dayAmount: Int) -> Unit
) {
    val a11y = LocalAccessibilitySuite.current
    var nameInput by remember(plant.id) { mutableStateOf(plant.name) }
    var strainInput by remember(plant.id) { mutableStateOf(plant.strain) }
    var dayInput by remember(plant.id, currentDayAmount) {
        mutableStateOf(currentDayAmount.coerceAtLeast(1).toString())
    }

    val trimmedName = nameInput.trim()
    val parsedDay = dayInput.toIntOrNull()
    val isNameValid = trimmedName.isNotEmpty()
    val isDayValid = parsedDay != null && parsedDay >= 1
    val canSave = isNameValid && isDayValid

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            border = BorderStroke(a11y.borderStrokeWidthDp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .testTag("dialog_emergency_plant_edit")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Edit Plant Record",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Identifier, strain & day counter",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Plant Identifier") },
                    singleLine = true,
                    isError = !isNameValid,
                    supportingText = if (!isNameValid) {
                        { Text("Plant identifier cannot be blank") }
                    } else null,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_emergency_plant_name")
                )

                OutlinedTextField(
                    value = strainInput,
                    onValueChange = { strainInput = it },
                    label = { Text("Strain") },
                    placeholder = { Text("e.g. Northern Lights") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_emergency_plant_strain")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = dayInput,
                        onValueChange = { raw ->
                            val digitsOnly = raw.filter { it.isDigit() }.take(4)
                            dayInput = digitsOnly
                        },
                        label = { Text("Day Amount") },
                        prefix = { Text("Day ") },
                        singleLine = true,
                        isError = !isDayValid,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_emergency_day_amount")
                    )

                    OutlinedButton(
                        onClick = { dayInput = "1" },
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("btn_reset_day_one")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.RestartAlt,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Day 1",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }

                Text(
                    text = if (isDayValid) {
                        "Grow counter will show Day $parsedDay. Logs and photos stay saved."
                    } else {
                        "Enter a valid day number (1 or higher)."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDayValid) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = CircleShape,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("btn_emergency_edit_cancel")
                    ) {
                        Text(
                            text = "Cancel",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }

                    Button(
                        onClick = {
                            if (canSave && parsedDay != null) {
                                onSave(trimmedName, strainInput.trim(), parsedDay)
                            }
                        },
                        enabled = canSave,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .weight(1.1f)
                            .heightIn(min = 48.dp)
                            .testTag("btn_emergency_edit_save")
                    ) {
                        Text(
                            text = "Save Changes",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
