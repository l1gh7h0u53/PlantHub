package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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

/**
 * Root Material 3 Expressive theme for PlantHub across Android, Desktop, and iOS.
 *
 * Automatically integrates the OS-synchronized [AccessibilitySuiteTokens] layer via
 * [rememberSystemAccessibilityState]:
 * - When system Color Correction is OFF, uses sanitized Monet dynamic theming (if enabled)
 *   or PlantHub's baseline emerald/amber high-contrast schemes.
 * - When system Color Correction is ON (Protan, Deutan, Tritan, Monochromacy), automatically
 *   bypasses dynamic wallpaper tints to guarantee non-collapsing semantic separation, and injects
 *   vision-calibrated tokens into [LocalAccessibilitySuite].
 * - When OS High Contrast Text is ON, tightens foreground ink and border contrast across all roles.
 */
@Composable
fun PlantHubTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    accessibilityOverride: SystemAccessibilityState? = null,
    content: @Composable () -> Unit
) {
    val systemA11y = accessibilityOverride ?: rememberSystemAccessibilityState()
    val useMonet = dynamicColor && systemA11y.colorVisionMode == ColorVisionMode.NONE
    val dynamicScheme = if (useMonet) platformDynamicColorScheme(darkTheme) else null
    val baseScheme = dynamicScheme ?: if (darkTheme) PlantDarkColorScheme else PlantLightColorScheme
    val accessibleScheme = buildAccessibleColorScheme(
        state = systemA11y,
        darkTheme = darkTheme,
        baseScheme = baseScheme
    )
    val suiteTokens = resolveAccessibilityTokens(
        state = systemA11y,
        darkTheme = darkTheme
    )

    CompositionLocalProvider(LocalAccessibilitySuite provides suiteTokens) {
        MaterialTheme(
            colorScheme = accessibleScheme,
            typography = Typography,
            content = content
        )
    }
}
