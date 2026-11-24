package com.example

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.CareActionType
import com.example.data.model.CareLog
import com.example.data.model.GrowthStage
import com.example.data.model.Plant
import com.example.data.repository.PlantRepository
import com.example.ui.components.EmergencyEditWarningDialog
import com.example.ui.components.EmergencyPlantEditDialog
import com.example.ui.theme.PlantHubTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EmergencyEditFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: PlantRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = PlantRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `two-step emergency dialog requires confirmation and updates identifier strain and day amount`() {
        var showWarning by mutableStateOf(true)
        var showEdit by mutableStateOf(false)
        var savedName: String? = null
        var savedStrain: String? = null
        var savedDay: Int? = null

        val samplePlant = Plant(
            id = 42L,
            name = "Tent A1",
            strain = "OG Kush",
            stage = GrowthStage.VEGETATIVE,
            plantedDate = System.currentTimeMillis() - (19L * 86_400_000L)
        )

        composeTestRule.setContent {
            PlantHubTheme(darkTheme = true, dynamicColor = false) {
                if (showWarning) {
                    EmergencyEditWarningDialog(
                        plantName = samplePlant.name,
                        onDismiss = { showWarning = false },
                        onConfirm = {
                            showWarning = false
                            showEdit = true
                        }
                    )
                }
                if (showEdit) {
                    EmergencyPlantEditDialog(
                        plant = samplePlant,
                        currentDayAmount = 20,
                        onDismiss = { showEdit = false },
                        onSave = { name, strain, day ->
                            savedName = name
                            savedStrain = strain
                            savedDay = day
                            showEdit = false
                        }
                    )
                }
            }
        }

        // Step 1: Warning dialog must appear first
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("dialog_emergency_edit_warning").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_emergency_confirm_warning").performClick()

        // Step 2: Compact edit dialog opens after confirming
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("dialog_emergency_plant_edit").assertIsDisplayed()

        composeTestRule.onNodeWithTag("input_emergency_plant_name").performTextClearance()
        composeTestRule.onNodeWithTag("input_emergency_plant_name").performTextInput("Tent A1 Replant")

        composeTestRule.onNodeWithTag("input_emergency_plant_strain").performTextClearance()
        composeTestRule.onNodeWithTag("input_emergency_plant_strain").performTextInput("Blue Dream")

        // Reset day to Day 1 via quick button
        composeTestRule.onNodeWithTag("btn_reset_day_one").performClick()
        composeTestRule.onNodeWithTag("btn_emergency_edit_save").performClick()
        composeTestRule.waitForIdle()

        assertFalse(showWarning)
        assertFalse(showEdit)
        assertEquals("Tent A1 Replant", savedName)
        assertEquals("Blue Dream", savedStrain)
        assertEquals(1, savedDay)
    }

    @Test
    fun `emergency plant override preserves care logs while resetting day counter`() = runBlocking {
        val now = System.currentTimeMillis()
        val plantId = repository.insertPlant(
            Plant(
                name = "Slot 2",
                strain = "Gelato",
                stage = GrowthStage.FLOWERING,
                plantedDate = now - (30L * 86_400_000L)
            )
        )
        repository.insertCareLog(
            CareLog(
                plantId = plantId,
                timestamp = now - 3_600_000L,
                actionType = CareActionType.WATER,
                waterAmountMl = 1200,
                notes = "Prior log retained"
            )
        )

        val original = repository.getPlantOnce(plantId)!!
        val targetDay = 5
        val adjustedPlantedDate = System.currentTimeMillis() - ((targetDay - 1L) * 86_400_000L)
        repository.updatePlant(
            original.copy(
                name = "Slot 2 Restart",
                strain = "Zkittlez",
                plantedDate = adjustedPlantedDate
            )
        )

        val updated = repository.getPlantOnce(plantId)!!
        val computedDaysInGrow = ((System.currentTimeMillis() - updated.plantedDate) / 86_400_000L).coerceAtLeast(0) + 1
        assertEquals("Slot 2 Restart", updated.name)
        assertEquals("Zkittlez", updated.strain)
        assertEquals(5L, computedDaysInGrow)

        val logs = repository.getAllCareLogsOnce()
        assertEquals(1, logs.size)
        assertTrue(logs[0].notes.contains("Prior log retained"))
    }
}
