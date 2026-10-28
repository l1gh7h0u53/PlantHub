package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.CareActionType
import com.example.data.model.GrowthStage
import com.example.data.repository.PlantRepository
import com.example.util.PlantBackupManager
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CsvImportSystemTest {

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
    fun `parseMultiTableCsv correctly ingests multi-table format with camelCase`() {
        val csv = """
            # TABLE: PLANTS
            id,name,strain,stage,lightCyclePreset,lightOnHours,lightOffHours,lightStartTimeMinute,medium,potSizeLiters,plantedDate,isTimerRunning,timerStartedAt,timerPausedRemainingSec,isPausedForMaintenance,iconKey,notes
            1,"Amnesia Haze","Sativa",FLOWERING,"12/12 Flowering",12,12,480,"Coco Coir",15.0,1700000000000,true,1700001000000,,false,flower,"Vigorous stretch"

            # TABLE: CARE_LOGS
            id,plantId,timestamp,actionType,waterAmountMl,phLevel,fertilizerName,fertilizerDose,ecPpm,notes
            10,1,1700005000000,WATER,1500,6.2,"Bloom A+B","2ml/L",850,"Routine watering"

            # TABLE: PHOTOS
            id,plantId,timestamp,imageUri,stage,dayNumber,title,canopyHeightCm,observation
            20,1,1700006000000,"/storage/photos/p1.jpg",FLOWERING,21,"Week 3 Stretch",45.5,"Great node spacing"
        """.trimIndent()

        val parsed = PlantBackupManager.parseMultiTableCsv(csv, "backup.csv")
        assertEquals(1, parsed.plants.size)
        assertEquals("Amnesia Haze", parsed.plants[0].name)
        assertEquals(GrowthStage.FLOWERING, parsed.plants[0].stage)

        assertEquals(1, parsed.logs.size)
        assertEquals(1L, parsed.logs[0].plantId)
        assertEquals(CareActionType.WATER, parsed.logs[0].actionType)
        assertEquals(1500, parsed.logs[0].waterAmountMl)
        assertEquals(6.2, parsed.logs[0].phLevel ?: 0.0, 0.01)

        assertEquals(1, parsed.photos.size)
        assertEquals(1L, parsed.photos[0].plantId)
        assertEquals(21, parsed.photos[0].dayNumber)
        assertEquals(45.5, parsed.photos[0].canopyHeightCm ?: 0.0, 0.01)
    }

    @Test
    fun `parseMultiTableCsv handles snake_case and human-friendly headers`() {
        val csv = """
            # TABLE: PLANTS
            plant_id,plant_name,variety,lifecycle,medium,pot_size_liters
            5,"Gelato 33","Indica",VEGETATIVE,"Living Soil",20.0

            # TABLE: CARE_LOGS
            plant_id,action_type,water_amount_ml,ph_level,observation_notes
            5,FEED,2000,6.5,"Organic compost tea"
        """.trimIndent()

        val parsed = PlantBackupManager.parseMultiTableCsv(csv, "aliased.csv")
        assertEquals(1, parsed.plants.size)
        assertEquals("Gelato 33", parsed.plants[0].name)
        assertEquals("Indica", parsed.plants[0].strain)

        assertEquals(1, parsed.logs.size)
        assertEquals(5L, parsed.logs[0].plantId)
        assertEquals(CareActionType.FERTILIZE, parsed.logs[0].actionType)
        assertEquals(2000, parsed.logs[0].waterAmountMl)
        assertEquals("Organic compost tea", parsed.logs[0].notes)
    }

    @Test
    fun `standalone care logs CSV auto-detects table type and synthesizes fallback cultivar`() {
        val csv = """
            Action Type,Water (ml),pH Level,Feed Name,Feed Dose,Notes
            WATER,1200,6.3,"Cal-Mag","1ml/L","Morning hydration"
            DEFOLIATE,,,,,"Removed bottom fan leaves"
        """.trimIndent()

        val parsed = PlantBackupManager.parseMultiTableCsv(csv, "care_logs.csv")

        // Standalone logs CSV without plants table synthesizes a primary fallback cultivar
        assertEquals(1, parsed.plants.size)
        assertEquals("Imported Cultivar", parsed.plants[0].name)

        assertEquals(2, parsed.logs.size)
        assertEquals(CareActionType.WATER, parsed.logs[0].actionType)
        assertEquals(1200, parsed.logs[0].waterAmountMl)
        assertEquals("Cal-Mag", parsed.logs[0].fertilizerName)
        assertEquals(CareActionType.DEFOLIATE, parsed.logs[1].actionType)
        assertEquals("Removed bottom fan leaves", parsed.logs[1].notes)
    }

    @Test
    fun `standalone photos CSV auto-detects table type and synthesizes fallback cultivar`() {
        val csv = """
            Day Number,Growth Stage,Image URI,Canopy Height (cm),Title,Observations
            14,VEGETATIVE,"/storage/emulated/0/photo1.jpg",24.0,"Node 4 Topping","Clean 45 degree cut"
            28,FLOWERING,"/storage/emulated/0/photo2.jpg",52.0,"Transition Bloom","Pistils emerging"
        """.trimIndent()

        val parsed = PlantBackupManager.parseMultiTableCsv(csv, "photo_vault.csv")

        assertEquals(1, parsed.plants.size)
        assertEquals("Imported Cultivar", parsed.plants[0].name)

        assertEquals(2, parsed.photos.size)
        assertEquals(14, parsed.photos[0].dayNumber)
        assertEquals(GrowthStage.VEGETATIVE, parsed.photos[0].stage)
        assertEquals(24.0, parsed.photos[0].canopyHeightCm ?: 0.0, 0.01)
        assertEquals("Node 4 Topping", parsed.photos[0].title)

        assertEquals(28, parsed.photos[1].dayNumber)
        assertEquals(GrowthStage.FLOWERING, parsed.photos[1].stage)
        assertEquals(52.0, parsed.photos[1].canopyHeightCm ?: 0.0, 0.01)
    }

    @Test
    fun `restoreDatabaseFromData persists standalone CSV and preserves foreign key referential integrity`() = runBlocking {
        val csv = """
            Action Type,Water (ml),pH Level,Feed Name,Feed Dose,Notes
            WATER,1000,6.4,,, "First watering"
            FEED,1500,6.2,"Kelp Meal","5ml/gal","Nutrient application"
        """.trimIndent()

        val parsed = PlantBackupManager.parseMultiTableCsv(csv, "standalone_care.csv")
        val restored = PlantBackupManager.restoreDatabaseFromData(context, repository, parsed)

        assertTrue("Restoration should succeed", restored)

        val plants = repository.getAllPlantsOnce()
        assertEquals(1, plants.size)
        val createdPlantId = plants[0].id
        assertTrue(createdPlantId > 0L)

        val logs = repository.getAllCareLogsOnce()
        assertEquals(2, logs.size)
        // Verify all unlinked logs were mapped to the newly created primary cultivar
        assertEquals(createdPlantId, logs[0].plantId)
        assertEquals(createdPlantId, logs[1].plantId)
    }
}
