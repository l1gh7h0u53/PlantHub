package com.example.util

import android.app.AlarmManager
import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.LightCycleStatus
import com.example.receiver.LightCycleAlarmReceiver

/**
 * NotificationHelper coordinates all local Android system notifications and exact alarms.
 *
 * Responsibilities:
 * - Initializes notification channels for standard care alerts and persistent photoperiod live chips.
 * - Supports Android 16 (API 36) Promoted Ongoing Live Updates with high-contrast status chips.
 * - Schedules exact daily recurring care alarms via Android's AlarmManager (`setExactAndAllowWhileIdle`).
 * - Formats rich notification payloads with progress meters, remaining countdowns, and deep-link intents.
 */
object NotificationHelper {
    const val CHANNEL_ID = "planthub_care_alerts"
    private const val CHANNEL_NAME = "PlantHub Cultivation Reminders"

    const val LIVE_CHANNEL_ID = "planthub_live_light_timer"
    private const val LIVE_CHANNEL_NAME = "PlantHub Live Photoperiod Timer"
    const val LIVE_UPDATE_NOTIFICATION_ID = 9999

    // Android 16 (API 36) Promoted Ongoing Live Updates & Status Chip bundle constants
    const val EXTRA_REQUEST_PROMOTED_ONGOING = "android.requestPromotedOngoing"
    const val EXTRA_SHORT_CRITICAL_TEXT = "android.shortCriticalText"

