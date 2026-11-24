package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.CareActionType
import com.example.data.model.CareLog
import com.example.data.model.GrowthStage
import com.example.data.model.PhotoEntry
import com.example.data.model.Plant
import com.example.data.model.ReminderSetting
import com.example.data.repository.PlantRepository
import com.example.util.HeatmapCalculator
import com.example.util.PlantBackupManager
import com.example.util.addDaysMillis
import com.example.util.currentTimeMillis
import com.example.util.formatDateKey
import com.example.util.formatDisplayDate
import com.example.util.formatTime12h
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.concurrent.Callable
import java.util.concurrent.Executors

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PerformanceOptimizationTest {

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
        File(context.filesDir, "planthub_plants_backup.json").delete()
        context.getSharedPreferences("planthub_backup_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun `heatmap calculator preserves accuracy and excludes out-of-window historical logs`() {
        val now = currentTimeMillis()
        val logs = mutableListOf<CareLog>()

        // Add 3 logs today, 2 yesterday, 1 two days ago -> current streak should be 3, total commits 6
        repeat(3) {
            logs.add(CareLog(plantId = 1L, timestamp = now, actionType = CareActionType.WATER))
        }
        repeat(2) {
            logs.add(CareLog(plantId = 1L, timestamp = addDaysMillis(now, -1), actionType = CareActionType.MIST))
        }
        logs.add(CareLog(plantId = 1L, timestamp = addDaysMillis(now, -2), actionType = CareActionType.FERTILIZE))

        // Add 500 old logs outside the 52-week grid window (~800 days ago)
        val ancientTimestamp = addDaysMillis(now, -800)
        repeat(500) { idx ->
            logs.add(CareLog(plantId = 1L, timestamp = ancientTimestamp - idx * 60_000L, actionType = CareActionType.NOTE))
        }

        val grid16 = HeatmapCalculator.calculate16Weeks(logs)
        assertEquals(16, grid16.weeks.size)
        assertEquals(6, grid16.totalContributions)
        assertEquals(3, grid16.currentStreak)
        assertEquals(3, grid16.longestStreak)

        val grid52 = HeatmapCalculator.calculateYearly(logs)
        assertEquals(52, grid52.weeks.size)
        assertEquals(6, grid52.totalContributions)
        assertEquals(3, grid52.currentStreak)
        assertEquals(3, grid52.longestStreak)
    }

    @Test
    fun `batched restoreDatabaseFromData maps multi-plant foreign keys and avoids SharedPreferences JSON bloat`() = runBlocking {
        val now = currentTimeMillis()
        val importPlants = listOf(
            Plant(id = 10L, name = "Alpha Cultivar", strain = "Sativa", stage = GrowthStage.VEGETATIVE),
            Plant(id = 25L, name = "Beta Cultivar", strain = "Indica", stage = GrowthStage.FLOWERING)
        )
        val importLogs = listOf(
            CareLog(id = 100L, plantId = 10L, timestamp = now, actionType = CareActionType.WATER, waterAmountMl = 500),
            CareLog(id = 101L, plantId = 25L, timestamp = now - 1000L, actionType = CareActionType.FERTILIZE)
        )
        val importPhotos = listOf(
            PhotoEntry(id = 200L, plantId = 25L, timestamp = now, imageUri = "android.resource://${context.packageName}/drawable/planthub_icon_1791667356162", stage = GrowthStage.FLOWERING, dayNumber = 28, title = "Week 4 Canopy")
        )
        val importReminders = listOf(
            ReminderSetting(id = 300L, plantId = 10L, careType = CareActionType.WATER, intervalHours = 48, lastTriggered = now, isEnabled = true)
        )

        val preview = PlantBackupManager.ImportPreviewData(
            plants = importPlants,
            logs = importLogs,
            photos = importPhotos,
            reminders = importReminders,
            sourceFormat = "JSON",
            fileName = "batch_test.json"
        )

        val ok = PlantBackupManager.restoreDatabaseFromData(context, repository, preview)
        assertTrue(ok)

        val restoredPlants = repository.getAllPlantsOnce()
        val restoredLogs = repository.getAllCareLogsOnce()
        val restoredPhotos = repository.getAllPhotosOnce()
        val restoredReminders = repository.getAllRemindersOnce()

        assertEquals(2, restoredPlants.size)
        assertEquals(2, restoredLogs.size)
        assertEquals(1, restoredPhotos.size)
        assertEquals(1, restoredReminders.size)

        val alpha = restoredPlants.first { it.name == "Alpha Cultivar" }
        val beta = restoredPlants.first { it.name == "Beta Cultivar" }

        assertEquals(1, restoredLogs.count { it.plantId == alpha.id && it.actionType == CareActionType.WATER })
        assertEquals(1, restoredLogs.count { it.plantId == beta.id && it.actionType == CareActionType.FERTILIZE })
        assertEquals(beta.id, restoredPhotos.first().plantId)
        assertEquals(alpha.id, restoredReminders.first().plantId)

        // Ensure atomic file cache exists and SharedPreferences no longer stores raw JSON strings
        assertTrue(PlantBackupManager.hasSavedBackup(context))
        val backupPrefs = context.getSharedPreferences("planthub_backup_prefs", Context.MODE_PRIVATE)
        assertFalse(backupPrefs.contains("backup_json"))
    }

    @Test
    fun `cached PlatformTime formatters are thread-safe and produce consistent output`() {
        val sampleEpoch = 1_735_689_600_000L // 2025-01-01T00:00:00Z
        val expectedKey = formatDateKey(sampleEpoch)
        val expectedTime = formatTime12h(sampleEpoch)
        val expectedDisplay = formatDisplayDate(sampleEpoch)

        val pool = Executors.newFixedThreadPool(4)
        try {
            val tasks = (0 until 40).map { offset ->
                Callable {
                    val ts = sampleEpoch + offset * 60_000L
                    assertEquals(expectedKey, formatDateKey(sampleEpoch))
                    assertEquals(expectedTime, formatTime12h(sampleEpoch))
                    assertEquals(expectedDisplay, formatDisplayDate(sampleEpoch))
                    assertTrue(formatDateKey(ts).matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
                }
            }
            pool.invokeAll(tasks).forEach { it.get() }
        } finally {
            pool.shutdown()
        }
    }
}
