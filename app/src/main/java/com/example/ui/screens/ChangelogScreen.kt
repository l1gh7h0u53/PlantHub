package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

data class FriendlyHighlight(
    val title: String,
    val description: String,
    val icon: ImageVector
)

data class FriendlyRelease(
    val version: String,
    val status: String,
    val releaseTitle: String,
    val date: String,
    val heroIcon: ImageVector,
    val highlights: List<FriendlyHighlight>,
    val architecturalSummary: String
)

val AppReleases = listOf(
    FriendlyRelease(
        version = "v1.7",
        status = "CURRENT",
        releaseTitle = "Interactive Onboarding Tour, Collapsible Export/Import Hub, Cultivar Ergonomics & High-Contrast System",
        date = "October 2026",
        heroIcon = Icons.Filled.Spa,
        highlights = listOf(
            FriendlyHighlight(
                title = "Full-Fidelity Photo Vault & Database ZIP Backup Bundle",
                description = "Export and restore entire cultivation databases bundled with uncompressed, full-resolution milestone photos in portable .zip archives, preserving authentic visual fidelity across device migrations.",
                icon = Icons.Filled.FolderZip
            ),
            FriendlyHighlight(
                title = "Universal CSV Auto-Detection & Resilient Ingestion",
                description = "Automatically identifies standalone CSV tables without section tags, normalizes camelCase, snake_case, and human-friendly headers, and guarantees zero data drops via automatic primary cultivar linking.",
                icon = Icons.Filled.TableChart
            ),
            FriendlyHighlight(
                title = "Interactive First-Launch Onboarding Carousel",
                description = "Guided multi-step tour explaining photoperiod lighting, M3 care ledgers, photo vaults, and backup portability in simple and technical English. Replayable anytime from Settings.",
                icon = Icons.Filled.Spa
            ),
            FriendlyHighlight(
                title = "Clean Garden Zero-State with Quick Launch",
                description = "Removed automated weed plant seeding on fresh launch, providing a clean, uncluttered garden with a centered 'Add First Plant' action.",
                icon = Icons.Filled.Eco
            ),
            FriendlyHighlight(
                title = "Collapsible Export/Import Hub in Settings",
                description = "Unified manual file exports, full ZIP backups, and restore operations under a single, animated collapsible 'Export/Import' card.",
                icon = Icons.Filled.SyncAlt
            ),
            FriendlyHighlight(
                title = "New Cultivar Profile UI Refinements",
                description = "Alphabetized botanical icons, M3 Expressive substrate popup list with segmented gaps and filled items, compact growth stage pills, and a red pill-shaped Cancel button.",
                icon = Icons.Filled.Tune
            ),
            FriendlyHighlight(
                title = "High-Contrast Dark Mode & Monet Readability Overhaul",
                description = "Elevated text contrast across the Floating Bottom Menu Bar, Photo Vault cards, and Change Growth Stage dialog, eliminating dark-on-dark legibility issues across standard dark and dynamic Monet schemes.",
                icon = Icons.Filled.Palette
            )
        ),
        architecturalSummary = "Standalone ZIP Photo Bundling • Multi-Table CSV Auto-Detection • Interactive Feature Carousel • Clean Zero-State Garden • Collapsible Export/Import Hub • M3 Segmented Substrate List • Red Pill Cancel Button • High-Contrast Dark/Monet Theming"
    ),
    FriendlyRelease(
        version = "v1.6.1",
        status = "STABLE",
        releaseTitle = "Full JSON & Multi-Table CSV Cultivation Ledger Import Engine",
        date = "October 2026",
        heroIcon = Icons.Filled.FileUpload,
        highlights = listOf(
            FriendlyHighlight(
                title = "Paired Manual Import Action Row in Settings",
                description = "Added an ergonomic 'Manual Import / Restore' row directly beneath export controls with fully rounded pill-shaped buttons for 'Import JSON' (tertiaryContainer) and 'Import CSV' (outlined).",
                icon = Icons.Filled.FileUpload
            ),
            FriendlyHighlight(
                title = "Universal Multi-Table CSV Auto-Detection",
                description = "Implemented section-aware CSV parsing that automatically detects and extracts # TABLE: PLANTS, CARE_LOGS, PHOTOS, and REMINDER_SETTINGS, while supporting single-table cultivar CSVs.",
                icon = Icons.Filled.TableChart
            ),
            FriendlyHighlight(
                title = "Multi-Schema Key & Wrapped Object Ingestion",
                description = "Transparently resolves JSON backup formats supporting both careLogs/logs, photos/photoEntries, and reminderSettings/reminders, as well as wrapped root data structures.",
                icon = Icons.Filled.Description
            ),
            FriendlyHighlight(
                title = "Automatic Primary Cultivar Linking for Foreign Keys",
                description = "Preserves 100% of care logs and milestone entries by automatically associating unlinked legacy plant IDs to the first restored cultivar, preventing SQLite foreign-key constraint rejections.",
                icon = Icons.Filled.Storage
            ),
            FriendlyHighlight(
                title = "Resilient Botanical Photo Placeholders",
                description = "Milestone entries with missing local image files now render beautiful botanical checkpoint cards preserving full day numbers, growth stage badges, canopy height metrics, and observation logs.",
                icon = Icons.Filled.PhotoLibrary
            ),
            FriendlyHighlight(
                title = "Pre-Import Safety Preview & Confirmation Modal",
                description = "Inspects and previews exact record counts for cultivars, care logs, photos, and reminder schedules before replacing records, preventing accidental data loss.",
                icon = Icons.Filled.CheckCircle
            ),
            FriendlyHighlight(
                title = "Immediate Chronological Timeline Expansion",
                description = "Automatically toggles the care ledger to full history mode upon import so all restored historical watering, misting, feeding, and LST events are immediately visible.",
                icon = Icons.Filled.History
            )
        ),
        architecturalSummary = "Full JSON & Multi-Table CSV Import • SAF Document Picker • Multi-Schema Key Ingestion • Automatic Cultivar ID Remapping • Botanical Photo Checkpoint Placeholders • Clean Database Replacement"
    ),
    FriendlyRelease(
        version = "v1.6",
        status = "STABLE",
        releaseTitle = "M3 Expressive Outlined Connected Groups, Background Ticker Service & Rounded Botanical Aesthetics",
        date = "October 2026",
        heroIcon = Icons.Filled.Tune,
        highlights = listOf(
            FriendlyHighlight(
                title = "Material 3 Expressive Outlined Connected Button Groups",
                description = "Re-architected connected button groups strictly following official M3 Expressive guidelines with an outlined container, interior segment dividers, independent pressed shape compression, and animated selection shape morphing between round and square.",
                icon = Icons.Filled.Tune
            ),
            FriendlyHighlight(
                title = "Continuous Live Status Chip Background Service",
                description = "Implemented LightCycleTickerService to continuously update the Android 16 live status countdown in the status bar and lock screen without freezing when closing the app, combined with zero-delay clock recalculation on resume.",
                icon = Icons.Filled.Timer
            ),
            FriendlyHighlight(
                title = "Unified Settings Connected Button Groups",
                description = "Converted 2-Day Care & Watering Alerts (Alerts / No Alerts) and Live Timer Status Chip (Live Chip / No Chip) into connected button groups matching Theme Mode and Monet Dynamic Colors.",
                icon = Icons.Filled.NotificationsActive
            ),
            FriendlyHighlight(
                title = "Condensed Growth Stage Switcher Dialog",
                description = "Redesigned the Change Growth Stage dialog into compact single-line pill rows with adjacent photoperiod hour badges (CircleShape), eliminating redundant horizontal whitespace.",
                icon = Icons.Filled.Eco
            ),
            FriendlyHighlight(
                title = "Streamlined Cultivation Care Options",
                description = "Refined quick cultivation action tiles into compact dimensions with reduced padding and fully rounded pill contours (24dp and 26dp) for an ergonomic, tactile feel.",
                icon = Icons.Filled.WaterDrop
            ),
            FriendlyHighlight(
                title = "Fully Rounded Dialogs & Care Action Ledgers",
                description = "Upgraded Today's Care Action logged entries, Photo Vault Checkpoint inputs (20dp), Observation Notes alignment, and all commit and cancel action buttons to fully rounded CircleShape styling.",
                icon = Icons.Filled.CheckCircle
            )
        ),
        architecturalSummary = "M3 Expressive Outlined Connected Groups • LightCycleTickerService Background Sync • Rounded Dialogs & Pill Buttons • Condensed Stage Selector • Zero-Lag System Clock Resume"
    ),
    FriendlyRelease(
        version = "v1.5",
        status = "STABLE",
        releaseTitle = "M3 Expressive Connected Groups, Android 16 Wavy Notification ProgressStyle & Background Alarm Transitions",
        date = "October 2026",
        heroIcon = Icons.Filled.Waves,
        highlights = listOf(
            FriendlyHighlight(
                title = "Android 16 Notification Wavy ProgressStyle",
                description = "Integrated the official Android 16 (API 36) Notification.ProgressStyle with phase segments and dynamic tracker icon for photoperiod notifications, with graceful standard progress fallback on earlier Android versions.",
                icon = Icons.Filled.Waves
            ),
            FriendlyHighlight(
                title = "Material 3 Expressive Connected Button Groups",
                description = "Upgraded Theme Mode (Dark / Light / System) and Monet Dynamic Colors (Monet vs Standard) into adaptive connected button groups featuring interactive shape-morphing physics on press and selection with compact touch targets.",
                icon = Icons.Filled.Palette
            ),
            FriendlyHighlight(
                title = "Fully Rounded Aesthetics across Tracker & Controls",
                description = "Refined Quick Care actions, Photoperiod timer controls (Start, Pause, Reset, Resume), and Activity Matrix view chips (16 Weeks, Yearly, Month Focus) with fully rounded pill shapes for an inviting, expressive visual feel.",
                icon = Icons.Filled.CheckCircle
            ),
            FriendlyHighlight(
                title = "Live Status Chip with Sun & Moon Icons + Live Ticking Countdown",
                description = "Upgraded the Live Updates Status Chip to display official Material Symbols Light Mode (Sun) for Lights ON and Nightlight (Moon) for Lights OFF, paired with a live ticking countdown timer.",
                icon = Icons.Filled.Timer
            ),
            FriendlyHighlight(
                title = "Background AlarmManager Phase Transitions & Sync",
                description = "Introduced LightCycleAlarmReceiver with exact alarms at the phase boundary, preventing negative timers (-00:01) by instantly transitioning phases, combined with battery-conscious 1-minute progress syncing when the app is closed.",
                icon = Icons.Filled.Alarm
            ),
            FriendlyHighlight(
                title = "Streamlined Single-Line Compact Alert Notifications",
                description = "Redesigned 48h watering alerts, photoperiod maintenance alerts, and care reminders into clean, single-line notifications eliminating multi-paragraph text clutter while showing cultivar name and essential care actions.",
                icon = Icons.Filled.WaterDrop
            )
        ),
        architecturalSummary = "Android 16 Notification.ProgressStyle • M3 Expressive Connected Button Groups • Fully Rounded Tracker Components • Sun/Moon Live Chip Countdown • Background AlarmManager Exact Transitions"
    ),
    FriendlyRelease(
        version = "v1.4",
        status = "STABLE",
        releaseTitle = "Live Update Status Chip Notification, Full DB Export, Light Maintenance Pause & Growth Stage Switcher",
        date = "October 2026",
        heroIcon = Icons.Filled.Storage,
        highlights = listOf(
            FriendlyHighlight(
                title = "Live Update Notification & Status Bar Chip",
                description = "Android 16 promoted ongoing live update notification featuring a compact real-time photoperiod status chip in the Android status bar and lock screen. Displays remaining on/off countdown, next switch time, and phase progress without unlocking the phone.",
                icon = Icons.Filled.Timer
            ),
            FriendlyHighlight(
                title = "Room Database JSON & CSV Export",
                description = "Export all local cultivation data—including plants, care logs, photos, and settings—to JSON or multi-table CSV files via Android Storage Access Framework destination picker for safe local backups.",
                icon = Icons.Filled.Storage
            ),
            FriendlyHighlight(
                title = "Light Cycle Maintenance Auto-Pause & Resume",
                description = "Photoperiod lights automatically pause when the dark phase ends so growers can perform plant care and maintenance. Includes a dedicated notification and a 'Resume Light Cycle' confirmation dialog before starting the next day's on-phase.",
                icon = Icons.Filled.Build
            ),
            FriendlyHighlight(
                title = "Bi-Daily 48h Watering & Hydration Alerts",
                description = "Background monitor tracks the time elapsed since each cultivar's last watering or feeding, dispatching care reminders when 48 hours have passed. Fully toggleable with on-demand test alerts.",
                icon = Icons.Filled.NotificationsActive
            ),
            FriendlyHighlight(
                title = "Day-Scoped Care Timeline in Tracker",
                description = "Tracker screen timeline defaults cleanly to today's care actions, keeping daily management uncluttered while preserving complete historical records in the Activity Matrix ledger and allowing an instant toggle to view all history.",
                icon = Icons.Filled.CheckCircle
            ),
            FriendlyHighlight(
                title = "Growth Stage Dialog & Quick Chips Removal",
                description = "Removed accidental quick-selection chips on plant cards so cultivars can only transition growth stages through the dedicated interactive stage dialog with confirmation, protecting grow logs from accidental stage mutations.",
                icon = Icons.Filled.Eco
            ),
            FriendlyHighlight(
                title = "Hybrid Substrate Selector in New Cultivar Profile",
                description = "Cultivar creator features an ExposedDropdownMenuBox populated with standard media (Living Soil, Coco Coir/Perlite, Hydroponic DWC) while maintaining an editable text field for custom grower recipes.",
                icon = Icons.Filled.AutoAwesome
            ),
            FriendlyHighlight(
                title = "Adaptive Material 3 Light Turn-On Time Picker",
                description = "Transformed numeric hour input into an expressive Material 3 TimePicker dialog with 12-hour AM/PM formatting and responsive dial/input layout.",
                icon = Icons.Filled.Schedule
            ),
            FriendlyHighlight(
                title = "Interactive Elapsed Days Progression Curve in Vault",
                description = "Expandable analytics card in the Vault charting cumulative days elapsed since the first photo checkpoint, featuring interactive node inspection with photo thumbnails and date badges.",
                icon = Icons.Filled.Speed
            )
        ),
        architecturalSummary = "Android 16 Live Updates (POST_PROMOTED_NOTIFICATIONS & Status Chip) • SAF JSON/CSV Exporters • Lifecycle Light Pause Engine • 48h Care Alert Scheduler"
    ),
    FriendlyRelease(
        version = "v1.3",
        status = "STABLE",
        releaseTitle = "Private Camera Sandbox, 7-Day Milestone Lock & M3 Expressive Overhaul",
        date = "October 2026",
        heroIcon = Icons.Filled.CameraAlt,
        highlights = listOf(
            FriendlyHighlight(
                title = "Private Camera Sandbox Storage",
                description = "Direct system camera capture saves photos strictly into private internal app sandbox storage, never cluttering or leaking into the phone's public gallery or Google Photos.",
                icon = Icons.Filled.Lock
            ),
            FriendlyHighlight(
                title = "Automated Room Photo Persistence",
                description = "Daily photos taken via the camera or imported via Photo Picker are automatically registered into the Room database, keyed by date, cultivar ID, growth stage, day number, and canopy height.",
                icon = Icons.Filled.Storage
            ),
            FriendlyHighlight(
                title = "7-Day Milestone Locking & Comparison",
                description = "Vault displays daily progress shots during the first 7 days, then locks intermediate daily photos to focus strictly on locked 7-day milestone comparison checkpoints (Day 1, 7, 14, 21...).",
                icon = Icons.Filled.CheckCircle
            ),
            FriendlyHighlight(
                title = "M3 Expressive Settings & Vault Redesign",
                description = "Appearance, Local Care Alerts, and Offline Storage re-engineered with tonal badges, live sandboxing indicators, and streamlined switches. Renamed to Monet Dynamic Colors.",
                icon = Icons.Filled.Palette
            ),
            FriendlyHighlight(
                title = "Plant Data Update Preservation",
                description = "User cultivars and custom crops are remembered and preserved in local Room tables across app updates without resetting to standard starter examples.",
                icon = Icons.Filled.Eco
            ),
            FriendlyHighlight(
                title = "M3 Expressive Speed Dial Camera FAB",
                description = "Consolidated dual Vault FABs into a single compact Camera FAB that smoothly morphs shape and color, springs into a close icon on tap, and vertically expands Attach Photo and Take Daily Photo options.",
                icon = Icons.Filled.Speed
            ),
            FriendlyHighlight(
                title = "Privacy Highlights Accordion & Disclosure Modal",
                description = "Inline expandable highlights accordion for Privacy & Philosophy in Settings, paired with a full solo vibecoder terms & offline data sovereignty disclosure dialog.",
                icon = Icons.Filled.Code
            )
        ),
        architecturalSummary = "Hardware-Sandboxed Private Image Storage • 7-Day Milestone Lock Engine • 100% Offline SQLite Room Persistence • M3 Expressive Design"
    ),
    FriendlyRelease(
        version = "v1.2",
        status = "STABLE",
        releaseTitle = "Official Cannabis Icon, Fluid Bouncy Navigation & Form Polish",
        date = "October 2026",
        heroIcon = Icons.Filled.AutoAwesome,
        highlights = listOf(
            FriendlyHighlight(
                title = "Official Google Fonts Cannabis Icon",
                description = "Native Material Symbols Outlined vector cannabis icon validated for Android 13+ Minimal and Themed Icons.",
                icon = Icons.Filled.Eco
            ),
            FriendlyHighlight(
                title = "Fluid Bouncy Bottom Menu Bar",
                description = "Snappy spring bounce physics with subtle scale pulse and smooth indicator slide; streamlined to 4 ergonomic tabs.",
                icon = Icons.Filled.Speed
            ),
            FriendlyHighlight(
                title = "Elevated Floating Action Buttons",
                description = "Add Plant FAB on Plants and Attach Photo FAB on Vault float 96.dp above the bottom navigation bar without collision.",
                icon = Icons.Filled.CameraAlt
            ),
            FriendlyHighlight(
                title = "New Cultivar Profile Form Polish",
                description = "2x2 growth stage grid avoiding text squishing, tactile A.M./P.M. toggle for Light Turn-On Hour, simplified Notes & Strategy label, fully rounded buttons.",
                icon = Icons.Filled.Schedule
            ),
            FriendlyHighlight(
                title = "Integrated Changelog in Settings",
                description = "Changelog and release notes moved inside Settings with expandable accordion preview and full historical viewer popup.",
                icon = Icons.Filled.Code
            ),
            FriendlyHighlight(
                title = "Notification Engine & Theme Persistence",
                description = "Dedicated vector notification icon, high priority channel, and persistent SharedPreferences for Monet dynamic color & Dark/Light theme.",
                icon = Icons.Filled.NotificationsActive
            )
        ),
        architecturalSummary = "100% Offline SQLite Room Database • Material 3 Expressive Design • Zero External Dependencies"
    ),
    FriendlyRelease(
        version = "v1.0",
        status = "STABLE",
        releaseTitle = "Production Launch & Expressive UI",
        date = "October 2026",
        heroIcon = Icons.Filled.AutoAwesome,
        highlights = listOf(
            FriendlyHighlight(
                title = "Interactive Photo Progression Vault",
                description = "Take or attach canopy photos with mandatory capture protection and unlock 7-day before/after growth reviews.",
                icon = Icons.Filled.CameraAlt
            ),
            FriendlyHighlight(
                title = "System-Clock Light Cycle HUD",
                description = "Real-time countdown gauge for 18/6, 12/12, and 20/4 photoperiods with intuitive Start, Pause, and Reset controls.",
                icon = Icons.Filled.Timer
            ),
            FriendlyHighlight(
                title = "GitHub Care Heatmap Matrix",
                description = "Visual 16-week, monthly, and yearly activity grids capturing daily watering, spraying, and nutrient commits.",
                icon = Icons.Filled.GridOn
            ),
            FriendlyHighlight(
                title = "Offline Text Report Exporter",
                description = "Generate and share clean plain-text cultivation logs (.txt) without any cloud accounts or trackers.",
                icon = Icons.Filled.Eco
            )
        ),
        architecturalSummary = "100% On-Device Room Database • Zero Cloud Dependencies • Material 3 Expressive Design"
    ),
    FriendlyRelease(
        version = "v0.9",
        status = "STABLE",
        releaseTitle = "Photoperiod Engine & Smart Alerts",
        date = "October 2026",
        heroIcon = Icons.Filled.Schedule,
        highlights = listOf(
            FriendlyHighlight(
                title = "Cannabis Cycle Presets",
                description = "Pre-configured routines for Seedling, Vegetative (18/6), Flowering (12/12), and Autoflower (20/4).",
                icon = Icons.Filled.Eco
            ),
            FriendlyHighlight(
                title = "Precise Local Alarms",
                description = "On-device notifications for watering schedules and photoperiod transitions without internet connection.",
                icon = Icons.Filled.NotificationsActive
            )
        ),
        architecturalSummary = "Synchronized with Android system clock for resilient background tracking."
    ),
    FriendlyRelease(
        version = "v0.8",
        status = "FOUNDATION",
        releaseTitle = "Core Foundation & Monet Theme",
        date = "October 2026",
        heroIcon = Icons.Filled.Code,
        highlights = listOf(
            FriendlyHighlight(
                title = "Floating Bottom Menu Scaffold",
                description = "Elevated floating navigation bar keeping screens uncluttered and ergonomic for one-handed operation.",
                icon = Icons.Filled.Eco
            ),
            FriendlyHighlight(
                title = "Dynamic Color & Dark Mode",
                description = "Full support for Dark Mode, Light Mode, and Android 12+ wallpaper dynamic color extraction.",
                icon = Icons.Filled.AutoAwesome
            )
        ),
        architecturalSummary = "Strictly zero gradients • High contrast flat surfaces • Clean Material 3 typography."
    )
)

@Composable
fun ChangelogScreen(
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("changelog_screen"),
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
                            text = "Release Notes",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "What's new and improved in PlantHub",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Friendly Release Cards
        items(AppReleases, key = { it.version }) { release ->
            ExpressiveReleaseCard(release = release)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpressiveReleaseCard(release: FriendlyRelease) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header Row: Version Pill + Status Pill + Date
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = release.heroIcon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Text(
                        text = release.version,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (release.status == "CURRENT") MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = release.status,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (release.status == "CURRENT") MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Text(
                    text = release.date,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = release.releaseTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Friendly Feature Rows
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                release.highlights.forEach { item ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(0.6.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Architecture Footer
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = release.architecturalSummary,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ChangelogDialog(
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Eco,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Release Log",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Version history & architectural upgrades",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(AppReleases, key = { it.version }) { release ->
                        ExpressiveReleaseCard(release = release)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onDismiss,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close Release Log", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
