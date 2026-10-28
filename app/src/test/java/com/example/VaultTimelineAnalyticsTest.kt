package com.example

import com.example.data.model.GrowthStage
import com.example.data.model.PhotoEntry
import com.example.ui.components.calculateTimelineChartPoints
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultTimelineAnalyticsTest {

    @Test
    fun calculateTimelineChartPoints_emptyList_returnsEmpty() {
        val points = calculateTimelineChartPoints(emptyList())
        assertTrue(points.isEmpty())
    }

    @Test
    fun calculateTimelineChartPoints_singlePhoto_hasZeroElapsedDays() {
        val single = PhotoEntry(
            id = 1L,
            plantId = 100L,
            timestamp = 1000000L,
            imageUri = "/mock/path/1.jpg",
            dayNumber = 1,
            stage = GrowthStage.SEEDLING
        )

        val points = calculateTimelineChartPoints(listOf(single))
        assertEquals(1, points.size)
        assertEquals(0f, points[0].elapsedDays, 0.001f)
        assertTrue(points[0].isMilestone)
    }

    @Test
    fun calculateTimelineChartPoints_multiplePhotos_calculatesAccurateElapsedDays() {
        val baseTime = 1700000000000L // arbitrary fixed base timestamp
        val oneDayMillis = 24L * 60L * 60L * 1000L

        val p1 = PhotoEntry(
            id = 1L,
            plantId = 10L,
            timestamp = baseTime,
            imageUri = "p1.jpg",
            dayNumber = 1,
            stage = GrowthStage.VEGETATIVE
        )
        val p2 = PhotoEntry(
            id = 2L,
            plantId = 10L,
            timestamp = baseTime + (3 * oneDayMillis),
            imageUri = "p2.jpg",
            dayNumber = 4,
            stage = GrowthStage.VEGETATIVE
        )
        val p3 = PhotoEntry(
            id = 3L,
            plantId = 10L,
            timestamp = baseTime + (6 * oneDayMillis),
            imageUri = "p3.jpg",
            dayNumber = 7,
            stage = GrowthStage.VEGETATIVE
        )
        val p4 = PhotoEntry(
            id = 4L,
            plantId = 10L,
            timestamp = baseTime + (13 * oneDayMillis),
            imageUri = "p4.jpg",
            dayNumber = 14,
            stage = GrowthStage.FLOWERING
        )

        val points = calculateTimelineChartPoints(listOf(p3, p1, p4, p2)) // deliberately unordered

        // Verify sorted chronologically
        assertEquals(4, points.size)
        assertEquals(1, points[0].photo.dayNumber)
        assertEquals(4, points[1].photo.dayNumber)
        assertEquals(7, points[2].photo.dayNumber)
        assertEquals(14, points[3].photo.dayNumber)

        // Verify elapsed days progression
        assertEquals(0f, points[0].elapsedDays, 0.05f)
        assertEquals(3f, points[1].elapsedDays, 0.05f)
        assertEquals(6f, points[2].elapsedDays, 0.05f)
        assertEquals(13f, points[3].elapsedDays, 0.05f)

        // Verify milestones (Day 1, Day 7, Day 14 are milestones; Day 4 is not)
        assertTrue(points[0].isMilestone) // Day 1
        assertFalse(points[1].isMilestone) // Day 4
        assertTrue(points[2].isMilestone) // Day 7
        assertTrue(points[3].isMilestone) // Day 14
    }
}