    /**
     * Creates the high-importance notification channel for daily watering, misting, and nutrient reminders.
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                importance
            ).apply {
                description = "Daily watering, misting, and nutrient feed notifications"
                enableVibration(true)
                enableLights(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun createLiveUpdateChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Use IMPORTANCE_LOW to prevent heads-up notification flashing while keeping persistent ongoing status
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(
                LIVE_CHANNEL_ID,
                LIVE_CHANNEL_NAME,
                importance
            ).apply {
                description = "Real-time photoperiod status and live update status chip in notification shade and status bar"
                enableVibration(false)
                enableLights(false)
                setSound(null, null)
                setShowBadge(false)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun sendCareAlert(
        context: Context,
        notificationId: Int,
        plantName: String,
        actionType: String,
        message: String
    ) {
        createNotificationChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("🌿 $plantName • $actionType")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    fun sendLightsOffAlert(
        context: Context,
        notificationId: Int,
        plantName: String
    ) {
        sendLightMaintenanceAlert(context, notificationId, plantName, "")
    }

    fun sendLightMaintenanceAlert(
        context: Context,
        notificationId: Int,
        plantName: String,
        strain: String
    ) {
        createNotificationChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("🌙 Lights Off • $plantName")
            .setContentText("Dark phase ended • Paused for maintenance")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    fun sendWateringReminder(
        context: Context,
        notificationId: Int,
        plantName: String,
        strain: String
    ) {
        createNotificationChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("💧 Water $plantName")
            .setContentText("48h hydration due • Soil check & watering required")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    fun scheduleDailyCareAlerts(context: Context) {
        createNotificationChannel(context)
        createLiveUpdateChannel(context)
    }

    private const val ALARM_REQ_TRANSITION = 7001
    private const val ALARM_REQ_SYNC = 7002

    /**
     * Dispatches or refreshes the Android 16 Promoted Ongoing Live Update notification
     * with Status Bar Chip support for the photoperiod Light-On/Light-Off timer.
     *
     * Features:
     * - Android 16 Wavy Progress Indicator (Notification.ProgressStyle) with tracker icon.
     * - Fallback to standard progress bar on Android versions below 16.
     * - Live Updates Status Chip with Sun (ic_light_on) / Moon (ic_light_off) icons and live countdown text.
     * - Fix for negative countdown: chronometer stops precisely at switch epoch and flips phase.
     * - Battery-conscious exact transition alarms & 1-minute background progress sync.
     */
    fun updateLightCycleLiveUpdate(
        context: Context,
        status: LightCycleStatus,
        plantName: String,
        plantId: Long = -1L
    ) {
        createLiveUpdateChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("extra_open_light_gauge", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            LIVE_UPDATE_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val isLightsOn = status.isLightOn
        val phaseTitle = if (isLightsOn) "Lights ON" else "Lights OFF"
        val nextPhase = if (isLightsOn) "OFF" else "ON"

        // Dynamic status icon for Status Chip: Sun for Lights ON, Moon for Lights OFF
        val chipIcon = when {
            status.isWaitingToStart -> R.drawable.ic_notification
            isLightsOn -> R.drawable.ic_light_on
            else -> R.drawable.ic_light_off
        }

        // Target switch epoch timestamp for native SystemUI countdown
        val now = System.currentTimeMillis()
        val targetSwitchEpoch = if (status.nextSwitchEpochMillis > 0) {
            status.nextSwitchEpochMillis
        } else {
            val remSec = (status.hoursRemainingInPhase * 3600L) + (status.minutesRemainingInPhase * 60L) + status.secondsRemainingInPhase
            now + (remSec * 1000L)
        }

        // Formatted ticking countdown remaining for Live Status Chip
        val countdownRemainingText = when {
            status.isWaitingToStart -> "WAIT"
            status.isPausedForMaintenance -> "PAUSE"
            !status.isTimerRunning -> "PAUSED"
            status.hoursRemainingInPhase > 0 -> "${status.hoursRemainingInPhase}h ${status.minutesRemainingInPhase}m"
            status.minutesRemainingInPhase > 0 -> "${status.minutesRemainingInPhase}m ${status.secondsRemainingInPhase}s"
            else -> "${status.secondsRemainingInPhase}s"
        }

        // Live status chip displays the ticking countdown time next to the Sun/Moon icon
        val chipShortCriticalText = countdownRemainingText

        // Compact notification shade card
        val contentTitle = "$phaseTitle • $plantName"
        val contentText = "$countdownRemainingText remaining • Next: $nextPhase at ${status.nextSwitchTimeFormatted}"
        val contentSubText = "${(status.phaseProgressPercent.coerceIn(0f, 1f) * 100).toInt()}%"
        val progressPercent = (status.phaseProgressPercent.coerceIn(0f, 1f) * 100).toInt()

        // Negative timer fix: only use chronometer countdown if the switch epoch is strictly in the future
        val hasFutureCountdown = status.isTimerRunning && targetSwitchEpoch > now

        // On Android 8.0+ (API 26+), attempt direct platform Notification.Builder invocation
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val platformBuilder = Notification.Builder(context, LIVE_CHANNEL_ID)
                    .setSmallIcon(chipIcon)
                    .setContentTitle(contentTitle)
                    .setSubText(contentSubText)
                    .setContentText(contentText)
                    .setShowWhen(true)
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setCategory(Notification.CATEGORY_STATUS)
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(false)
                    .setTicker(chipShortCriticalText)

                if (hasFutureCountdown) {
                    platformBuilder.setWhen(targetSwitchEpoch)
                    platformBuilder.setUsesChronometer(true)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        platformBuilder.setChronometerCountDown(true)
                    }
                } else {
                    platformBuilder.setWhen(now)
                    platformBuilder.setUsesChronometer(false)
                }

                // Android 16 Wavy Progress Indicator (Notification.ProgressStyle)
                // If device has at least Android 16 (API 36), apply wavy progress indicator with tracker icon
                // Otherwise fall back to standard notification progress indicator
                if (Build.VERSION.SDK_INT >= 36) {
                    try {
                        val progressStyle = Notification.ProgressStyle()
                        progressStyle.setProgress(progressPercent)
                        progressStyle.addProgressSegment(Notification.ProgressStyle.Segment(100))
                        val trackerIcon = android.graphics.drawable.Icon.createWithResource(context, chipIcon)
                        progressStyle.setProgressTrackerIcon(trackerIcon)
                        platformBuilder.setStyle(progressStyle)
                    } catch (_: Throwable) {
                        platformBuilder.setProgress(100, progressPercent, false)
                    }
                } else {
                    platformBuilder.setProgress(100, progressPercent, false)
                }

                // Call Notification.Builder#setShortCriticalText via reflection (API 35+)
                try {
                    val setShortCriticalTextMethod = platformBuilder.javaClass.getMethod(
                        "setShortCriticalText",
                        CharSequence::class.java
                    )
                    setShortCriticalTextMethod.invoke(platformBuilder, chipShortCriticalText)
                } catch (_: Throwable) {}

                // Call Notification.Builder#setRequestPromotedOngoing if available
                try {
                    val setPromotedMethod = platformBuilder.javaClass.getMethod(
                        "setRequestPromotedOngoing",
                        Boolean::class.javaPrimitiveType
                    )
                    setPromotedMethod.invoke(platformBuilder, true)
                } catch (_: Throwable) {}

                val nativeNotification = platformBuilder.build().apply {
                    flags = flags or Notification.FLAG_ONGOING_EVENT
                    visibility = Notification.VISIBILITY_PUBLIC
                    extras.putBoolean(EXTRA_REQUEST_PROMOTED_ONGOING, true)
                    extras.putCharSequence(EXTRA_SHORT_CRITICAL_TEXT, chipShortCriticalText)
                    extras.putCharSequence("android.shortCriticalText", chipShortCriticalText)
                    extras.putCharSequence("android.statusChipText", chipShortCriticalText)
                    extras.putCharSequence("android.ongoingActivity.statusText", chipShortCriticalText)
                    extras.putBoolean("android.requestPromotedOngoing", true)
                }

                NotificationManagerCompat.from(context).notify(LIVE_UPDATE_NOTIFICATION_ID, nativeNotification)
                scheduleLightCycleAlarms(context, status, plantId)
                return
            } catch (_: Throwable) {
                // Fall back to NotificationCompat.Builder below if platform builder throws
            }
        }

        // Standard NotificationCompat builder fallback
        val builder = NotificationCompat.Builder(context, LIVE_CHANNEL_ID)
            .setSmallIcon(chipIcon)
            .setContentTitle(contentTitle)
            .setSubText(contentSubText)
            .setContentText(contentText)
            .setProgress(100, progressPercent, false)
            .setShowWhen(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setTicker(chipShortCriticalText)

        if (hasFutureCountdown) {
            builder.setWhen(targetSwitchEpoch)
            builder.setUsesChronometer(true)
            builder.setChronometerCountDown(true)
        } else {
            builder.setWhen(now)
            builder.setUsesChronometer(false)
        }

        // Android 16 Promoted Ongoing Notification & Status Chip Extras
        builder.addExtras(Bundle().apply {
            putBoolean(EXTRA_REQUEST_PROMOTED_ONGOING, true)
            putCharSequence(EXTRA_SHORT_CRITICAL_TEXT, chipShortCriticalText)
            putCharSequence("android.shortCriticalText", chipShortCriticalText)
            putCharSequence("android.statusChipText", chipShortCriticalText)
            putCharSequence("android.ongoingActivity.statusText", chipShortCriticalText)
            putBoolean("android.requestPromotedOngoing", true)
        })

        val notification = builder.build().apply {
            flags = flags or Notification.FLAG_ONGOING_EVENT
            visibility = Notification.VISIBILITY_PUBLIC
            extras.putBoolean(EXTRA_REQUEST_PROMOTED_ONGOING, true)
            extras.putCharSequence(EXTRA_SHORT_CRITICAL_TEXT, chipShortCriticalText)
            extras.putCharSequence("android.shortCriticalText", chipShortCriticalText)
            extras.putCharSequence("android.statusChipText", chipShortCriticalText)
            extras.putCharSequence("android.ongoingActivity.statusText", chipShortCriticalText)
            extras.putBoolean("android.requestPromotedOngoing", true)
        }

        NotificationManagerCompat.from(context).notify(LIVE_UPDATE_NOTIFICATION_ID, notification)
        scheduleLightCycleAlarms(context, status, plantId)
    }

    /**
     * Builds a fallback placeholder notification for starting foreground service if needed.
     */
    fun createDefaultPlaceholderNotification(context: Context): Notification {
        createLiveUpdateChannel(context)
        return NotificationCompat.Builder(context, LIVE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Photoperiod Active")
            .setContentText("Monitoring light cycle timer")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    /**
     * Schedules background AlarmManager triggers for exact phase transitions
     * and battery-conscious 1-minute progress sync while the app is closed.
     */
    fun scheduleLightCycleAlarms(
        context: Context,
        status: LightCycleStatus,
        plantId: Long = -1L
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val now = System.currentTimeMillis()
        val targetSwitchEpoch = if (status.nextSwitchEpochMillis > 0) {
            status.nextSwitchEpochMillis
        } else {
            val remSec = (status.hoursRemainingInPhase * 3600L) + (status.minutesRemainingInPhase * 60L) + status.secondsRemainingInPhase
            now + (remSec * 1000L)
        }

        if (status.isTimerRunning && targetSwitchEpoch > now) {
            // 1. Exact Phase Transition Alarm at switch epoch (RTC_WAKEUP so phase flips immediately without delay)
            val transitionIntent = Intent(context, LightCycleAlarmReceiver::class.java).apply {
                action = LightCycleAlarmReceiver.ACTION_PHASE_TRANSITION
                putExtra(LightCycleAlarmReceiver.EXTRA_PLANT_ID, plantId)
            }
            val transitionPendingIntent = PendingIntent.getBroadcast(
                context,
                ALARM_REQ_TRANSITION,
                transitionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        targetSwitchEpoch,
                        transitionPendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        targetSwitchEpoch,
                        transitionPendingIntent
                    )
                }
            } catch (_: SecurityException) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, targetSwitchEpoch, transitionPendingIntent)
            }

            // 2. Periodic 1-minute Progress Sync while app is closed (RTC non-wakeup to maximize battery)
            val nextMinuteEpoch = ((now / 60000L) + 1L) * 60000L
            if (nextMinuteEpoch < targetSwitchEpoch) {
                val syncIntent = Intent(context, LightCycleAlarmReceiver::class.java).apply {
                    action = LightCycleAlarmReceiver.ACTION_PROGRESS_SYNC
                    putExtra(LightCycleAlarmReceiver.EXTRA_PLANT_ID, plantId)
                }
                val syncPendingIntent = PendingIntent.getBroadcast(
                    context,
                    ALARM_REQ_SYNC,
                    syncIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC,
                        nextMinuteEpoch,
                        syncPendingIntent
                    )
                } else {
                    alarmManager.set(
                        AlarmManager.RTC,
                        nextMinuteEpoch,
                        syncPendingIntent
                    )
                }
            }
        } else {
            cancelLightCycleAlarms(context)
        }
    }

    /**
     * Cancels any scheduled background transition and sync alarms to maximize battery.
     */
    fun cancelLightCycleAlarms(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val transitionIntent = Intent(context, LightCycleAlarmReceiver::class.java).apply {
                action = LightCycleAlarmReceiver.ACTION_PHASE_TRANSITION
            }
            val transitionPending = PendingIntent.getBroadcast(
                context,
                ALARM_REQ_TRANSITION,
                transitionIntent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (transitionPending != null) {
                alarmManager.cancel(transitionPending)
                transitionPending.cancel()
            }

            val syncIntent = Intent(context, LightCycleAlarmReceiver::class.java).apply {
                action = LightCycleAlarmReceiver.ACTION_PROGRESS_SYNC
            }
            val syncPending = PendingIntent.getBroadcast(
                context,
                ALARM_REQ_SYNC,
                syncIntent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (syncPending != null) {
                alarmManager.cancel(syncPending)
                syncPending.cancel()
            }
        } catch (_: Throwable) {}
    }

    /**
     * Cancels the active Light Cycle Live Update notification when the timer is paused,
     * completed, or reset, and cancels background alarms.
     */
    fun cancelLightCycleLiveUpdate(context: Context) {
        try {
            cancelLightCycleAlarms(context)
            val manager = NotificationManagerCompat.from(context)
            manager.cancel(LIVE_UPDATE_NOTIFICATION_ID)
        } catch (_: Throwable) {}
    }

    /**
     * Dispatches a test Live Update notification with the Status Bar Chip for verification.
     */
    fun triggerTestLiveUpdateAlert(
        context: Context,
        plantName: String? = null,
        status: LightCycleStatus? = null,
        plantId: Long = -1L
    ) {
        val testSwitchMillis = System.currentTimeMillis() + (6 * 3600 + 30 * 60) * 1000L
        val effectiveStatus = status ?: LightCycleStatus(
            isLightOn = true,
            currentPhaseName = "LIGHTS ON",
            hoursRemainingInPhase = 6,
            minutesRemainingInPhase = 30,
            secondsRemainingInPhase = 0,
            phaseProgressPercent = 0.64f,
            startTimeFormatted = "06:00 AM",
            nextSwitchTimeFormatted = "20:00",
            totalOnHours = 18,
            totalOffHours = 6,
            presetName = "18/6 Veg",
            isTimerRunning = true,
            isWaitingToStart = false,
            isPausedForMaintenance = false,
            nextSwitchEpochMillis = testSwitchMillis
        )
        updateLightCycleLiveUpdate(
            context = context,
            status = effectiveStatus,
            plantName = plantName ?: "Northern Lights #5",
            plantId = plantId
        )
    }
}
