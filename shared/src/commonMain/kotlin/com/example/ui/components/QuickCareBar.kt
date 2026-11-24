package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalAccessibilitySuite

/**
 * Horizontal quick care action ribbon shared across Android, Desktop, and iOS.
 *
 * Adapts action icon tints and border contrast dynamically via [LocalAccessibilitySuite] and
 * provides descriptive TalkBack labels for each one-tap care action.
 */
@Composable
fun QuickCareBar(
    onWaterClick: (Int) -> Unit,
    onMistClick: () -> Unit,
    onFertilizeClick: () -> Unit,
    onOpenCustomDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val a11y = LocalAccessibilitySuite.current
    val scrollState = rememberScrollState()
    val chipBorder = if (a11y.state.highContrastText || a11y.isColorCorrectionActive) {
        BorderStroke(a11y.borderStrokeWidthDp, MaterialTheme.colorScheme.outline)
    } else {
        BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onOpenCustomDialog,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier
                .heightIn(min = 48.dp)
                .semantics { contentDescription = "Log custom cultivation care action" }
                .testTag("action_custom_log")
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "Log Action",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        AssistChip(
            onClick = { onWaterClick(250) },
            label = { Text("+250mL", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.WaterDrop,
                    contentDescription = null,
                    tint = a11y.waterAccent,
                    modifier = Modifier.size(16.dp)
                )
            },
            shape = CircleShape,
            border = chipBorder,
            colors = AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier
                .heightIn(min = 48.dp)
                .semantics { contentDescription = "Quick log 250 milliliters water" }
                .testTag("quick_water_250")
        )

        AssistChip(
            onClick = { onWaterClick(500) },
            label = { Text("+500mL", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.WaterDrop,
                    contentDescription = null,
                    tint = a11y.waterAccent,
                    modifier = Modifier.size(16.dp)
                )
            },
            shape = CircleShape,
            border = chipBorder,
            colors = AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier
                .heightIn(min = 48.dp)
                .semantics { contentDescription = "Quick log 500 milliliters water" }
                .testTag("quick_water_500")
        )

        AssistChip(
            onClick = onMistClick,
            label = { Text("Foliar Mist", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Waves,
                    contentDescription = null,
                    tint = a11y.mistAccent,
                    modifier = Modifier.size(16.dp)
                )
            },
            shape = CircleShape,
            border = chipBorder,
            colors = AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier
                .heightIn(min = 48.dp)
                .semantics { contentDescription = "Quick log foliar mist" }
                .testTag("quick_mist")
        )

        AssistChip(
            onClick = onFertilizeClick,
            label = { Text("Feed NPK", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Science,
                    contentDescription = null,
                    tint = a11y.fertilizeAccent,
                    modifier = Modifier.size(16.dp)
                )
            },
            shape = CircleShape,
            border = chipBorder,
            colors = AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier
                .heightIn(min = 48.dp)
                .semantics { contentDescription = "Quick log nutrient feed" }
                .testTag("quick_feed")
        )
    }
}
