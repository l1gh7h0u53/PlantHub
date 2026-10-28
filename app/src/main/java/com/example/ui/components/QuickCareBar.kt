package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GithubHeatmapColors

@Composable
fun QuickCareBar(
    onWaterClick: (Int) -> Unit,
    onMistClick: () -> Unit,
    onFertilizeClick: () -> Unit,
    onOpenCustomDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Detailed Custom Log Button (Expressive Pill)
        Button(
            onClick = onOpenCustomDialog,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier
                .heightIn(min = 44.dp)
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

        // Quick Water 250mL
        AssistChip(
            onClick = { onWaterClick(250) },
            label = { Text("+250mL", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.WaterDrop,
                    contentDescription = null,
                    tint = GithubHeatmapColors.WaterBlue,
                    modifier = Modifier.size(16.dp)
                )
            },
            shape = CircleShape,
            colors = AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier
                .heightIn(min = 44.dp)
                .testTag("quick_water_250")
        )

        // Quick Water 500mL
        AssistChip(
            onClick = { onWaterClick(500) },
            label = { Text("+500mL", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.WaterDrop,
                    contentDescription = null,
                    tint = GithubHeatmapColors.WaterBlue,
                    modifier = Modifier.size(16.dp)
                )
            },
            shape = CircleShape,
            colors = AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier
                .heightIn(min = 44.dp)
                .testTag("quick_water_500")
        )

        // Quick Mist
        AssistChip(
            onClick = onMistClick,
            label = { Text("Foliar Mist", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Waves,
                    contentDescription = null,
                    tint = GithubHeatmapColors.MistTeal,
                    modifier = Modifier.size(16.dp)
                )
            },
            shape = CircleShape,
            colors = AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier
                .heightIn(min = 44.dp)
                .testTag("quick_mist")
        )

        // Quick Feed
        AssistChip(
            onClick = onFertilizeClick,
            label = { Text("Feed NPK", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Science,
                    contentDescription = null,
                    tint = GithubHeatmapColors.NutrientPurple,
                    modifier = Modifier.size(16.dp)
                )
            },
            shape = CircleShape,
            colors = AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier
                .heightIn(min = 44.dp)
                .testTag("quick_feed")
        )
    }
}
