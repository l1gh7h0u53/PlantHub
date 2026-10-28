package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.util.LightCycleEngine
import com.example.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver responsible for exact background Light Cycle phase transitions
 * and battery-conscious 1-minute progress sync for the Android 16 live status notification.
 *
 * Ensures the notification never ticks down into negative numbers and transitions
 * immediately between Lights ON and Lights OFF even when the app is closed.
 */
class LightCycleAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val action = intent.action ?: return
        val plantId = intent.getLongExtra(EXTRA_PLANT_ID, -1L)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val plantDao = db.plantDao()
                val plant = if (plantId > 0) {
                    plantDao.getPlantByIdOnce(plantId)
                } else {
                    plantDao.getAllPlantsOnce().firstOrNull { it.isTimerRunning }
                        ?: plantDao.getAllPlantsOnce().firstOrNull()
                }

                if (plant != null) {
                    val status = LightCycleEngine.calculateStatus(plant)
                    when (action) {
                        ACTION_PHASE_TRANSITION -> {
                            Log.d(TAG, "Executing exact phase transition alarm for plant ${plant.name}")
                            if (status.isPausedForMaintenance && !plant.isPausedForMaintenance) {
                                plantDao.updatePlant(
                                    plant.copy(
                                        isPausedForMaintenance = true,
                                        isTimerRunning = false
                                    )
                                )
                                NotificationHelper.sendLightsOffAlert(
                                    context = context,
                                    notificationId = plant.id.toInt() + 1000,
                                    plantName = plant.name
                                )
                            }
                            NotificationHelper.updateLightCycleLiveUpdate(
                                context = context,
                                status = status,
                                plantName = plant.name,
                                plantId = plant.id
                            )
                        }
                        ACTION_PROGRESS_SYNC -> {
                            if (plant.isTimerRunning) {
                                if (status.isPausedForMaintenance && !plant.isPausedForMaintenance) {
                                    plantDao.updatePlant(
                                        plant.copy(
                                            isPausedForMaintenance = true,
                                            isTimerRunning = false
                                        )
                                    )
                                    NotificationHelper.sendLightsOffAlert(
                                        context = context,
                                        notificationId = plant.id.toInt() + 1000,
                                        plantName = plant.name
                                    )
                                }
                                NotificationHelper.updateLightCycleLiveUpdate(
                                    context = context,
                                    status = status,
                                    plantName = plant.name,
                                    plantId = plant.id
                                )
                            } else {
                                // Timer was paused or stopped; cancel further sync alarms
                                NotificationHelper.cancelLightCycleAlarms(context)
                            }
                        }
                    }
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Error in LightCycleAlarmReceiver onReceive", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "LightCycleReceiver"
        const val ACTION_PHASE_TRANSITION = "com.example.action.LIGHT_CYCLE_PHASE_TRANSITION"
        const val ACTION_PROGRESS_SYNC = "com.example.action.LIGHT_CYCLE_PROGRESS_SYNC"
        const val EXTRA_PLANT_ID = "extra_plant_id"
    }
}
