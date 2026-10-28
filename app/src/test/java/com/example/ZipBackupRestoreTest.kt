package com.example

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.GrowthStage
import com.example.data.model.PhotoEntry
import com.example.data.model.Plant
import com.example.data.repository.PlantRepository
import com.example.ui.screens.isPhotoFileAvailable
import com.example.util.PlantBackupManager
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ZipBackupRestoreTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: PlantRepository
    private lateinit var testDir: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = PlantRepository(database)
        testDir = File(context.cacheDir, "test_zip_${System.currentTimeMillis()}").apply { mkdirs() }
    }

    @After
    fun tearDown() {
        database.close()
        testDir.deleteRecursively()
    }

    @Test
    fun `exportCompleteZipBackup packages database and real photo files into a standalone ZIP bundle`() = runBlocking {
        // 1. Setup sample plant and real photo file on disk
        val plantId = repository.insertPlant(
            Plant(
                name = "Gorilla Glue #4",
                strain = "Hybrid",
                stage = GrowthStage.FLOWERING
            )
        )

        val photosDir = File(context.filesDir, "growth_photos").apply { mkdirs() }
        val sampleImageFile = File(photosDir, "test_sample_image.jpg")
        val sampleImageBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 1, 2, 3, 4, 5)
        FileOutputStream(sampleImageFile).use { it.write(sampleImageBytes) }

        val photoId = repository.insertPhotoEntry(
            PhotoEntry(
                plantId = plantId,
                timestamp = System.currentTimeMillis(),
                imageUri = sampleImageFile.absolutePath,
                stage = GrowthStage.FLOWERING,
                dayNumber = 35,
                title = "Frosty Trichomes",
                observation = "Heavy resin coverage"
            )
        )

        // 2. Export full ZIP backup
        val zipFile = File(testDir, "export_test.zip")
        val zipUri = Uri.fromFile(zipFile)

        val exportSuccess = PlantBackupManager.exportCompleteZipBackup(context, zipUri, repository)
        assertTrue("ZIP export should report success", exportSuccess)
        assertTrue("ZIP file should exist on disk", zipFile.exists())
        assertTrue("ZIP file size should be greater than zero", zipFile.length() > 0)

        // 3. Inspect ZIP bundle
        val (preview, error) = PlantBackupManager.parseImportZip(context, zipUri, "export_test.zip")
        assertNotNull(preview)
        assertEquals("ZIP Bundle", preview!!.sourceFormat)
        assertEquals(1, preview.plants.size)
        assertEquals(1, preview.photos.size)
        assertEquals(1, preview.bundledImagesCount)

        // 4. Wipe database and original photo file to simulate a new device
        repository.clearAllData()
        sampleImageFile.delete()

        assertEquals(0, repository.getAllPlantsOnce().size)
        assertEquals(0, repository.getAllPhotosOnce().size)

        // 5. Restore from ZIP
        val restoreSuccess = PlantBackupManager.restoreDatabaseFromZip(context, repository, preview, zipUri)
        assertTrue("Restoration from ZIP should succeed", restoreSuccess)

        // 6. Verify database records and real physical photo file restoration
        val restoredPlants = repository.getAllPlantsOnce()
        assertEquals(1, restoredPlants.size)
        assertEquals("Gorilla Glue #4", restoredPlants[0].name)

        val restoredPhotos = repository.getAllPhotosOnce()
        assertEquals(1, restoredPhotos.size)
        val restoredPhoto = restoredPhotos[0]
        assertEquals("Frosty Trichomes", restoredPhoto.title)

        // The photo's imageUri must be a real file on disk that exists
        val restoredFile = File(restoredPhoto.imageUri)
        assertTrue("Restored image file must physically exist at ${restoredPhoto.imageUri}", restoredFile.exists())
        assertEquals(sampleImageBytes.size.toLong(), restoredFile.length())
        assertArrayEquals(sampleImageBytes, restoredFile.readBytes())

        // isPhotoFileAvailable should evaluate to true
        assertTrue("isPhotoFileAvailable should be true for restored photo", isPhotoFileAvailable(context, restoredPhoto.imageUri))
    }
}
