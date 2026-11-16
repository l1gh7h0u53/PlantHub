package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val PlantDarkColorScheme = darkColorScheme(
    primary = PlantGreenPrimaryDark,
    onPrimary = PlantGreenOnPrimaryDark,
    primaryContainer = PlantGreenContainerDark,
    onPrimaryContainer = PlantGreenOnContainerDark,
    secondary = PlantSageSecondaryDark,
    onSecondary = PlantSageOnSecondaryDark,
    secondaryContainer = PlantSageContainerDark,
    onSecondaryContainer = PlantSageOnContainerDark,
    tertiary = PlantAmberDark,
    onTertiary = PlantAmberOnDark,
    tertiaryContainer = PlantAmberContainerDark,
    onTertiaryContainer = PlantAmberOnContainerDark,
    background = PlantDarkBackground,
    onBackground = Color(0xFFF0F6FC),
    surface = PlantDarkSurface,
    onSurface = Color(0xFFF0F6FC),
    surfaceVariant = PlantDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFD0D7DE),
    outline = PlantDarkOutline,
    outlineVariant = PlantDarkOutlineVariant
)

val PlantLightColorScheme = lightColorScheme(
    primary = PlantGreenPrimaryLight,
    onPrimary = PlantGreenOnPrimaryLight,
    primaryContainer = PlantGreenContainerLight,
    onPrimaryContainer = PlantGreenOnContainerLight,
    secondary = PlantSageSecondaryLight,
    onSecondary = PlantSageOnSecondaryLight,
    secondaryContainer = PlantSageContainerLight,
    onSecondaryContainer = PlantSageOnContainerLight,
    tertiary = PlantAmberLight,
    onTertiary = PlantAmberOnLight,
    tertiaryContainer = PlantAmberContainerLight,
    onTertiaryContainer = PlantAmberOnContainerLight,
    background = PlantLightBackground,
    onBackground = Color(0xFF1F2328),
    surface = PlantLightSurface,
    onSurface = Color(0xFF1F2328),
    surfaceVariant = PlantLightSurfaceVariant,
    onSurfaceVariant = Color(0xFF656D76),
    outline = PlantLightOutline,
    outlineVariant = PlantLightOutlineVariant
)

/**
 * Resolves a platform-specific dynamic color scheme (e.g. Android 12+ Monet wallpaper palette
 * wrapped with high-contrast text guarantees) or returns null to use PlantHub baseline palettes.
 */
@Composable
expect fun platformDynamicColorScheme(darkTheme: Boolean): ColorScheme?

@Composable
fun PlantHubTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val dynamicScheme = if (dynamicColor) platformDynamicColorScheme(darkTheme) else null
    val colorScheme = dynamicScheme ?: if (darkTheme) PlantDarkColorScheme else PlantLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
