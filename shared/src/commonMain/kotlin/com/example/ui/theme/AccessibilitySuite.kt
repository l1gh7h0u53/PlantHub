package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Vision deficiency / color correction modes automatically mirrored from the host
 * operating system's Accessibility settings (such as Android Settings -> Accessibility ->
 * Color and motion -> Color correction / Daltonizer).
 *
 * DESIGN PHILOSOPHY:
 * PlantHub does not expose an in-app colorblindness switch or promote accessibility as a
 * marketing headline. Instead, the suite stays invisible under normal system settings and
 * automatically activates the matching scientifically calibrated palette, non-color geometric
 * pattern markers, high-contrast strokes, and enriched screen-reader semantics whenever the
 * user has configured accessibility preferences at the OS level.
 */
enum class ColorVisionMode {
    /** Standard PlantHub visual styling and sanitized Monet dynamic colors. */
    NONE,

    /** Red-weak (Protanomaly / Protanopia, Android daltonizer code 11). */
    PROTAN,

    /** Green-weak (Deuteranomaly / Deuteranopia, Android daltonizer code 12). */
    DEUTAN,

    /** Blue-weak (Tritanomaly / Tritanopia, Android daltonizer code 13). */
    TRITAN,

    /** Complete color deficiency / grayscale (Monochromacy, Android daltonizer code 0). */
    MONOCHROMACY
}

/**
 * Immutable snapshot of OS-level accessibility preferences observed at runtime.
 *
 * @property colorVisionMode Active system color-correction mode, or [ColorVisionMode.NONE].
 * @property highContrastText True when OS High Contrast Text is enabled, forcing maximal ink contrast and crisp borders.
 * @property screenReaderActive True when TalkBack / Touch Exploration or accessibility services are active.
 */
@Immutable
data class SystemAccessibilityState(
    val colorVisionMode: ColorVisionMode = ColorVisionMode.NONE,
    val highContrastText: Boolean = false,
    val screenReaderActive: Boolean = false
)

/**
 * Semantic design tokens resolved dynamically from [SystemAccessibilityState] and active theme luminance.
 *
 * Provides colorblind-safe Okabe-Ito / Tol palettes alongside non-color pattern flags so every
 * heatmap intensity cell, light phase indicator, care badge, milestone pill, and timeline chart
 * remains unambiguous regardless of color vision profile.
 */
@Immutable
data class AccessibilitySuiteTokens(
    val state: SystemAccessibilityState,
    val isDark: Boolean,
    val isColorCorrectionActive: Boolean,
    val useNonColorPatterns: Boolean,
    val borderStrokeWidthDp: Dp,
    // GitHub-style Heatmap levels (0 = empty, 1..4 = increasing activity density)
    val heatmapLevel0: Color,
    val heatmapLevel1: Color,
    val heatmapLevel2: Color,
    val heatmapLevel3: Color,
    val heatmapLevel4: Color,
    // Photoperiod Light Phase HUD & Wave Gauge
    val lightsOnAccent: Color,
    val lightsOnBadgeBg: Color,
    val lightsOnBadgeContent: Color,
    val lightsOnGradientStart: Color,
    val lightsOnGradientEnd: Color,
    val lightsOffAccent: Color,
    val lightsOffBadgeBg: Color,
    val lightsOffBadgeContent: Color,
    val lightsOffGradientStart: Color,
    val lightsOffGradientEnd: Color,
    val maintenanceAccent: Color,
    val maintenanceBadgeBg: Color,
    val maintenanceBadgeContent: Color,
    val maintenanceCtaBg: Color,
    val maintenanceCtaContent: Color,
    // Care Action Accents (Quick Care Bar, Timeline, Bottom Sheet)
    val waterAccent: Color,
    val waterBg: Color,
    val mistAccent: Color,
    val mistBg: Color,
    val fertilizeAccent: Color,
    val fertilizeBg: Color,
    val defoliateAccent: Color,
    val defoliateBg: Color,
    val trainAccent: Color,
    val trainBg: Color,
    // High-Contrast Filled Status Pills (Stage Badges, Day Badges, Milestone Locks)
    val stageBadgeBg: Color,
    val stageBadgeContent: Color,
    val dayBadgeBg: Color,
    val dayBadgeContent: Color,
    val milestoneBadgeBg: Color,
    val milestoneBadgeContent: Color,
    val milestoneBorderColor: Color,
    val destructiveCtaBg: Color,
    val destructiveCtaContent: Color,
    // Photo Vault Analytics Chart
    val chartCurveColor: Color,
    val chartMilestoneNodeColor: Color,
    val chartStandardNodeColor: Color
) {
    /** Maps care heatmap activity level [0..4] to the active vision-calibrated swatch. */
    fun heatmapColor(level: Int): Color = when (level) {
        1 -> heatmapLevel1
        2 -> heatmapLevel2
        3 -> heatmapLevel3
        4 -> heatmapLevel4
        else -> heatmapLevel0
    }
}

