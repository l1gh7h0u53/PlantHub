package com.example.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import com.example.data.local.AppDatabase
import com.example.util.LightCycleEngine
import com.example.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Background and Foreground Ticker Service for the Photoperiod Light Cycle.
 *
 * Runs continuously while an active light timer is ticking so that the
 * Android 16 Promoted Live Update notification and Status Bar Chip
 * never freeze or hang on the last caught time when the app is closed.
 */
class LightCycleTickerService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var isTicking = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "LightCycleTickerService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val targetPlantId = intent?.getLongExtra(EXTRA_PLANT_ID, -1L) ?: -1L

        // Promote to foreground service to prevent OS kill while timer is active
        try {
            val placeholder = NotificationHelper.createDefaultPlaceholderNotification(this)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceCompat.startForeground(
                    this,
                    NotificationHelper.LIVE_UPDATE_NOTIFICATION_ID,
                    placeholder,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NotificationHelper.LIVE_UPDATE_NOTIFICATION_ID, placeholder)
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Foreground start failed, running standard service ticker", e)
        }

        if (!isTicking) {
            isTicking = true
            startTickerLoop(targetPlantId)
        }

        return START_STICKY
    }

    private fun startTickerLoop(initialPlantId: Long) {
        serviceScope.launch {
            val db = AppDatabase.getDatabase(applicationContext)
            val plantDao = db.plantDao()
            val prefs = applicationContext.getSharedPreferences("app_settings_prefs", Context.MODE_PRIVATE)

            var lastNotifiedSecond = -1L
            var lastNotifiedPhase = ""

            while (isActive) {
                try {
                    val isLiveEnabled = prefs.getBoolean("key_live_timer_chip_enabled", true)
                    val allPlants = plantDao.getAllPlantsOnce()
                    val activePlant = if (initialPlantId > 0) {
                        allPlants.firstOrNull { it.id == initialPlantId && it.isTimerRunning && !it.isPausedForMaintenance }
                            ?: allPlants.firstOrNull { it.isTimerRunning && !it.isPausedForMaintenance }
                    } else {
                        allPlants.firstOrNull { it.isTimerRunning && !it.isPausedForMaintenance }
                    }

                    if (activePlant == null || !isLiveEnabled) {
                        Log.d(TAG, "No running plant or chip disabled; stopping ticker service")
                        break
                    }

                    val status = LightCycleEngine.calculateStatus(activePlant)

                    // Automatic phase auto-pause check
                    if (status.isPausedForMaintenance && !activePlant.isPausedForMaintenance) {
                        plantDao.updatePlant(
                            activePlant.copy(
                                isPausedForMaintenance = true,
                                isTimerRunning = false
                            )
                        )
                        NotificationHelper.sendLightsOffAlert(
                            context = applicationContext,
                            notificationId = activePlant.id.toInt() + 1000,
                            plantName = activePlant.name
                        )
                        break
                    }

                    val currentSec = status.secondsRemainingInPhase
                    val currentPhase = status.currentPhaseName

                    // Update live notification when second ticks or phase transitions
                    if (currentSec != lastNotifiedSecond || currentPhase != lastNotifiedPhase) {
                        NotificationHelper.updateLightCycleLiveUpdate(
                            context = applicationContext,
                            status = status,
                            plantName = activePlant.name,
                            plantId = activePlant.id
                        )
                        lastNotifiedSecond = currentSec
                        lastNotifiedPhase = currentPhase
                    }
                } catch (e: Throwable) {
                    Log.e(TAG, "Error in LightCycleTickerService loop", e)
                }

                delay(1000L)
            }

            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isTicking = false
        serviceJob.cancel()
        Log.d(TAG, "LightCycleTickerService destroyed")
    }

    companion object {
        private const val TAG = "LightCycleTickerService"
        const val EXTRA_PLANT_ID = "extra_ticker_plant_id"

        /**
         * Starts the background ticker service for the given plant.
         */
        fun start(context: Context, plantId: Long = -1L) {
            try {
                val intent = Intent(context, LightCycleTickerService::class.java).apply {
                    if (plantId > 0) putExtra(EXTRA_PLANT_ID, plantId)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to start LightCycleTickerService", e)
            }
        }

        /**
         * Stops the background ticker service.
         */
        fun stop(context: Context) {
            try {
                val intent = Intent(context, LightCycleTickerService::class.java)
                context.stopService(intent)
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to stop LightCycleTickerService", e)
            }
        }
    }
}
