package com.example.util

import com.example.data.model.CareActionType
import com.example.data.model.CareLog
import com.example.data.model.GrowthStage
import com.example.data.model.PhotoEntry
import com.example.data.model.Plant
import com.example.data.repository.PlantRepository
import java.util.Calendar

object SeedData {

    suspend fun populateIfEmpty(repository: PlantRepository) {
        if (repository.getPlantCount() > 0) return

        val now = System.currentTimeMillis()
        val oneDayMillis = 24 * 60 * 60 * 1000L

        // Plant 1: Northern Lights (Vegetative 18/6) - Day 28
        val plant1 = Plant(
            name = "Northern Lights #1",
            strain = "Northern Lights (Indica Dominant)",
            stage = GrowthStage.VEGETATIVE,
            lightCyclePreset = "18/6 Vegetative",
            lightOnHours = 18,
            lightOffHours = 6,
            lightStartTimeMinute = 360, // 06:00 AM
            medium = "Organic Living Soil",
            potSizeLiters = 18.0,
            plantedDate = now - (28 * oneDayMillis),
            notes = "Main canopy topped at node 5. Responding vigorously to LST.",
            isTimerRunning = true,
            timerStartedAt = now - (2 * 3600 * 1000L),
            iconKey = "cannabis"
        )
        val p1Id = repository.insertPlant(plant1)

        // Plant 2: Super Lemon Haze (Flowering 12/12) - Day 54
        val plant2 = Plant(
            name = "Super Lemon Haze #3",
            strain = "Super Lemon Haze (Sativa)",
            stage = GrowthStage.FLOWERING,
            lightCyclePreset = "12/12 Flowering",
            lightOnHours = 12,
            lightOffHours = 12,
            lightStartTimeMinute = 480, // 08:00 AM
            medium = "Coco Coir + Perlite 70/30",
            potSizeLiters = 15.0,
            plantedDate = now - (54 * oneDayMillis),
            notes = "Week 4 flower. Heavy trichome production and citrus terpene profile.",
            isTimerRunning = true,
            timerStartedAt = now - (4 * 3600 * 1000L),
            iconKey = "flower"
        )
        val p2Id = repository.insertPlant(plant2)

        // Plant 3: Gorilla Cookies Auto (Autoflower 20/4) - Day 14 (Waiting state)
        val plant3 = Plant(
            name = "Gorilla Cookies Auto",
            strain = "Gorilla Glue x Girl Scout Cookies",
            stage = GrowthStage.AUTOFLOWER,
            lightCyclePreset = "20/4 Autoflower",
            lightOnHours = 20,
            lightOffHours = 4,
            lightStartTimeMinute = 300, // 05:00 AM
            medium = "BioBizz Light-Mix",
            potSizeLiters = 11.0,
            plantedDate = now - (14 * oneDayMillis),
            notes = "Rapid vegetative burst. Perfect internodal spacing.",
            isTimerRunning = false,
            timerStartedAt = null,
            iconKey = "seedling"
        )
        val p3Id = repository.insertPlant(plant3)

        // Generate past 60 days care logs to populate GitHub heatmap
        val logs = mutableListOf<CareLog>()
        val cal = Calendar.getInstance()

        for (dayAgo in 0..50) {
            val dayTime = now - (dayAgo * oneDayMillis)

            // Northern Lights logs (Watering every 2-3 days, spraying alternate days)
            if (dayAgo % 3 == 0) {
                logs.add(
                    CareLog(
                        plantId = p1Id,
                        timestamp = dayTime + (2 * 3600 * 1000L),
                        actionType = CareActionType.WATER,
                        waterAmountMl = 1200,
                        phLevel = 6.4,
                        notes = "Deep soak with 10% runoff"
                    )
                )
            }
            if (dayAgo % 2 == 0) {
                logs.add(
                    CareLog(
                        plantId = p1Id,
                        timestamp = dayTime + (5 * 3600 * 1000L),
                        actionType = CareActionType.MIST,
                        notes = "Kelp foliar misting before lights on"
                    )
                )
            }
            if (dayAgo % 6 == 0) {
                logs.add(
                    CareLog(
                        plantId = p1Id,
                        timestamp = dayTime + (3 * 3600 * 1000L),
                        actionType = CareActionType.FERTILIZE,
                        fertilizerName = "Bio-Grow + Bio-Heaven",
                        fertilizerDose = "2mL/L",
                        ecPpm = 650,
                        notes = "Organic vegetative nutrition cycle"
                    )
                )
            }

            // Super Lemon Haze logs (Coco requires daily watering/fertigation)
            if (dayAgo % 2 == 0) {
                logs.add(
                    CareLog(
                        plantId = p2Id,
                        timestamp = dayTime + (4 * 3600 * 1000L),
                        actionType = CareActionType.WATER,
                        waterAmountMl = 1500,
                        phLevel = 5.8,
                        notes = "Coco fertigation"
                    )
                )
                logs.add(
                    CareLog(
                        plantId = p2Id,
                        timestamp = dayTime + (4 * 3600 * 1000L + 5000),
                        actionType = CareActionType.FERTILIZE,
                        fertilizerName = "Canna Coco Bloom A+B",
                        fertilizerDose = "3.5mL/L",
                        ecPpm = 950,
                        notes = "Bloom boost with PK 13/14"
                    )
                )
            }

            // Gorilla Cookies logs
            if (dayAgo in 0..14 && dayAgo % 4 == 0) {
                logs.add(
                    CareLog(
                        plantId = p3Id,
                        timestamp = dayTime + (6 * 3600 * 1000L),
                        actionType = CareActionType.WATER,
                        waterAmountMl = 500,
                        phLevel = 6.2,
                        notes = "Seedling/early veg root drench"
                    )
                )
            }
        }
        repository.insertCareLogs(logs)

        // Seed photo entries with placeholder drawable resource path so Coil can render cleanly
        val iconResUri = "android.resource://com.aistudio.planthub.gnxkty/drawable/planthub_icon_1791667356162"

        repository.insertPhotoEntry(
            PhotoEntry(
                plantId = p1Id,
                timestamp = now - (28 * oneDayMillis),
                imageUri = iconResUri,
                stage = GrowthStage.SEEDLING,
                dayNumber = 1,
                title = "Baseline Germination Sprout",
                observation = "Cotyledons fully opened, taproot anchored into living soil.",
                canopyHeightCm = 5.0
            )
        )

        repository.insertPhotoEntry(
            PhotoEntry(
                plantId = p1Id,
                timestamp = now - (21 * oneDayMillis),
                imageUri = iconResUri,
                stage = GrowthStage.VEGETATIVE,
                dayNumber = 7,
                title = "Week 1 Vegetative Checkpoint",
                observation = "Healthy first set of true serrated leaves. Roots spreading through smart pot.",
                canopyHeightCm = 12.0
            )
        )

        repository.insertPhotoEntry(
            PhotoEntry(
                plantId = p1Id,
                timestamp = now - (14 * oneDayMillis),
                imageUri = iconResUri,
                stage = GrowthStage.VEGETATIVE,
                dayNumber = 14,
                title = "Week 2 Node Inspection",
                observation = "Third node emerged, rich green coloration and vigorous stem thickening.",
                canopyHeightCm = 20.5
            )
        )

        repository.insertPhotoEntry(
            PhotoEntry(
                plantId = p1Id,
                timestamp = now - (7 * oneDayMillis),
                imageUri = iconResUri,
                stage = GrowthStage.VEGETATIVE,
                dayNumber = 21,
                title = "Week 3 Canopy Training",
                observation = "First LST ties installed to flatten canopy. 6 tops developing evenly.",
                canopyHeightCm = 29.0
            )
        )

        repository.insertPhotoEntry(
            PhotoEntry(
                plantId = p1Id,
                timestamp = now - (1 * oneDayMillis),
                imageUri = iconResUri,
                stage = GrowthStage.VEGETATIVE,
                dayNumber = 28,
                title = "Week 4 Pre-Flower Stretch",
                observation = "Bushy canopy with excellent lateral branching. Ready for 12/12 transition soon.",
                canopyHeightCm = 38.5
            )
        )
    }
}