/**
 * CompositionLocal delivering the active [AccessibilitySuiteTokens] down the Compose hierarchy.
 */
val LocalAccessibilitySuite = staticCompositionLocalOf {
    resolveAccessibilityTokens(SystemAccessibilityState(), darkTheme = true)
}

/**
 * Observes host operating system accessibility settings (Color Correction / Daltonizer,
 * High Contrast Text, and screen reader availability) cleanly across KMP targets.
 */
@Composable
expect fun rememberSystemAccessibilityState(): SystemAccessibilityState

/**
 * Resolves the complete Material 3 [ColorScheme] adapted for the detected OS [SystemAccessibilityState].
 *
 * When [SystemAccessibilityState.colorVisionMode] is active, dynamic wallpaper colors (Monet) are
 * intentionally bypassed in favor of scientifically validated high-separation palettes so OS-level
 * daltonization never collapses semantic contrast. When [SystemAccessibilityState.highContrastText]
 * is enabled, surface ink and outline roles are tightened to maximal WCAG AAA contrast.
 */
fun buildAccessibleColorScheme(
    state: SystemAccessibilityState,
    darkTheme: Boolean,
    baseScheme: ColorScheme
): ColorScheme {
    val paletteScheme = when (state.colorVisionMode) {
        ColorVisionMode.NONE -> baseScheme
        // Okabe-Ito Blue + Gold/Orange palette: immune to Red-Green (Protan/Deutan) confusion
        ColorVisionMode.PROTAN,
        ColorVisionMode.DEUTAN -> if (darkTheme) {
            darkColorScheme(
                primary = Color(0xFF56B4E9),
                onPrimary = Color(0xFF001E2E),
                primaryContainer = Color(0xFF004C77),
                onPrimaryContainer = Color(0xFFD6F0FF),
                secondary = Color(0xFFE69F00),
                onSecondary = Color(0xFF281800),
                secondaryContainer = Color(0xFF5A3B00),
                onSecondaryContainer = Color(0xFFFFE4A8),
                tertiary = Color(0xFFF0E442),
                onTertiary = Color(0xFF252100),
                tertiaryContainer = Color(0xFF4D4600),
                onTertiaryContainer = Color(0xFFFFF8AE),
                error = Color(0xFFFF8A50),
                onError = Color(0xFF2C0D00),
                errorContainer = Color(0xFF7A2800),
                onErrorContainer = Color(0xFFFFDFD1),
                background = PlantDarkBackground,
                onBackground = Color(0xFFF0F6FC),
                surface = PlantDarkSurface,
                onSurface = Color(0xFFF0F6FC),
                surfaceVariant = PlantDarkSurfaceVariant,
                onSurfaceVariant = Color(0xFFD8E0E8),
                outline = Color(0xFF6B7785),
                outlineVariant = Color(0xFF3A434D)
            )
        } else {
            lightColorScheme(
                primary = Color(0xFF005B94),
                onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFCDE9FF),
                onPrimaryContainer = Color(0xFF001D33),
                secondary = Color(0xFF8C5300),
                onSecondary = Color(0xFFFFFFFF),
                secondaryContainer = Color(0xFFFFDEA8),
                onSecondaryContainer = Color(0xFF2D1700),
                tertiary = Color(0xFF665A00),
                onTertiary = Color(0xFFFFFFFF),
                tertiaryContainer = Color(0xFFF5E678),
                onTertiaryContainer = Color(0xFF211C00),
                error = Color(0xFFA83200),
                onError = Color(0xFFFFFFFF),
                errorContainer = Color(0xFFFFDBD0),
                onErrorContainer = Color(0xFF380D00),
                background = PlantLightBackground,
                onBackground = Color(0xFF111418),
                surface = PlantLightSurface,
                onSurface = Color(0xFF111418),
                surfaceVariant = PlantLightSurfaceVariant,
                onSurfaceVariant = Color(0xFF3E454D),
                outline = Color(0xFF747E88),
                outlineVariant = Color(0xFFB8C0C8)
            )
        }
        // Deep Teal + Crimson Rose palette: immune to Blue-Yellow (Tritan) confusion
        ColorVisionMode.TRITAN -> if (darkTheme) {
            darkColorScheme(
                primary = Color(0xFF33D6C4),
                onPrimary = Color(0xFF00211D),
                primaryContainer = Color(0xFF005248),
                onPrimaryContainer = Color(0xFFB8FFF4),
                secondary = Color(0xFFFF7AA2),
                onSecondary = Color(0xFF380016),
                secondaryContainer = Color(0xFF701438),
                onSecondaryContainer = Color(0xFFFFD8E4),
                tertiary = Color(0xFFFF9671),
                onTertiary = Color(0xFF330D00),
                tertiaryContainer = Color(0xFF682810),
                onTertiaryContainer = Color(0xFFFFDFD3),
                error = Color(0xFFFF7AA2),
                onError = Color(0xFF380016),
                errorContainer = Color(0xFF701438),
                onErrorContainer = Color(0xFFFFD8E4),
                background = PlantDarkBackground,
                onBackground = Color(0xFFF0F6FC),
                surface = PlantDarkSurface,
                onSurface = Color(0xFFF0F6FC),
                surfaceVariant = PlantDarkSurfaceVariant,
                onSurfaceVariant = Color(0xFFD8E0E8),
                outline = Color(0xFF6B7785),
                outlineVariant = Color(0xFF3A434D)
            )
        } else {
            lightColorScheme(
                primary = Color(0xFF006B60),
                onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFB2F5EA),
                onPrimaryContainer = Color(0xFF00201C),
                secondary = Color(0xFFA61B4D),
                onSecondary = Color(0xFFFFFFFF),
                secondaryContainer = Color(0xFFFFD9E3),
                onSecondaryContainer = Color(0xFF3B0015),
                tertiary = Color(0xFF9C3E18),
                onTertiary = Color(0xFFFFFFFF),
                tertiaryContainer = Color(0xFFFFDBCE),
                onTertiaryContainer = Color(0xFF360E00),
                error = Color(0xFFA61B4D),
                onError = Color(0xFFFFFFFF),
                errorContainer = Color(0xFFFFD9E3),
                onErrorContainer = Color(0xFF3B0015),
                background = PlantLightBackground,
                onBackground = Color(0xFF111418),
                surface = PlantLightSurface,
                onSurface = Color(0xFF111418),
                surfaceVariant = PlantLightSurfaceVariant,
                onSurfaceVariant = Color(0xFF3E454D),
                outline = Color(0xFF747E88),
                outlineVariant = Color(0xFFB8C0C8)
            )
        }
        // Grayscale / Monochromacy: purely luminance-stepped contrast
        ColorVisionMode.MONOCHROMACY -> if (darkTheme) {
            darkColorScheme(
                primary = Color(0xFFFFFFFF),
                onPrimary = Color(0xFF000000),
                primaryContainer = Color(0xFFD9DEE4),
                onPrimaryContainer = Color(0xFF050709),
                secondary = Color(0xFFC0C7D0),
                onSecondary = Color(0xFF0A0D10),
                secondaryContainer = Color(0xFF30363D),
                onSecondaryContainer = Color(0xFFF5F8FA),
                tertiary = Color(0xFFEAEFF4),
                onTertiary = Color(0xFF080A0C),
                tertiaryContainer = Color(0xFF3D444D),
                onTertiaryContainer = Color(0xFFFFFFFF),
                background = Color(0xFF090B0E),
                onBackground = Color(0xFFFFFFFF),
                surface = Color(0xFF12161B),
                onSurface = Color(0xFFFFFFFF),
                surfaceVariant = Color(0xFF222830),
                onSurfaceVariant = Color(0xFFE6ECF2),
                outline = Color(0xFF9EA7B3),
                outlineVariant = Color(0xFF5A636E)
            )
        } else {
            lightColorScheme(
                primary = Color(0xFF0D1117),
                onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFF24292F),
                onPrimaryContainer = Color(0xFFFFFFFF),
                secondary = Color(0xFF424A53),
                onSecondary = Color(0xFFFFFFFF),
                secondaryContainer = Color(0xFFD8DEE4),
                onSecondaryContainer = Color(0xFF0D1117),
                tertiary = Color(0xFF2F363D),
                onTertiary = Color(0xFFFFFFFF),
                tertiaryContainer = Color(0xFFE1E6EB),
                onTertiaryContainer = Color(0xFF0D1117),
                background = Color(0xFFFFFFFF),
                onBackground = Color(0xFF000000),
                surface = Color(0xFFFFFFFF),
                onSurface = Color(0xFF000000),
                surfaceVariant = Color(0xFFEAEEF2),
                onSurfaceVariant = Color(0xFF1F2328),
                outline = Color(0xFF424A53),
                outlineVariant = Color(0xFF8B949E)
            )
        }
    }

    return if (state.highContrastText) {
        if (darkTheme) {
            paletteScheme.copy(
                onBackground = Color(0xFFFFFFFF),
                onSurface = Color(0xFFFFFFFF),
                onSurfaceVariant = Color(0xFFF0F6FC),
                outline = Color(0xFFB1BAC4),
                outlineVariant = Color(0xFF6E7681)
            )
        } else {
            paletteScheme.copy(
                onBackground = Color(0xFF000000),
                onSurface = Color(0xFF000000),
                onSurfaceVariant = Color(0xFF1F2328),
                outline = Color(0xFF24292F),
                outlineVariant = Color(0xFF57606A)
            )
        }
    } else {
        paletteScheme
    }
}

