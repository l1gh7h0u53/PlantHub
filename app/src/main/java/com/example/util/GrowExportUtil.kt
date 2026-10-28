package com.example.util

import android.content.Context
import android.content.Intent
import com.example.data.model.CareActionType
import com.example.data.model.CareLog
import com.example.data.model.PhotoEntry
import com.example.data.model.Plant
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object GrowExportUtil {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
    private val dateOnlyFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)

    fun generateGrowReport(
        plant: Plant,
        careLogs: List<CareLog>,
        photos: List<PhotoEntry>
    ): String {
        val daysInGrow = ((System.currentTimeMillis() - plant.plantedDate) / (1000 * 60 * 60 * 24)).coerceAtLeast(0) + 1
        val totalWaterMl = careLogs.filter { it.actionType == CareActionType.WATER }.sumOf { it.waterAmountMl ?: 0 }
        val wateringCount = careLogs.count { it.actionType == CareActionType.WATER }
        val mistingCount = careLogs.count { it.actionType == CareActionType.MIST }
        val feedingCount = careLogs.count { it.actionType == CareActionType.FERTILIZE }

        return buildString {
            appendLine("==================================================")
            appendLine("   PLANTHUB CULTIVATION LOG & COMMIT HISTORY      ")
            appendLine("==================================================")
            appendLine("Plant Name    : ${plant.name}")
            appendLine("Strain/Species: ${plant.strain}")
            appendLine("Current Stage : ${plant.stage.label}")
            appendLine("Planted Date  : ${dateOnlyFormat.format(Date(plant.plantedDate))} (Day $daysInGrow)")
            appendLine("Medium        : ${plant.medium}")
            appendLine("Pot Volume    : ${plant.potSizeLiters} L")
            appendLine("Light Cycle   : ${plant.lightCyclePreset} (${plant.lightOnHours}h ON / ${plant.lightOffHours}h OFF)")
            appendLine("Lights Start  : ${LightCycleEngine.formatMinutes(plant.lightStartTimeMinute)}")
            if (plant.notes.isNotBlank()) {
                appendLine("Notes         : ${plant.notes}")
            }
            appendLine()
            appendLine("--- AGGREGATE CARE METRICS ---")
            appendLine("Total Water Given : $totalWaterMl mL across $wateringCount waterings")
            appendLine("Misting Events    : $mistingCount")
            appendLine("Feeding Events    : $feedingCount")
            appendLine("Photo Checkpoints : ${photos.size} entries")
            appendLine()
            appendLine("--- RECENT CARE TIMELINE (${careLogs.size} logs) ---")
            if (careLogs.isEmpty()) {
                appendLine("No care logs recorded yet.")
            } else {
                careLogs.forEachIndexed { index, log ->
                    val timestamp = dateFormat.format(Date(log.timestamp))
                    appendLine("[$timestamp] #${index + 1} ${log.actionType.displayName}")
                    when (log.actionType) {
                        CareActionType.WATER -> {
                            appendLine("   * Volume: ${log.waterAmountMl ?: 0} mL | pH: ${log.phLevel ?: "N/A"}")
                        }
                        CareActionType.FERTILIZE -> {
                            appendLine("   * Nutrient: ${log.fertilizerName ?: "Custom"} | Dose: ${log.fertilizerDose ?: "Standard"} | EC/PPM: ${log.ecPpm ?: "N/A"}")
                        }
                        CareActionType.MIST -> {
                            appendLine("   * Foliar misting cycle applied")
                        }
                        CareActionType.DEFOLIATE, CareActionType.TRAIN, CareActionType.NOTE -> {
                            // general note handled below
                        }
                    }
                    if (log.notes.isNotBlank()) {
                        appendLine("   * Note: ${log.notes}")
                    }
                }
            }
            appendLine()
            appendLine("--- PHOTO PROGRESSION VAULT ---")
            if (photos.isEmpty()) {
                appendLine("No photo entries logged yet.")
            } else {
                photos.forEach { p ->
                    val date = dateFormat.format(Date(p.timestamp))
                    appendLine("[$date] Day ${p.dayNumber} [${p.stage.label}] - ${p.title}")
                    if (p.canopyHeightCm != null) {
                        appendLine("   * Canopy Height: ${p.canopyHeightCm} cm")
                    }
                    if (p.observation.isNotBlank()) {
                        appendLine("   * Observation: ${p.observation}")
                    }
                }
            }
            appendLine()
            appendLine("Exported via PlantHub offline cultivation engine.")
            appendLine("Timestamp: ${dateFormat.format(Date())}")
        }
    }

    fun shareReport(context: Context, report: String, plantName: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, report)
            putExtra(Intent.EXTRA_SUBJECT, "PlantHub Grow Log - $plantName")
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Export PlantHub Grow Log")
        context.startActivity(shareIntent)
    }
}
