package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GithubHeatmapColors
import com.example.ui.viewmodel.HeatmapViewMode
import com.example.util.HeatmapCalculator
import com.example.util.HeatmapDay
import com.example.util.HeatmapGridData

@Composable
fun GithubHeatmapGrid(
    gridData: HeatmapGridData,
    currentViewMode: HeatmapViewMode,
    onViewModeChange: (HeatmapViewMode) -> Unit,
    title: String = "Cultivation Activity",
    isCollapsible: Boolean = false,
    initiallyExpanded: Boolean = true,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val scrollState = rememberScrollState()

    var isExpanded by remember { mutableStateOf(if (isCollapsible) initiallyExpanded else true) }
    var selectedDay by remember { mutableStateOf<HeatmapDay?>(null) }

    // Auto-scroll to the end of the heatmap to see today when expanded
    LaunchedEffect(gridData.weeks.size, isExpanded) {
        if (isExpanded) {
            scrollState.scrollTo(scrollState.maxValue)
        }
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("github_heatmap_grid")
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header stats & title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = isCollapsible) { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (!isExpanded && isCollapsible) {
                            "${gridData.totalContributions} care commits • Tap to unfold activity grid"
                        } else {
                            "${gridData.totalContributions} care commits in past period"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Streak Badge
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocalFireDepartment,
                                contentDescription = "Streak",
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "${gridData.currentStreak}d streak",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }

                    if (isCollapsible) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Collapsible Content
            AnimatedVisibility(visible = isExpanded) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))

                    // View Mode Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = currentViewMode == HeatmapViewMode.WEEKS_16,
                            onClick = { onViewModeChange(HeatmapViewMode.WEEKS_16) },
                            label = { Text("16 Weeks", fontSize = 12.sp) },
                            shape = CircleShape,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                        FilterChip(
                            selected = currentViewMode == HeatmapViewMode.YEARLY,
                            onClick = { onViewModeChange(HeatmapViewMode.YEARLY) },
                            label = { Text("Yearly", fontSize = 12.sp) },
                            shape = CircleShape,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                        FilterChip(
                            selected = currentViewMode == HeatmapViewMode.MONTHLY,
                            onClick = { onViewModeChange(HeatmapViewMode.MONTHLY) },
                            label = { Text("Month Focus", fontSize = 12.sp) },
                            shape = CircleShape,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Matrix Grid
                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Day of week labels (Sun, Tue, Thu, Sat)
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            val dayNames = listOf("S", "M", "T", "W", "T", "F", "S")
                            dayNames.forEachIndexed { index, name ->
                                Box(
                                    modifier = Modifier.size(14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (index % 2 == 1) {
                                        Text(
                                            text = name,
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }

                        // Scrollable week columns
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(scrollState),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            gridData.weeks.forEach { week ->
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    week.forEach { day ->
                                        val cellColor = getLevelColor(day.level, isDark)
                                        val isSelected = selectedDay?.dateKey == day.dateKey

                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(cellColor)
                                                .border(
                                                    width = if (isSelected) 1.5.dp else 0.5.dp,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                                    else if (isDark) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                                    else Color.Transparent,
                                                    shape = RoundedCornerShape(3.dp)
                                                )
                                                .clickable {
                                                    selectedDay = day
                                                }
                                                .testTag("day_cell_${day.dateKey}")
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Heatmap Legend
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Less",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        for (lvl in 0..4) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(RoundedCornerShape(2.5.dp))
                                    .background(getLevelColor(lvl, isDark))
                                    .border(
                                        0.5.dp,
                                        if (isDark) MaterialTheme.colorScheme.outlineVariant else Color.LightGray,
                                        RoundedCornerShape(2.5.dp)
                                    )
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                        }
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "More",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Interactive Day Inspector Card
                    AnimatedVisibility(visible = selectedDay != null) {
                        selectedDay?.let { day ->
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(getLevelColor(day.level, isDark))
                                            )
                                            Text(
                                                text = day.displayDate,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(
                                            onClick = { selectedDay = null },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Close,
                                                contentDescription = "Close",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    if (day.careLogs.isEmpty()) {
                                        Text(
                                            text = "No cultivation care commits on this date.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                        )
                                    } else {
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            day.careLogs.forEach { log ->
                                                Text(
                                                    text = "• ${HeatmapCalculator.describeAction(log)}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontWeight = FontWeight.Medium
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
        }
    }
}

private fun getLevelColor(level: Int, isDark: Boolean): Color {
    return if (isDark) {
        when (level) {
            1 -> GithubHeatmapColors.DarkLevel1
            2 -> GithubHeatmapColors.DarkLevel2
            3 -> GithubHeatmapColors.DarkLevel3
            4 -> GithubHeatmapColors.DarkLevel4
            else -> GithubHeatmapColors.DarkLevel0
        }
    } else {
        when (level) {
            1 -> GithubHeatmapColors.LightLevel1
            2 -> GithubHeatmapColors.LightLevel2
            3 -> GithubHeatmapColors.LightLevel3
            4 -> GithubHeatmapColors.LightLevel4
            else -> GithubHeatmapColors.LightLevel0
        }
    }
}