/**
 * Maps the detected [ColorVisionMode] and [darkTheme] to full semantic UI, chart, heatmap,
 * light-phase, and status-pill color tokens consumed across shared and Android screens.
 */
fun resolveAccessibilityTokens(
    state: SystemAccessibilityState,
    darkTheme: Boolean
): AccessibilitySuiteTokens {
    val active = state.colorVisionMode != ColorVisionMode.NONE
    val strokeWidth = if (state.highContrastText) 1.5.dp else 1.dp

    return when (state.colorVisionMode) {
        ColorVisionMode.NONE -> AccessibilitySuiteTokens(
            state = state,
            isDark = darkTheme,
            isColorCorrectionActive = false,
            useNonColorPatterns = false,
            borderStrokeWidthDp = strokeWidth,
            heatmapLevel0 = if (darkTheme) GithubHeatmapColors.DarkLevel0 else GithubHeatmapColors.LightLevel0,
            heatmapLevel1 = if (darkTheme) GithubHeatmapColors.DarkLevel1 else GithubHeatmapColors.LightLevel1,
            heatmapLevel2 = if (darkTheme) GithubHeatmapColors.DarkLevel2 else GithubHeatmapColors.LightLevel2,
            heatmapLevel3 = if (darkTheme) GithubHeatmapColors.DarkLevel3 else GithubHeatmapColors.LightLevel3,
            heatmapLevel4 = if (darkTheme) GithubHeatmapColors.DarkLevel4 else GithubHeatmapColors.LightLevel4,
            lightsOnAccent = GithubHeatmapColors.LightsOn,
            lightsOnBadgeBg = Color(0xFF332A00),
            lightsOnBadgeContent = GithubHeatmapColors.LightsOn,
            lightsOnGradientStart = Color(0xFFFFB300),
            lightsOnGradientEnd = Color(0xFF81C784),
            lightsOffAccent = Color(0xFF90CAF9),
            lightsOffBadgeBg = Color(0xFF1E2836),
            lightsOffBadgeContent = Color(0xFF90CAF9),
            lightsOffGradientStart = Color(0xFF3949AB),
            lightsOffGradientEnd = Color(0xFF80D8FF),
            maintenanceAccent = Color(0xFFFFB74D),
            maintenanceBadgeBg = Color(0xFF38260D),
            maintenanceBadgeContent = Color(0xFFFFB74D),
            maintenanceCtaBg = Color(0xFFFFB74D),
            maintenanceCtaContent = Color(0xFF201505),
            waterAccent = GithubHeatmapColors.WaterBlue,
            waterBg = Color(0xFF0D2538),
            mistAccent = GithubHeatmapColors.MistTeal,
            mistBg = Color(0xFF0D2E2B),
            fertilizeAccent = GithubHeatmapColors.NutrientPurple,
            fertilizeBg = Color(0xFF26122E),
            defoliateAccent = Color(0xFFFFB74D),
            defoliateBg = Color(0xFF332014),
            trainAccent = Color(0xFF81C784),
            trainBg = Color(0xFF142B1A),
            stageBadgeBg = Color(0xFFA3F5B1),
            stageBadgeContent = Color(0xFF00210B),
            dayBadgeBg = Color(0xFFA3F5B1),
            dayBadgeContent = Color(0xFF00210B),
            milestoneBadgeBg = Color(0xFFF6BE48),
            milestoneBadgeContent = Color(0xFF261900),
            milestoneBorderColor = Color(0xFFD4971A),
            destructiveCtaBg = Color(0xFFDC2626),
            destructiveCtaContent = Color(0xFFFFFFFF),
            chartCurveColor = if (darkTheme) PlantGreenPrimaryDark else PlantGreenPrimaryLight,
            chartMilestoneNodeColor = if (darkTheme) PlantAmberDark else PlantAmberLight,
            chartStandardNodeColor = if (darkTheme) PlantSageSecondaryDark else PlantSageSecondaryLight
        )

        // Okabe-Ito Blue / Cyan / Amber-Gold palette (zero Red-Green overlap)
        ColorVisionMode.PROTAN,
        ColorVisionMode.DEUTAN -> AccessibilitySuiteTokens(
            state = state,
            isDark = darkTheme,
            isColorCorrectionActive = active,
            useNonColorPatterns = true,
            borderStrokeWidthDp = strokeWidth,
            heatmapLevel0 = if (darkTheme) Color(0xFF161B22) else Color(0xFFEAEEF2),
            heatmapLevel1 = if (darkTheme) Color(0xFF0F3554) else Color(0xFFC5E6FA),
            heatmapLevel2 = if (darkTheme) Color(0xFF005A8D) else Color(0xFF7AC2EE),
            heatmapLevel3 = if (darkTheme) Color(0xFF2B8CBE) else Color(0xFF0072B2),
            heatmapLevel4 = if (darkTheme) Color(0xFF56B4E9) else Color(0xFF004775),
            lightsOnAccent = Color(0xFFF0E442),
            lightsOnBadgeBg = Color(0xFF2B2600),
            lightsOnBadgeContent = Color(0xFFF0E442),
            lightsOnGradientStart = Color(0xFFE69F00),
            lightsOnGradientEnd = Color(0xFFF0E442),
            lightsOffAccent = Color(0xFF56B4E9),
            lightsOffBadgeBg = Color(0xFF0A2438),
            lightsOffBadgeContent = Color(0xFF7FD0FA),
            lightsOffGradientStart = Color(0xFF004C77),
            lightsOffGradientEnd = Color(0xFF56B4E9),
            maintenanceAccent = Color(0xFFE69F00),
            maintenanceBadgeBg = Color(0xFF332000),
            maintenanceBadgeContent = Color(0xFFFFC145),
            maintenanceCtaBg = Color(0xFFE69F00),
            maintenanceCtaContent = Color(0xFF1F1200),
            waterAccent = Color(0xFF56B4E9),
            waterBg = Color(0xFF0B2538),
            mistAccent = Color(0xFF9CD9FF),
            mistBg = Color(0xFF142C3D),
            fertilizeAccent = Color(0xFFE69F00),
            fertilizeBg = Color(0xFF2E1E00),
            defoliateAccent = Color(0xFFD55E00),
            defoliateBg = Color(0xFF331600),
            trainAccent = Color(0xFFF0E442),
            trainBg = Color(0xFF2A2600),
            stageBadgeBg = Color(0xFF9AD9FA),
            stageBadgeContent = Color(0xFF001A29),
            dayBadgeBg = Color(0xFF9AD9FA),
            dayBadgeContent = Color(0xFF001A29),
            milestoneBadgeBg = Color(0xFFFFCF5C),
            milestoneBadgeContent = Color(0xFF241700),
            milestoneBorderColor = Color(0xFFE69F00),
            destructiveCtaBg = Color(0xFFD55E00),
            destructiveCtaContent = Color(0xFFFFFFFF),
            chartCurveColor = Color(0xFF56B4E9),
            chartMilestoneNodeColor = Color(0xFFE69F00),
            chartStandardNodeColor = Color(0xFFF0E442)
        )

        // Paul Tol Teal / Crimson Rose / Coral palette (zero Blue-Yellow overlap)
        ColorVisionMode.TRITAN -> AccessibilitySuiteTokens(
            state = state,
            isDark = darkTheme,
            isColorCorrectionActive = active,
            useNonColorPatterns = true,
            borderStrokeWidthDp = strokeWidth,
            heatmapLevel0 = if (darkTheme) Color(0xFF161B22) else Color(0xFFEAEEF2),
            heatmapLevel1 = if (darkTheme) Color(0xFF0B3B36) else Color(0xFFBDF2EB),
            heatmapLevel2 = if (darkTheme) Color(0xFF00665C) else Color(0xFF6BD9CC),
            heatmapLevel3 = if (darkTheme) Color(0xFF009988) else Color(0xFF008072),
            heatmapLevel4 = if (darkTheme) Color(0xFF33D6C4) else Color(0xFF004D45),
            lightsOnAccent = Color(0xFFFF9671),
            lightsOnBadgeBg = Color(0xFF361408),
            lightsOnBadgeContent = Color(0xFFFFB296),
            lightsOnGradientStart = Color(0xFFEE7733),
            lightsOnGradientEnd = Color(0xFFFFB296),
            lightsOffAccent = Color(0xFF33D6C4),
            lightsOffBadgeBg = Color(0xFF082925),
            lightsOffBadgeContent = Color(0xFF7CEBE0),
            lightsOffGradientStart = Color(0xFF005950),
            lightsOffGradientEnd = Color(0xFF33D6C4),
            maintenanceAccent = Color(0xFFFF7AA2),
            maintenanceBadgeBg = Color(0xFF380D1D),
            maintenanceBadgeContent = Color(0xFFFFB0C8),
            maintenanceCtaBg = Color(0xFFFF7AA2),
            maintenanceCtaContent = Color(0xFF2B000E),
            waterAccent = Color(0xFF33D6C4),
            waterBg = Color(0xFF092A26),
            mistAccent = Color(0xFF8DF2E6),
            mistBg = Color(0xFF10332F),
            fertilizeAccent = Color(0xFFFF7AA2),
            fertilizeBg = Color(0xFF330F1D),
            defoliateAccent = Color(0xFFEE7733),
            defoliateBg = Color(0xFF331808),
            trainAccent = Color(0xFFB8FFF4),
            trainBg = Color(0xFF0E2B27),
            stageBadgeBg = Color(0xFF9DF2E8),
            stageBadgeContent = Color(0xFF00211D),
            dayBadgeBg = Color(0xFF9DF2E8),
            dayBadgeContent = Color(0xFF00211D),
            milestoneBadgeBg = Color(0xFFFFB0C8),
            milestoneBadgeContent = Color(0xFF330012),
            milestoneBorderColor = Color(0xFFCC3366),
            destructiveCtaBg = Color(0xFFCC3366),
            destructiveCtaContent = Color(0xFFFFFFFF),
            chartCurveColor = Color(0xFF33D6C4),
            chartMilestoneNodeColor = Color(0xFFFF7AA2),
            chartStandardNodeColor = Color(0xFFEE7733)
        )

        // Monochromacy: maximal grayscale luminance stepping + crisp structural contrast
        ColorVisionMode.MONOCHROMACY -> AccessibilitySuiteTokens(
            state = state,
            isDark = darkTheme,
            isColorCorrectionActive = active,
            useNonColorPatterns = true,
            borderStrokeWidthDp = 1.5.dp,
            heatmapLevel0 = if (darkTheme) Color(0xFF14171C) else Color(0xFFF2F4F7),
            heatmapLevel1 = if (darkTheme) Color(0xFF3A414A) else Color(0xFFCFD6DE),
            heatmapLevel2 = if (darkTheme) Color(0xFF6B7480) else Color(0xFF95A0AC),
            heatmapLevel3 = if (darkTheme) Color(0xFFAAB3BD) else Color(0xFF525B66),
            heatmapLevel4 = if (darkTheme) Color(0xFFFFFFFF) else Color(0xFF14171C),
            lightsOnAccent = Color(0xFFFFFFFF),
            lightsOnBadgeBg = Color(0xFF282D34),
            lightsOnBadgeContent = Color(0xFFFFFFFF),
            lightsOnGradientStart = Color(0xFFBFC7D0),
            lightsOnGradientEnd = Color(0xFFFFFFFF),
            lightsOffAccent = Color(0xFFC2CAD3),
            lightsOffBadgeBg = Color(0xFF14181D),
            lightsOffBadgeContent = Color(0xFFE6ECF2),
            lightsOffGradientStart = Color(0xFF5A636E),
            lightsOffGradientEnd = Color(0xFFC2CAD3),
            maintenanceAccent = Color(0xFFF0F4F8),
            maintenanceBadgeBg = Color(0xFF2B313A),
            maintenanceBadgeContent = Color(0xFFFFFFFF),
            maintenanceCtaBg = Color(0xFFFFFFFF),
            maintenanceCtaContent = Color(0xFF000000),
            waterAccent = Color(0xFFFFFFFF),
            waterBg = Color(0xFF22272E),
            mistAccent = Color(0xFFE0E5EB),
            mistBg = Color(0xFF1C2026),
            fertilizeAccent = Color(0xFFD0D6DE),
            fertilizeBg = Color(0xFF282E36),
            defoliateAccent = Color(0xFFF5F7FA),
            defoliateBg = Color(0xFF2A3038),
            trainAccent = Color(0xFFEAEFF4),
            trainBg = Color(0xFF1E2329),
            stageBadgeBg = Color(0xFFFFFFFF),
            stageBadgeContent = Color(0xFF000000),
            dayBadgeBg = Color(0xFFFFFFFF),
            dayBadgeContent = Color(0xFF000000),
            milestoneBadgeBg = Color(0xFFD9DEE4),
            milestoneBadgeContent = Color(0xFF000000),
            milestoneBorderColor = Color(0xFF9EA7B3),
            destructiveCtaBg = Color(0xFF14171C),
            destructiveCtaContent = Color(0xFFFFFFFF),
            chartCurveColor = if (darkTheme) Color(0xFFFFFFFF) else Color(0xFF14171C),
            chartMilestoneNodeColor = if (darkTheme) Color(0xFFD0D7DE) else Color(0xFF3A414A),
            chartStandardNodeColor = if (darkTheme) Color(0xFF8B949E) else Color(0xFF6E7681)
        )
    }
}
