package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LightCycleStatus
import com.example.ui.theme.GithubHeatmapColors
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LightCycleGauge(
    status: LightCycleStatus?,
    onStartTimer: () -> Unit = {},
    onPauseTimer: () -> Unit = {},
    onResetTimer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (status == null) return

    val isLightsOn = status.isLightOn
    val isWaiting = status.isWaitingToStart
    val isRunning = status.isTimerRunning
    val isMaintenance = status.isPausedForMaintenance

    var showResumeConfirm by remember { mutableStateOf(false) }

    val badgeBgColor = when {
        isMaintenance -> Color(0xFF38260D)
        isWaiting -> MaterialTheme.colorScheme.surfaceVariant
        !isRunning -> Color(0xFF2E2214)
        isLightsOn -> Color(0xFF332A00)
        else -> Color(0xFF1E2836)
    }

    val badgeTextColor = when {
        isMaintenance -> Color(0xFFFFB74D)
        isWaiting -> MaterialTheme.colorScheme.onSurfaceVariant
        !isRunning -> Color(0xFFFFB74D)
        isLightsOn -> GithubHeatmapColors.LightsOn
        else -> Color(0xFF90CAF9)
    }

    val indicatorColor = when {
        isMaintenance -> Color(0xFFFFB74D)
        isWaiting -> MaterialTheme.colorScheme.outlineVariant
        !isRunning -> Color(0xFFFFB74D)
        isLightsOn -> GithubHeatmapColors.LightsOn
        else -> Color(0xFF64B5F6)
    }

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.2.dp,
            if (isRunning && isLightsOn) GithubHeatmapColors.LightsOn.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("light_cycle_gauge")
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header Row: Phase Badge + Photoperiod Preset Chip
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Status Pill
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = badgeBgColor,
                    border = androidx.compose.foundation.BorderStroke(1.dp, badgeTextColor.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WavyActiveDotIndicator(
                            isRunning = isRunning && !isMaintenance && !isWaiting,
                            color = badgeTextColor
                        )

                        Icon(
                            imageVector = when {
                                isMaintenance -> Icons.Filled.Build
                                isWaiting -> Icons.Filled.HourglassBottom
                                !isRunning -> Icons.Filled.Pause
                                isLightsOn -> Icons.Filled.WbSunny
                                else -> Icons.Filled.Bedtime
                            },
                            contentDescription = null,
                            tint = badgeTextColor,
                            modifier = Modifier.size(16.dp)
                        )

                        Text(
                            text = status.currentPhaseName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = badgeTextColor
                        )
                    }
                }

                // Photoperiod Schedule Tag
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Text(
                        text = "${status.presetName} (${status.totalOnHours}h / ${status.totalOffHours}h)",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Countdown HUD Digital Display
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Column(modifier = Modifier.padding(end = 8.dp)) {
                    Text(
                        text = if (isWaiting) "INITIAL DURATION (READY TO START)" else "TIME REMAINING IN CURRENT PHASE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = String.format(
                            Locale.US,
                            "%02dh %02dm %02ds",
                            status.hoursRemainingInPhase,
                            status.minutesRemainingInPhase,
                            status.secondsRemainingInPhase
                        ),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.align(Alignment.Bottom)
                ) {
                    Text(
                        text = if (isWaiting) "0% (Idle)" else "${(status.phaseProgressPercent * 100).toInt()}% elapsed",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // M3 Expressive Animated Wavy Active Indicator
            ExpressiveWavyProgressIndicator(
                progress = status.phaseProgressPercent,
                isRunning = isRunning,
                isLightsOn = isLightsOn,
                baseColor = indicatorColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Timing Details Row
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = "Kickoff: ${status.startTimeFormatted}",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = "Next Switch: ${status.nextSwitchTimeFormatted}",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Interactive Timer Control Buttons (Start / Pause / Reset)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isWaiting) {
                    Button(
                        onClick = onStartTimer,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("btn_start_light_cycle")
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Light Cycle Now", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                } else if (isRunning) {
                    Button(
                        onClick = onPauseTimer,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("btn_pause_light_cycle")
                    ) {
                        Icon(Icons.Filled.Pause, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pause Timer", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onResetTimer,
                        shape = CircleShape,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("btn_reset_light_cycle")
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset", fontWeight = FontWeight.Bold)
                    }
                } else {
                    // Paused or Maintenance state
                    Button(
                        onClick = {
                            if (isMaintenance) {
                                showResumeConfirm = true
                            } else {
                                onStartTimer()
                            }
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isMaintenance) Color(0xFFFFB74D) else MaterialTheme.colorScheme.primary,
                            contentColor = if (isMaintenance) Color(0xFF201505) else MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("btn_resume_light_cycle")
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isMaintenance) "Resume Light Cycle" else "Resume", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onResetTimer,
                        shape = CircleShape,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("btn_reset_light_cycle")
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

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
                    text = "Starting the light cycle will initiate today's ${status.totalOnHours}h photoperiod ON phase.\n\n" +
                            "Confirm that maintenance, inspection, and care routines are completed."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onStartTimer()
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
}

/**
 * Material 3 Expressive Animated Wavy Progress Indicator.
 * Renders an oscillating sinusoidal fluid wave representing active photonic energy during
 * the photoperiod ON and OFF phases, replacing flat progress bars with expressive organic motion.
 */
/**
 * Material 3 Expressive Wavy Linear Progress Indicator.
 * Conforms strictly to M3 Progress Indicator guidelines:
 * - Undulating active sine wave stroke with rounded caps (not a flat-bottom fill)
 * - Separation gap between active wave head and inactive track
 * - Stop indicator dot at track's 100% boundary
 * - Continuous smooth traveling wave phase when active
 * - Smooth amplitude easing into a resting flat bar when paused or idle
 */
@Composable
fun ExpressiveWavyProgressIndicator(
    progress: Float,
    isRunning: Boolean,
    isLightsOn: Boolean,
    modifier: Modifier = Modifier,
    baseColor: Color = if (isLightsOn) GithubHeatmapColors.LightsOn else Color(0xFF64B5F6),
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val clampedProgress = progress.coerceIn(0f, 1f)

    // Continuous smooth wave flow animation conforming to M3 Expressive kinetic physics
    val transition = rememberInfiniteTransition(label = "m3_wavy_progress_transition")
    val wavePhase by if (isRunning) {
        transition.animateFloat(
            initialValue = 0f,
            targetValue = (2 * Math.PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2000, easing = LinearEasing)
            ),
            label = "wave_phase"
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    // M3 amplitude easing: animates from full wave amplitude down to 0f (flat resting bar) when paused or idle
    val amplitudeFraction by animateFloatAsState(
        targetValue = if (isRunning) 1f else 0f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "m3_wave_amplitude"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("wavy_progress_canvas")
        ) {
            val width = size.width
            val height = size.height
            if (width <= 0f || height <= 0f) return@Canvas

            val strokeWidth = 8.dp.toPx()
            val trackStrokeWidth = 6.dp.toPx()
            val gap = 5.dp.toPx()
            val stopDotRadius = 3.dp.toPx()
            val centerY = height / 2f

            // M3 sinusoidal wave geometry
            val targetAmplitude = 3.5.dp.toPx() * amplitudeFraction
            val wavelength = 30.dp.toPx()
            val frequency = (2 * Math.PI / wavelength).toFloat()

            // Calculate active segment width
            val activeEnd = (width * clampedProgress).coerceIn(0f, width)

            // 1. Draw M3 Stop Indicator dot at the 100% boundary
            val stopDotCenterX = width - stopDotRadius - 1.dp.toPx()
            drawCircle(
                color = trackColor.copy(alpha = 0.85f),
                radius = stopDotRadius,
                center = Offset(stopDotCenterX, centerY)
            )

            // 2. Draw Inactive Track with M3 separation gap
            val trackStart = if (activeEnd > strokeWidth / 2f) {
                (activeEnd + gap).coerceAtMost(width)
            } else {
                strokeWidth / 2f
            }
            val trackEnd = (stopDotCenterX - stopDotRadius - gap).coerceAtLeast(trackStart)
            if (trackEnd > trackStart) {
                drawLine(
                    color = trackColor.copy(alpha = 0.55f),
                    start = Offset(trackStart, centerY),
                    end = Offset(trackEnd, centerY),
                    strokeWidth = trackStrokeWidth,
                    cap = StrokeCap.Round
                )
            }

            // 3. Draw M3 Expressive Undulating Active Wave Stroke
            if (activeEnd > strokeWidth / 2f) {
                val activeWavePath = Path()
                val startX = strokeWidth / 2f
                val startY = centerY + kotlin.math.sin(startX * frequency + wavePhase) * targetAmplitude
                activeWavePath.moveTo(startX, startY)

                val step = 2.dp.toPx()
                var x = startX + step
                while (x < activeEnd) {
                    val y = centerY + kotlin.math.sin(x * frequency + wavePhase) * targetAmplitude
                    activeWavePath.lineTo(x, y)
                    x += step
                }
                val endY = centerY + kotlin.math.sin(activeEnd * frequency + wavePhase) * targetAmplitude
                activeWavePath.lineTo(activeEnd, endY)

                // Vibrant photoperiod gradient brush along the active wave
                val waveBrush = if (isLightsOn) {
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFFFB300), // Amber gold
                            baseColor,          // Radiant sunlight
                            Color(0xFF81C784)   // Canopy emerald
                        ),
                        startX = 0f,
                        endX = activeEnd.coerceAtLeast(1f)
                    )
                } else {
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF3949AB), // Indigo
                            baseColor,          // Celestial blue
                            Color(0xFF80D8FF)   // Cyan aurora
                        ),
                        startX = 0f,
                        endX = activeEnd.coerceAtLeast(1f)
                    )
                }

                drawPath(
                    path = activeWavePath,
                    brush = waveBrush,
                    style = Stroke(
                        width = strokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    }
}

@Composable
private fun WavyActiveDotIndicator(
    isRunning: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    if (isRunning) {
        val infiniteTransition = rememberInfiniteTransition(label = "dot_wave")
        val waveScale by infiniteTransition.animateFloat(
            initialValue = 0.8f,
            targetValue = 1.9f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "wave_scale"
        )
        val waveAlpha by infiniteTransition.animateFloat(
            initialValue = 0.65f,
            targetValue = 0.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "wave_alpha"
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier.size(16.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = color.copy(alpha = waveAlpha),
                modifier = Modifier
                    .size(10.dp)
                    .graphicsLayer(scaleX = waveScale, scaleY = waveScale)
            ) {}
            Surface(
                shape = CircleShape,
                color = color,
                modifier = Modifier.size(8.dp)
            ) {}
        }
    } else {
        Surface(
            shape = CircleShape,
            color = color,
            modifier = modifier.size(8.dp)
        ) {}
    }
}
