@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import android.content.Intent
import android.widget.Toast
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.Tune
import com.example.ui.components.ConnectedButtonItem
import com.example.ui.components.M3ConnectedButtonGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.WbSunny
import com.example.ui.components.PrivacyPolicyDialog
import com.example.util.PlantBackupManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.PlantViewModel
import com.example.ui.viewmodel.ThemeMode

@Composable
fun SettingsScreen(
    viewModel: PlantViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentTheme by viewModel.themeMode.collectAsStateWithLifecycle()
    val dynamicColor by viewModel.dynamicColor.collectAsStateWithLifecycle()
    val plants by viewModel.allPlants.collectAsStateWithLifecycle()
    val logs by viewModel.allCareLogs.collectAsStateWithLifecycle()
    val photos by viewModel.allPhotos.collectAsStateWithLifecycle()
    val isChangelogExpanded by viewModel.settingsChangelogExpanded.collectAsStateWithLifecycle()
    val isPrivacyExpanded by viewModel.settingsPrivacyExpanded.collectAsStateWithLifecycle()

    var showResetDialog by remember { mutableStateOf(false) }
    var showChangelogDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (isGranted) {
            viewModel.triggerTestNotification(context)
        }
    }

    val careRemindersEnabled by viewModel.careRemindersEnabled.collectAsStateWithLifecycle()
    val liveTimerChipEnabled by viewModel.liveTimerChipEnabled.collectAsStateWithLifecycle()
    val weeklyBackupEnabled by viewModel.weeklyBackupEnabled.collectAsStateWithLifecycle()
    val backupFolderUri by viewModel.backupFolderUri.collectAsStateWithLifecycle()
    val backupFolderName by viewModel.backupFolderName.collectAsStateWithLifecycle()
    val lastBackupTimestamp by viewModel.lastBackupTimestamp.collectAsStateWithLifecycle()

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            try {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (_: Exception) {}
            val displayName = PlantBackupManager.getFolderDisplayName(context, uri)
            viewModel.setBackupFolderUri(uri, displayName)
            Toast.makeText(context, "Selected backup folder: $displayName", Toast.LENGTH_SHORT).show()
        }
    }

    val zipExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            viewModel.exportCompleteZipBackup(uri) { success ->
                if (success) {
                    Toast.makeText(context, "Full Cultivation Backup (.zip with photos) exported successfully!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "ZIP export failed. Please check folder permissions.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val jsonExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportDatabaseToUri(uri, isJson = true) { success ->
                if (success) {
                    Toast.makeText(context, "Full Room Database (JSON) exported successfully!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Export failed. Please check folder permissions.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            viewModel.exportDatabaseToUri(uri, isJson = false) { success ->
                if (success) {
                    Toast.makeText(context, "Full Room Database (CSV) exported successfully!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Export failed. Please check folder permissions.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    var pendingImportData by remember { mutableStateOf<PlantBackupManager.ImportPreviewData?>(null) }
    var importErrorMessage by remember { mutableStateOf<String?>(null) }
    var isImporting by remember { mutableStateOf(false) }
    var isExportImportExpanded by remember { mutableStateOf(false) }

    val unifiedImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            isImporting = true
            viewModel.parseImportFile(uri, isJson = true) { data, error ->
                isImporting = false
                if (data != null) {
                    pendingImportData = data
                } else {
                    importErrorMessage = error ?: "Could not parse backup file."
                }
            }
        }
    }

    val csvImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            isImporting = true
            viewModel.parseImportFile(uri, isJson = false) { data, error ->
                isImporting = false
                if (data != null) {
                    pendingImportData = data
                } else {
                    importErrorMessage = error ?: "Could not parse CSV backup."
                }
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Expressive Screen Header
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Eco,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = "Settings & Vault",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Cultivation preferences, alerts, and offline storage",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Group 1: Appearance & Theme (M3 Expressive Grouped Card)
        item {
            ExpressiveSettingsGroup(
                icon = Icons.Filled.Palette,
                title = "Appearance & Theme",
                subtitle = "Select system contrast mode and dynamic Monet colors"
            ) {
                // Theme Mode Connected Button Group (M3 Expressive: compact pill shape, shape morphing on selection & press)
                val themeItems = remember {
                    listOf(
                        ConnectedButtonItem(ThemeMode.DARK, "Dark", Icons.Filled.DarkMode, "theme_dark"),
                        ConnectedButtonItem(ThemeMode.LIGHT, "Light", Icons.Filled.LightMode, "theme_light"),
                        ConnectedButtonItem(ThemeMode.SYSTEM, "System", Icons.Filled.SettingsBrightness, "theme_system")
                    )
                }
                M3ConnectedButtonGroup(
                    items = themeItems,
                    selectedValue = currentTheme,
                    onItemSelected = { viewModel.setThemeMode(it) }
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Monet Dynamic Colors",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 2.dp, bottom = 6.dp)
                    )
                    // Monet Connected Button Group: Monet vs Standard with M3 Expressive shape morphing
                    val monetItems = remember {
                        listOf(
                            ConnectedButtonItem(true, "Monet", Icons.Filled.Palette, "monet_dynamic"),
                            ConnectedButtonItem(false, "Standard", Icons.Filled.Tune, "monet_standard")
                        )
                    }
                    M3ConnectedButtonGroup(
                        items = monetItems,
                        selectedValue = dynamicColor,
                        onItemSelected = { viewModel.setDynamicColor(it) }
                    )
                }
            }
        }

        // Group 2: Notifications & Local Alerts (M3 Expressive Grouped Card)
        item {
            ExpressiveSettingsGroup(
                icon = Icons.Filled.Notifications,
                title = "Local Care Alerts & Photoperiod Alarms",
                subtitle = "Precise device notifications for feeding & light cycles"
            ) {
                // 2-Day Watering Reminder Schedule Connected Button Group (M3 Expressive)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.WaterDrop,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "2-Day Care & Watering Alerts",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Notify when 48h have passed since last hydration",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        val careAlertItems = remember {
                            listOf(
                                ConnectedButtonItem(true, "Alerts", Icons.Filled.NotificationsActive, "btn_alerts_enabled"),
                                ConnectedButtonItem(false, "No Alerts", Icons.Filled.NotificationsOff, "btn_alerts_disabled")
                            )
                        }
                        M3ConnectedButtonGroup(
                            items = careAlertItems,
                            selectedValue = careRemindersEnabled,
                            onItemSelected = { viewModel.setCareRemindersEnabled(it) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = androidx.compose.foundation.BorderStroke(0.6.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(8.dp)
                            ) {}
                            Text(
                                text = "Maintenance & Alarm System Active",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = "100% On-Device",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Live Timer Status Bar Chip Connected Button Group (M3 Expressive)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_live_timer_toggle")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (liveTimerChipEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.WbSunny,
                                        contentDescription = null,
                                        tint = if (liveTimerChipEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Live Timer Status Chip",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (liveTimerChipEnabled) {
                                        "Status bar chip and lock screen countdown are enabled for active light cycles."
                                    } else {
                                        "Turned off. Photoperiod timers will run in-app without status bar chips."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        val liveChipItems = remember {
                            listOf(
                                ConnectedButtonItem(true, "Live Chip", Icons.Filled.WbSunny, "btn_live_chip_on"),
                                ConnectedButtonItem(false, "No Chip", Icons.Filled.Close, "btn_live_chip_off")
                            )
                        }
                        M3ConnectedButtonGroup(
                            items = liveChipItems,
                            selectedValue = liveTimerChipEnabled,
                            onItemSelected = { viewModel.setLiveTimerChipEnabled(it) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Compact Test Alert Buttons in a FlowRow with fully rounded CircleShape
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                viewModel.triggerTestWateringDueAlert(context)
                            }
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.WaterDrop, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = {
                            Text("Watering Alert", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        },
                        shape = CircleShape,
                        modifier = Modifier.testTag("btn_test_watering_due")
                    )

                    AssistChip(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                viewModel.triggerTestMaintenanceAlert(context)
                            }
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = {
                            Text("Light Off Alert", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        },
                        shape = CircleShape,
                        modifier = Modifier.testTag("btn_test_maintenance_alert")
                    )

                    AssistChip(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                viewModel.triggerTestNotification(context)
                            }
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = {
                            Text(
                                text = if (!hasNotificationPermission) "Grant Permission" else "General Alert",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        },
                        shape = CircleShape,
                        modifier = Modifier.testTag("btn_test_notification")
                    )

                    AssistChip(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                viewModel.triggerTestLiveUpdateAlert(context)
                            }
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.WbSunny, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = {
                            Text("Live Timer Chip", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        },
                        shape = CircleShape,
                        modifier = Modifier.testTag("btn_test_live_update")
                    )
                }
            }
        }

        // Group 3: Local Database & Storage Vault (M3 Expressive Grouped Card)
        item {
            ExpressiveSettingsGroup(
                icon = Icons.Filled.Storage,
                title = "Offline SQLite & Room Safekeeping",
                subtitle = "Export or preserve your entire cultivation ledger on internal storage"
            ) {
                // Metric Pills
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricPill(label = "Active Cultivars", count = plants.size.toString())
                    MetricPill(label = "Care Commits", count = logs.size.toString())
                    MetricPill(label = "Vault Photos", count = photos.size.toString())
                    MetricPill(label = "Cloud Calls", count = "0 (Offline)")
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Database Export Section (JSON & CSV via Storage Access Framework)
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.FileDownload,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Database Safekeeping & Backup",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Export all plants, logs, photos & settings to files",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Automated Weekly Backups Card (M3 Expressive)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("card_automated_weekly_backup")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Automated Weekly Backups",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Auto-saves a complete timestamped JSON snapshot every 7 days to your chosen folder.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = weeklyBackupEnabled,
                                        onCheckedChange = { isChecked ->
                                            if (isChecked && backupFolderUri == null) {
                                                folderPickerLauncher.launch(null)
                                            }
                                            viewModel.setWeeklyBackupEnabled(isChecked)
                                        },
                                        modifier = Modifier.testTag("switch_weekly_backup")
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Folder Destination Row
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Folder,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Column {
                                                Text(
                                                    text = "Destination Folder",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = backupFolderName ?: "Tap 'Select' to pick folder",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (backupFolderName != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        OutlinedButton(
                                            onClick = { folderPickerLauncher.launch(null) },
                                            shape = CircleShape,
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                            modifier = Modifier.testTag("btn_select_backup_folder")
                                        ) {
                                            Text(
                                                text = if (backupFolderUri == null) "Select Folder" else "Change",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                if (lastBackupTimestamp > 0L) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    val lastDate = SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.getDefault()).format(Date(lastBackupTimestamp))
                                    Text(
                                        text = "Last automated backup: $lastDate",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Button(
                                    onClick = {
                                        if (backupFolderUri == null) {
                                            folderPickerLauncher.launch(null)
                                        } else {
                                            viewModel.triggerBackupNow { success, fileName ->
                                                if (success) {
                                                    Toast.makeText(context, "Saved backup $fileName to chosen folder", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "Backup failed. Please check folder permissions.", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    },
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("btn_backup_now_folder")
                                ) {
                                    Icon(Icons.Filled.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Backup Now to Folder", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Collapsible Export/Import Section
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isExportImportExpanded = !isExportImportExpanded }
                                .testTag("btn_toggle_export_import")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Filled.SyncAlt,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Column {
                                        Text(
                                            text = "Export/Import",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Manual file transfers, JSON/CSV ledgers & full ZIP bundles",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = if (isExportImportExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                    contentDescription = if (isExportImportExpanded) "Collapse" else "Expand",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = isExportImportExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp)
                            ) {
                                // Manual File Export Section
                                Text(
                                    text = "Manual Export / Transfer",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                // Hero Full ZIP Backup Button (Photos + Ledger)
                                Button(
                                    onClick = {
                                        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                                        val fileName = "planthub_complete_backup_$timeStamp.zip"
                                        zipExportLauncher.launch(fileName)
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("btn_export_zip")
                                ) {
                                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Export Full ZIP Bundle (Database + Photos)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Includes original resolution milestone pictures & full ledger", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f))
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            val fileName = "planthub_backup_${System.currentTimeMillis()}.json"
                                            jsonExportLauncher.launch(fileName)
                                        },
                                        shape = CircleShape,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("btn_export_json")
                                    ) {
                                        Icon(Icons.Filled.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Export JSON", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val fileName = "planthub_backup_${System.currentTimeMillis()}.csv"
                                            csvExportLauncher.launch(fileName)
                                        },
                                        shape = CircleShape,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("btn_export_csv")
                                    ) {
                                        Icon(Icons.Filled.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Export CSV", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Manual File Import Section
                                Text(
                                    text = "Manual Import / Restore",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                Button(
                                    onClick = {
                                        unifiedImportLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/json", "text/*", "*/*"))
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("btn_import_bundle")
                                ) {
                                    Icon(Icons.Filled.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Import Backup (ZIP Bundle or JSON)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Restores full-resolution photos, cultivars, care logs & schedules", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f))
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedButton(
                                    onClick = {
                                        csvImportLauncher.launch(arrayOf("text/*", "text/comma-separated-values", "text/csv", "*/*"))
                                    },
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("btn_import_csv")
                                ) {
                                    Icon(Icons.Filled.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Import CSV (Spreadsheet / Tables)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = androidx.compose.foundation.BorderStroke(0.6.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Hardware Sandboxed • Room SQLite Engine",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { showResetDialog = true },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_seed_sample_data")
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reload Starter Cultivars & 60-Day Care Ledger", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        // Group: Release Notes & Architectural Changelog (M3 Expressive Grouped Card)
        item {
            ExpressiveSettingsGroup(
                icon = Icons.Filled.AutoAwesome,
                title = "Release Notes & Changelog",
                subtitle = "Version notes, UI evolutions & engine upgrades"
            ) {
                // Header with version badge and accordion toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "v1.7 Current",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Text(
                            text = "Onboarding, Cultivar Ergonomics & Readability",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = { viewModel.setSettingsChangelogExpanded(!isChangelogExpanded) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isChangelogExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = if (isChangelogExpanded) "Collapse" else "Expand",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Expandable accordion section
                AnimatedVisibility(visible = isChangelogExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppReleases.firstOrNull()?.highlights?.forEach { h ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = h.icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp).padding(top = 2.dp)
                                    )
                                    Column {
                                        Text(
                                            text = h.title,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = h.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { showChangelogDialog = true },
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_open_full_changelog")
                ) {
                    Icon(
                        imageVector = Icons.Filled.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "View Full Release Notes & Architecture History",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { viewModel.setOnboardingCompleted(false) },
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_replay_onboarding")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Spa,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Replay Feature & Function Tour",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Group 4: Privacy & Philosophy (Expandable Accordion Menu Point)
        item {
            ExpressiveSettingsGroup(
                icon = Icons.Filled.Shield,
                title = "Privacy & Philosophy",
                subtitle = "Vibecoded manifesto, on-device policy & architecture"
            ) {
                // Header with v1.6 version badge and accordion toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                text = "v1.7 Active",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Text(
                            text = "Zero-Cloud Guarantee",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = { viewModel.setSettingsPrivacyExpanded(!isPrivacyExpanded) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isPrivacyExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = if (isPrivacyExpanded) "Collapse Privacy" else "Expand Privacy",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "100% on-device SQLite storage. Zero tracking or accounts.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Expandable Privacy & Solo Vibecoder Disclosure Highlights Accordion
                AnimatedVisibility(visible = isPrivacyExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PrivacyHighlightRow(
                            icon = Icons.Filled.Code,
                            title = "Solo Vibecoder Development",
                            description = "Vibecoded product created by a solo vibecoder \"dev\". Rapid iterations could have edge-case bugs that haven't been caught yet."
                        )
                        PrivacyHighlightRow(
                            icon = Icons.Filled.CloudOff,
                            title = "100% On-Device Boundary",
                            description = "Zero telemetry, analytics, cloud databases, accounts, or background pings. All cultivation metrics live in local SQLite."
                        )
                        PrivacyHighlightRow(
                            icon = Icons.Filled.Storage,
                            title = "Active Plant State Memory",
                            description = "Cultivars, timers, and ledgers mirror to persistent device backup so app updates never reset your ongoing cycle."
                        )
                        PrivacyHighlightRow(
                            icon = Icons.Filled.PhotoLibrary,
                            title = "Sandboxed Photo Vault",
                            description = "Attached milestone pictures remain protected inside local app sandbox using zero-permission Android Photo Picker."
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { showPrivacyDialog = true },
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_open_full_privacy_dialog")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "View Full Disclosure & Privacy Policy Manifesto",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reload Starter Cultivars?") },
            text = { Text("This will reset the local database with pre-configured starter cultivars (Gorilla Glue, OG Kush, Northern Lights) and 60 days of care commit history.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetDatabaseWithSampleData()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Reload Starter Data")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showChangelogDialog) {
        ChangelogDialog(
            onDismiss = { showChangelogDialog = false }
        )
    }

    if (showPrivacyDialog) {
        PrivacyPolicyDialog(
            onDismiss = { showPrivacyDialog = false }
        )
    }

    pendingImportData?.let { importData ->
        AlertDialog(
            onDismissRequest = { pendingImportData = null },
            shape = RoundedCornerShape(24.dp),
            icon = {
                Icon(
                    imageVector = Icons.Filled.WarningAmber,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Restore Cultivation Ledger?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Importing \"${importData.fileName}\" (${importData.sourceFormat}) will replace all current database records with the imported ledger.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Detected Records:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "• Cultivars: ${importData.plants.size}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "• Care Log Entries: ${importData.logs.size}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (importData.bundledImagesCount > 0) {
                                Text(
                                    text = "• Progress Photos: ${importData.photos.size} (${importData.bundledImagesCount} Full-Resolution Image Files Bundled)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Text(
                                    text = "• Progress Photos: ${importData.photos.size}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            if (importData.reminders.isNotEmpty()) {
                                Text(
                                    text = "• Reminder Schedules: ${importData.reminders.size}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                    if (importData.bundledImagesCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PhotoLibrary,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "Full-Resolution Photo Backup Detected",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "${importData.bundledImagesCount} original-quality images will be restored directly to your Photo Vault storage.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        text = "Warning: Any current cultivars, timers, and logs not in this file will be permanently overwritten.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toRestore = importData
                        pendingImportData = null
                        viewModel.restoreDatabaseFromImport(toRestore) { success, message ->
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        }
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("btn_confirm_restore_database")
                ) {
                    Text("Replace & Restore", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { pendingImportData = null },
                    shape = CircleShape,
                    modifier = Modifier.testTag("btn_cancel_restore_database")
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    importErrorMessage?.let { errorMsg ->
        AlertDialog(
            onDismissRequest = { importErrorMessage = null },
            shape = RoundedCornerShape(24.dp),
            icon = {
                Icon(
                    imageVector = Icons.Filled.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Import Failed",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = errorMsg,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = { importErrorMessage = null },
                    shape = CircleShape,
                    modifier = Modifier.testTag("btn_dismiss_import_error")
                ) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun PrivacyHighlightRow(
    icon: ImageVector,
    title: String,
    description: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp).padding(top = 2.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ExpressiveSettingsGroup(
    icon: ImageVector,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            content()
        }
    }
}

@Composable
private fun MetricPill(label: String, count: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        border = androidx.compose.foundation.BorderStroke(0.6.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "$label:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = count,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
