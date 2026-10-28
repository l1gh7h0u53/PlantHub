package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.PhotoEntry
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max

/**
 * Data representation for each plotted point on the elapsed days timeline curve.
 */
data class TimelineChartPoint(
    val photo: PhotoEntry,
    val index: Int,
    val elapsedDays: Float,
    val isMilestone: Boolean,
    val formattedDate: String,
    val dayLabel: String
)

/**
 * Calculates elapsed days progression from the first baseline photo.
 */
fun calculateTimelineChartPoints(photos: List<PhotoEntry>): List<TimelineChartPoint> {
    if (photos.isEmpty()) return emptyList()
    // Chronological order based on dayNumber and timestamp
    val sorted = photos.sortedWith(compareBy({ it.dayNumber }, { it.timestamp }))
    val firstPhoto = sorted.first()
    val firstTimestamp = firstPhoto.timestamp
    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    return sorted.mapIndexed { index, photo ->
        val rawDiffDays = (photo.timestamp - firstTimestamp).toDouble() / (1000.0 * 60 * 60 * 24)
        val elapsedDays = if (rawDiffDays >= 0.05) {
            rawDiffDays.toFloat()
        } else {
            // If timestamps were taken in rapid succession or simulated on the same day,
            // fall back to dayNumber progression so the curve is distinct and educational.
            (photo.dayNumber - firstPhoto.dayNumber).coerceAtLeast(0).toFloat()
        }
        val isMilestone = photo.dayNumber == 1 || (photo.dayNumber % 7 == 0)

        TimelineChartPoint(
            photo = photo,
            index = index,
            elapsedDays = elapsedDays,
            isMilestone = isMilestone,
            formattedDate = dateFormat.format(Date(photo.timestamp)),
            dayLabel = "Day ${photo.dayNumber}"
        )
    }
}

/**
 * Expandable analytics card displaying the interactive timeline curve of elapsed days
 * since the first photo was taken, with tap-activated preview tooltip.
 */
