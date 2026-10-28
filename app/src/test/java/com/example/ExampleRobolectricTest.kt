package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.GrowthStage
import com.example.data.model.Plant
import com.example.util.HeatmapCalculator
import com.example.util.LightCycleEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context matches PlantHub`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("PlantHub", appName)
    }

    @Test
    fun `new plant light cycle stays in waiting state until started`() {
        val newPlant = Plant(
            name = "New Seedling",
            strain = "OG Kush",
            stage = GrowthStage.VEGETATIVE,
            lightOnHours = 18,
            lightOffHours = 6,
            isTimerRunning = false,
            timerStartedAt = null
        )
        val status = LightCycleEngine.calculateStatus(newPlant)
        assertNotNull(status)
        assertTrue(status.isWaitingToStart)
        assertFalse(status.isTimerRunning)
        assertEquals(18L, status.hoursRemainingInPhase)
        assertEquals(0L, status.minutesRemainingInPhase)
        assertEquals(0.0f, status.phaseProgressPercent, 0.001f)
        assertEquals("WAITING TO START", status.currentPhaseName)
    }

    @Test
    fun `started plant countdown is active`() {
        val runningPlant = Plant(
            name = "Northern Lights",
            strain = "Indica",
            stage = GrowthStage.VEGETATIVE,
            lightOnHours = 18,
            lightOffHours = 6,
            isTimerRunning = true,
            timerStartedAt = System.currentTimeMillis() - (1 * 3600 * 1000L) // started 1 hour ago
        )
        val status = LightCycleEngine.calculateStatus(runningPlant)
        assertNotNull(status)
        assertTrue(status.isTimerRunning)
        assertFalse(status.isWaitingToStart)
        assertTrue(status.isLightOn)
        assertEquals(17L, status.hoursRemainingInPhase)
        assertTrue(status.phaseProgressPercent > 0.0f)
    }

    @Test
    fun `heatmap calculation builds 16 weeks matrix`() {
        val gridData = HeatmapCalculator.calculate16Weeks(emptyList())
        assertEquals(16, gridData.weeks.size)
        assertEquals(7, gridData.weeks[0].size)
    }

    @Test
    fun `botanical icon helper resolves curated botanical icons`() {
        val cannabisIcon = com.example.ui.components.PlantIconHelper.getPlantIcon("cannabis")
        assertNotNull(cannabisIcon)

        val flowerIcon = com.example.ui.components.PlantIconHelper.getPlantIcon("flower")
        assertNotNull(flowerIcon)

        val seedlingIcon = com.example.ui.components.PlantIconHelper.getPlantIcon("seedling")
        assertNotNull(seedlingIcon)

        val unknownIcon = com.example.ui.components.PlantIconHelper.getPlantIcon("nonexistent")
        assertNotNull(unknownIcon)
    }

    @Test
    fun `notification channel creates without error on Robolectric`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        com.example.util.NotificationHelper.createNotificationChannel(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val channel = nm.getNotificationChannel(com.example.util.NotificationHelper.CHANNEL_ID)
        assertNotNull(channel)
        assertEquals("PlantHub Cultivation Reminders", channel.name)
    }
}
