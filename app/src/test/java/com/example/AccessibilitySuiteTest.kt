package com.example

import android.content.Context
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.GrowthStage
import com.example.data.model.LightCycleStatus
import com.example.data.model.PhotoEntry
import com.example.ui.components.GithubHeatmapGrid
import com.example.ui.components.LightCycleGauge
import com.example.ui.components.QuickCareBar
import com.example.ui.components.VaultElapsedDaysAnalyticsCard
import com.example.ui.theme.ColorVisionMode
import com.example.ui.theme.LocalAccessibilitySuite
import com.example.ui.theme.PlantDarkColorScheme
import com.example.ui.theme.PlantHubTheme
import com.example.ui.theme.PlantLightColorScheme
import com.example.ui.theme.SystemAccessibilityState
import com.example.ui.theme.buildAccessibleColorScheme
import com.example.ui.theme.readAndroidAccessibilityState
import com.example.ui.theme.resolveAccessibilityTokens
import com.example.ui.viewmodel.HeatmapViewMode
import com.example.util.HeatmapDay
import com.example.util.HeatmapGridData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AccessibilitySuiteTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `readAndroidAccessibilityState maps OS daltonizer and high contrast text automatically`() {
        val resolver = context.contentResolver

        // Disabled daltonizer -> NONE
        Settings.Secure.putInt(resolver, "accessibility_display_daltonizer_enabled", 0)
        Settings.Secure.putInt(resolver, "accessibility_display_daltonizer", 12)
        Settings.Secure.putInt(resolver, "high_text_contrast_enabled", 0)
        val baseline = readAndroidAccessibilityState(context)
        assertEquals(ColorVisionMode.NONE, baseline.colorVisionMode)
        assertFalse(baseline.highContrastText)

        // Protan (11)
        Settings.Secure.putInt(resolver, "accessibility_display_daltonizer_enabled", 1)
        Settings.Secure.putInt(resolver, "accessibility_display_daltonizer", 11)
        assertEquals(ColorVisionMode.PROTAN, readAndroidAccessibilityState(context).colorVisionMode)

        // Deutan (12) + High Contrast Text
        Settings.Secure.putInt(resolver, "accessibility_display_daltonizer", 12)
        Settings.Secure.putInt(resolver, "high_text_contrast_enabled", 1)
        val deutanHighContrast = readAndroidAccessibilityState(context)
        assertEquals(ColorVisionMode.DEUTAN, deutanHighContrast.colorVisionMode)
        assertTrue(deutanHighContrast.highContrastText)

        // Tritan (13)
        Settings.Secure.putInt(resolver, "accessibility_display_daltonizer", 13)
        assertEquals(ColorVisionMode.TRITAN, readAndroidAccessibilityState(context).colorVisionMode)

        // Monochromacy (0)
        Settings.Secure.putInt(resolver, "accessibility_display_daltonizer", 0)
        assertEquals(ColorVisionMode.MONOCHROMACY, readAndroidAccessibilityState(context).colorVisionMode)
    }

    @Test
    fun `calibrated palettes and non-color pattern flags resolve distinctly across vision modes`() {
        val normalTokens = resolveAccessibilityTokens(SystemAccessibilityState(), darkTheme = true)
        assertFalse(normalTokens.isColorCorrectionActive)
        assertFalse(normalTokens.useNonColorPatterns)
        assertEquals(1.dp, normalTokens.borderStrokeWidthDp)

        val protanTokens = resolveAccessibilityTokens(
            SystemAccessibilityState(colorVisionMode = ColorVisionMode.PROTAN, highContrastText = true),
            darkTheme = true
        )
        val tritanTokens = resolveAccessibilityTokens(
            SystemAccessibilityState(colorVisionMode = ColorVisionMode.TRITAN),
            darkTheme = true
        )
        val monoTokens = resolveAccessibilityTokens(
            SystemAccessibilityState(colorVisionMode = ColorVisionMode.MONOCHROMACY),
            darkTheme = false
        )

        assertTrue(protanTokens.isColorCorrectionActive)
        assertTrue(protanTokens.useNonColorPatterns)
        assertEquals(1.5.dp, protanTokens.borderStrokeWidthDp)
        assertNotEquals(normalTokens.heatmapLevel4, protanTokens.heatmapLevel4)
        assertNotEquals(protanTokens.heatmapLevel4, tritanTokens.heatmapLevel4)
        assertNotEquals(tritanTokens.lightsOnAccent, protanTokens.lightsOnAccent)
        assertEquals(Color(0xFF14171C), monoTokens.heatmapLevel4)

        val darkHighContrastScheme = buildAccessibleColorScheme(
            state = SystemAccessibilityState(colorVisionMode = ColorVisionMode.DEUTAN, highContrastText = true),
            darkTheme = true,
            baseScheme = PlantDarkColorScheme
        )
        assertEquals(Color(0xFFFFFFFF), darkHighContrastScheme.onBackground)
        assertEquals(Color(0xFFFFFFFF), darkHighContrastScheme.onSurface)

        val lightHighContrastScheme = buildAccessibleColorScheme(
            state = SystemAccessibilityState(colorVisionMode = ColorVisionMode.NONE, highContrastText = true),
            darkTheme = false,
            baseScheme = PlantLightColorScheme
        )
        assertEquals(Color(0xFF000000), lightHighContrastScheme.onBackground)
        assertEquals(Color(0xFF000000), lightHighContrastScheme.onSurface)
    }

    @Test
    fun `PlantHubTheme propagates LocalAccessibilitySuite and TalkBack semantics across UI components`() {
        val now = 1_700_000_000_000L
        val sampleGrid = HeatmapGridData(
            weeks = listOf(
                listOf(
                    HeatmapDay(
                        dateKey = "2026-11-20",
                        dateMillis = now - 86_400_000L,
                        displayDate = "Nov 20",
                        dayOfWeek = 5,
                        logCount = 2,
                        level = 2,
                        careLogs = emptyList()
                    ),
                    HeatmapDay(
                        dateKey = "2026-11-21",
                        dateMillis = now,
                        displayDate = "Nov 21",
                        dayOfWeek = 6,
                        logCount = 4,
                        level = 4,
                        careLogs = emptyList()
                    )
                )
            ),
            totalContributions = 6,
            currentStreak = 2,
            longestStreak = 2,
            averagePerWeek = 6.0
        )

        val sampleLightStatus = LightCycleStatus(
            isLightOn = true,
            currentPhaseName = "LIGHTS ON",
            hoursRemainingInPhase = 10,
            minutesRemainingInPhase = 30,
            secondsRemainingInPhase = 15,
            phaseProgressPercent = 0.42f,
            startTimeFormatted = "06:00 AM",
            nextSwitchTimeFormatted = "12:00 AM",
            totalOnHours = 18,
            totalOffHours = 6,
            presetName = "18/6 Vegetative",
            isTimerRunning = true,
            isWaitingToStart = false
        )

        val samplePhotos = listOf(
            PhotoEntry(
                id = 1L,
                plantId = 1L,
                timestamp = now - (14L * 86_400_000L),
                dayNumber = 1,
                stage = GrowthStage.SEEDLING,
                title = "Baseline",
                imageUri = ""
            ),
            PhotoEntry(
                id = 2L,
                plantId = 1L,
                timestamp = now - (7L * 86_400_000L),
                dayNumber = 7,
                stage = GrowthStage.VEGETATIVE,
                title = "Week 1",
                imageUri = ""
            ),
            PhotoEntry(
                id = 3L,
                plantId = 1L,
                timestamp = now,
                dayNumber = 14,
                stage = GrowthStage.VEGETATIVE,
                title = "Week 2",
                imageUri = ""
            )
        )

        var resolvedPrimary = Color.Unspecified
        var resolvedUsePatterns = false

        composeTestRule.setContent {
            PlantHubTheme(
                darkTheme = true,
                dynamicColor = true,
                accessibilityOverride = SystemAccessibilityState(
                    colorVisionMode = ColorVisionMode.TRITAN,
                    highContrastText = true,
                    screenReaderActive = true
                )
            ) {
                resolvedPrimary = MaterialTheme.colorScheme.primary
                resolvedUsePatterns = LocalAccessibilitySuite.current.useNonColorPatterns

                Column(modifier = androidx.compose.ui.Modifier.verticalScroll(rememberScrollState())) {
                    GithubHeatmapGrid(
                        gridData = sampleGrid,
                        currentViewMode = HeatmapViewMode.WEEKS_16,
                        onViewModeChange = {}
                    )
                    LightCycleGauge(status = sampleLightStatus)
                    QuickCareBar(
                        onWaterClick = {},
                        onMistClick = {},
                        onFertilizeClick = {},
                        onOpenCustomDialog = {}
                    )
                    VaultElapsedDaysAnalyticsCard(
                        photos = samplePhotos,
                        onPhotoClick = {},
                        isExpanded = true
                    )
                }
            }
        }

        composeTestRule.waitForIdle()

        // Verify Tritan palette bypassed Monet & turned on non-color geometric markers
        assertEquals(Color(0xFF33D6C4), resolvedPrimary)
        assertTrue(resolvedUsePatterns)

        // Verify TalkBack semantics on heatmap streak, day cell, light cycle gauge, quick care chips, and chart
        composeTestRule.onNodeWithContentDescription("2 day cultivation care streak")
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("day_cell_2026-11-21")
            .assertContentDescriptionContains("activity level 4 of 4", substring = true, ignoreCase = true)
        composeTestRule.onNodeWithTag("light_cycle_gauge")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("LIGHTS ON", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("timeline_canvas_chart")
            .performScrollTo()
            .assertIsDisplayed()
            .assertContentDescriptionContains("weekly milestones", substring = true, ignoreCase = true)
        composeTestRule.onNodeWithContentDescription("Quick log 250 milliliters water")
            .performScrollTo()
            .assertIsDisplayed()
    }
}