@Composable
fun VaultElapsedDaysAnalyticsCard(
    photos: List<PhotoEntry>,
    onPhotoClick: (PhotoEntry) -> Unit,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = true,
    onExpandedChange: (Boolean) -> Unit = {}
) {
    val points = remember(photos) { calculateTimelineChartPoints(photos) }
    var selectedPoint by remember(points) { mutableStateOf<TimelineChartPoint?>(points.lastOrNull()) }

    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "analytics_chevron_rotation"
    )

    val maxElapsed = points.maxOfOrNull { it.elapsedDays } ?: 0f
    val milestoneCount = points.count { it.isMilestone }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("analytics_expandable_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row (Clickable to toggle expand/collapse)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandedChange(!isExpanded) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Growth Timeline Analytics",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = when {
                                points.isEmpty() -> "No photos logged yet"
                                points.size == 1 -> "Baseline established • 0 days elapsed"
                                else -> "+${String.format(Locale.getDefault(), "%.1f", maxElapsed).removeSuffix(".0")} days elapsed across ${points.size} entries"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (points.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
                        ) {
                            Text(
                                text = "+${maxElapsed.toInt()}d",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { onExpandedChange(!isExpanded) },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("analytics_toggle_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse Analytics" else "Expand Analytics",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.rotate(chevronRotation)
                        )
                    }
                }
            }

            // Collapsible Content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (points.isEmpty()) {
                        // Empty helper state
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PhotoLibrary,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Column {
                                    Text(
                                        text = "No Progression Data Yet",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Capture daily photos using the camera FAB to track elapsed days along your growth timeline.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else if (points.size == 1) {
                        // Single photo state
                        val single = points.first()
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                AsyncImage(
                                    model = single.photo.imageUri.let { if (it.startsWith("/")) File(it) else it },
                                    placeholder = painterResource(R.drawable.planthub_icon_1791667356162),
                                    error = painterResource(R.drawable.planthub_icon_1791667356162),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Baseline Photo Set (${single.dayLabel})",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Recorded on ${single.formattedDate}. Log your next milestone photo to generate the elapsed days timeline curve.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        // Summary Metric Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AnalyticsStatPill(
                                label = "Total Elapsed",
                                value = "+${String.format(Locale.getDefault(), "%.1f", maxElapsed).removeSuffix(".0")} Days",
                                icon = Icons.Filled.Schedule,
                                modifier = Modifier.weight(1f)
                            )
                            AnalyticsStatPill(
                                label = "Photos Logged",
                                value = "${points.size} Entries",
                                icon = Icons.Filled.PhotoLibrary,
                                modifier = Modifier.weight(1f)
                            )
                            AnalyticsStatPill(
                                label = "Milestones",
                                value = "$milestoneCount Checkpoints",
                                icon = Icons.Filled.Event,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Interactive Timeline Chart Curve
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Elapsed Days Curve",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Tap any node to inspect",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                InteractiveElapsedDaysChart(
                                    points = points,
                                    selectedPoint = selectedPoint,
                                    onSelectPoint = { selectedPoint = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(175.dp)
                                        .testTag("timeline_canvas_chart")
                                )
                            }
                        }

                        // Tap-Activated Photo Tooltip / Preview Card
                        selectedPoint?.let { point ->
                            SelectedPointPreviewCard(
                                point = point,
                                onInspectPhoto = { onPhotoClick(point.photo) },
                                onDismiss = { selectedPoint = null }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Metric pill displaying quick analytics statistics.
 */
@Composable
private fun AnalyticsStatPill(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * High-performance interactive Compose Canvas rendering the elapsed days curve.
 */
@Composable
private fun InteractiveElapsedDaysChart(
    points: List<TimelineChartPoint>,
    selectedPoint: TimelineChartPoint?,
    onSelectPoint: (TimelineChartPoint) -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val outlineColor = MaterialTheme.colorScheme.outlineVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    val labelTextStyle = remember(onSurfaceVariant) {
        TextStyle(
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            color = onSurfaceVariant
        )
    }

    val maxDays = remember(points) {
        val calculated = points.maxOfOrNull { it.elapsedDays } ?: 0f
        max(1f, ceil(calculated / 5f) * 5f)
    }

    // Canvas drawing with touch listener
    Canvas(
        modifier = modifier
            .pointerInput(points, maxDays) {
                detectTapGestures { tapOffset ->
                    val paddingLeft = 32.dp.toPx()
                    val paddingRight = 16.dp.toPx()
                    val chartWidth = size.width - paddingLeft - paddingRight
                    val stepX = if (points.size > 1) chartWidth / (points.size - 1) else chartWidth

                    // Find nearest point within touch threshold
                    var closestPoint: TimelineChartPoint? = null
                    var minDistance = Float.MAX_VALUE
                    points.forEachIndexed { index, pt ->
                        val px = paddingLeft + (index * stepX)
                        val dist = abs(tapOffset.x - px)
                        if (dist < minDistance && dist < 42.dp.toPx()) {
                            minDistance = dist
                            closestPoint = pt
                        }
                    }

                    if (closestPoint != null) {
                        onSelectPoint(closestPoint!!)
                    }
                }
            }
    ) {
        val paddingLeft = 32.dp.toPx()
        val paddingRight = 16.dp.toPx()
        val paddingTop = 16.dp.toPx()
        val paddingBottom = 24.dp.toPx()

        val chartWidth = size.width - paddingLeft - paddingRight
        val chartHeight = size.height - paddingTop - paddingBottom
        if (chartWidth <= 0 || chartHeight <= 0) return@Canvas

        // 1. Draw horizontal grid lines and Y-axis elapsed day values
        val gridSteps = 4
        val dashedEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

        for (i in 0..gridSteps) {
            val ratio = i.toFloat() / gridSteps.toFloat()
            val y = chartHeight + paddingTop - (ratio * chartHeight)
            val dayValue = (ratio * maxDays).toInt()

            // Dashed horizontal guideline
            drawLine(
                color = outlineColor.copy(alpha = if (i == 0) 0.6f else 0.35f),
                start = Offset(paddingLeft, y),
                end = Offset(size.width - paddingRight, y),
                strokeWidth = if (i == 0) 1.5.dp.toPx() else 1.dp.toPx(),
                pathEffect = if (i == 0) null else dashedEffect
            )

            // Y-axis label text
            val yLabelText = "${dayValue}d"
            val textLayoutResult = textMeasurer.measure(
                text = yLabelText,
                style = labelTextStyle
            )
            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(
                    x = (paddingLeft - textLayoutResult.size.width - 6.dp.toPx()).coerceAtLeast(0f),
                    y = y - (textLayoutResult.size.height / 2f)
                )
            )
        }

        if (points.size < 2) return@Canvas

        val stepX = chartWidth / (points.size - 1)

        // Compute plotted coordinates (x, y) for all points
        val pointCoords = points.mapIndexed { index, pt ->
            val px = paddingLeft + (index * stepX)
            val normalizedY = (pt.elapsedDays / maxDays).coerceIn(0f, 1f)
            val py = chartHeight + paddingTop - (normalizedY * chartHeight)
            Offset(px, py)
        }

        // 2. Build smooth cubic Bézier spline
        val curvePath = Path()
        val fillPath = Path()

        curvePath.moveTo(pointCoords.first().x, pointCoords.first().y)
        fillPath.moveTo(pointCoords.first().x, chartHeight + paddingTop)
        fillPath.lineTo(pointCoords.first().x, pointCoords.first().y)

        for (i in 0 until pointCoords.size - 1) {
            val p0 = pointCoords[i]
            val p1 = pointCoords[i + 1]
            val cx = (p0.x + p1.x) / 2f

            // Smooth horizontal tangent cubic bezier
            curvePath.cubicTo(
                cx, p0.y,
                cx, p1.y,
                p1.x, p1.y
            )
            fillPath.cubicTo(
                cx, p0.y,
                cx, p1.y,
                p1.x, p1.y
            )
        }

        fillPath.lineTo(pointCoords.last().x, chartHeight + paddingTop)
        fillPath.close()

        // 3. Draw gradient area underneath curve
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.28f),
                    primaryColor.copy(alpha = 0.08f),
                    primaryColor.copy(alpha = 0.0f)
                ),
                startY = paddingTop,
                endY = chartHeight + paddingTop
            )
        )

        // 4. Draw curve outline stroke
        drawPath(
            path = curvePath,
            color = primaryColor,
            style = Stroke(
                width = 3.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // 5. Draw node circles and selection indicators
        pointCoords.forEachIndexed { index, coord ->
            val pt = points[index]
            val isSelected = selectedPoint?.photo?.id == pt.photo.id

            // If selected: draw vertical crosshair guide
            if (isSelected) {
                drawLine(
                    color = primaryColor.copy(alpha = 0.5f),
                    start = Offset(coord.x, paddingTop),
                    end = Offset(coord.x, chartHeight + paddingTop),
                    strokeWidth = 1.2.dp.toPx(),
                    pathEffect = dashedEffect
                )
            }

            // Draw outer node indicator
            if (isSelected) {
                // Outer glowing pulse ring
                drawCircle(
                    color = primaryColor.copy(alpha = 0.22f),
                    radius = 12.dp.toPx(),
                    center = coord
                )
                drawCircle(
                    color = primaryColor,
                    radius = 7.dp.toPx(),
                    center = coord
                )
                drawCircle(
                    color = surfaceColor,
                    radius = 3.5.dp.toPx(),
                    center = coord
                )
            } else if (pt.isMilestone) {
                // Milestone checkpoint node
                drawCircle(
                    color = tertiaryColor.copy(alpha = 0.3f),
                    radius = 8.dp.toPx(),
                    center = coord
                )
                drawCircle(
                    color = surfaceColor,
                    radius = 5.5.dp.toPx(),
                    center = coord
                )
                drawCircle(
                    color = primaryColor,
                    radius = 4.dp.toPx(),
                    center = coord
                )
            } else {
                // Regular daily photo node
                drawCircle(
                    color = surfaceColor,
                    radius = 4.5.dp.toPx(),
                    center = coord
                )
                drawCircle(
                    color = secondaryColor.copy(alpha = 0.85f),
                    radius = 3.dp.toPx(),
                    center = coord
                )
            }

            // Draw X-axis label below each node (e.g. "D1", "D7", "D14")
            val xLabel = "D${pt.photo.dayNumber}"
            val xLayout = textMeasurer.measure(
                text = xLabel,
                style = labelTextStyle.copy(
                    fontWeight = if (isSelected || pt.isMilestone) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) primaryColor else onSurfaceVariant
                )
            )
            drawText(
                textLayoutResult = xLayout,
                topLeft = Offset(
                    x = coord.x - (xLayout.size.width / 2f),
                    y = chartHeight + paddingTop + 6.dp.toPx()
                )
            )
        }
    }
}

/**
 * Rich preview card displaying the photo thumbnail, day number, and formatted date
 * upon tapping any point on the chart.
 */
@Composable
private fun SelectedPointPreviewCard(
    point: TimelineChartPoint,
    onInspectPhoto: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("timeline_point_tooltip")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Photo Thumbnail
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                AsyncImage(
                    model = point.photo.imageUri.let { if (it.startsWith("/")) File(it) else it },
                    placeholder = painterResource(R.drawable.planthub_icon_1791667356162),
                    error = painterResource(R.drawable.planthub_icon_1791667356162),
                    contentDescription = "Photo thumbnail for ${point.dayLabel}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth()
                )

                if (point.isMilestone) {
                    Surface(
                        shape = RoundedCornerShape(topStart = 0.dp, bottomEnd = 8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Text(
                            text = "Milestone",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Info column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = point.dayLabel,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "+${String.format(Locale.getDefault(), "%.1f", point.elapsedDays).removeSuffix(".0")}d",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }

                Text(
                    text = "${point.formattedDate} • ${point.photo.stage.name.lowercase().replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (!point.photo.title.isNullOrBlank() || !point.photo.observation.isNullOrBlank()) {
                    Text(
                        text = (point.photo.title.takeIf { !it.isNullOrBlank() } ?: point.photo.observation).orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onInspectPhoto,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ZoomIn,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Inspect Photo",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Close / Dismiss button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(28.dp)
                    .align(Alignment.Top)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close photo tooltip",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
