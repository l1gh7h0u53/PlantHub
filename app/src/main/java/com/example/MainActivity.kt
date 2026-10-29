package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.FloatingBottomBar
import com.example.ui.screens.ChangelogScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PlantDetailScreen
import com.example.ui.screens.PhotoVaultScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.PlantHubTheme
import com.example.ui.viewmodel.NavTab
import com.example.ui.viewmodel.PlantViewModel
import com.example.ui.viewmodel.ThemeMode
import com.example.util.NotificationHelper

import android.content.Intent

/**
 * MainActivity serves as the single-activity entry point for the PlantHub application.
 *
 * Responsibilities:
 * - Configures edge-to-edge window insets for modern edge-to-edge Android displays.
 * - Initializes system notification channels for care reminders and ongoing photoperiod live status chips.
 * - Schedules daily care alarm broadcasts via the Android AlarmManager.
 * - Handles deep-linking intents (e.g. tapping a notification status chip to navigate to the live light gauge).
 * - Observes user-selected theme settings (Light, Dark, System default, and Android 12+ Monet dynamic theming).
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Enable edge-to-edge drawing so content gracefully flows behind status and navigation bars
        enableEdgeToEdge()

        // Initialize Android notification channels
        NotificationHelper.createNotificationChannel(this)
        NotificationHelper.createLiveUpdateChannel(this)

        // Schedule exact daily care alerts (watering, nutrients, environmental inspection)
        NotificationHelper.scheduleDailyCareAlerts(this)

        // Check if app was launched via deep-link intent from the photoperiod live status chip
        val openGauge = intent?.getBooleanExtra("extra_open_light_gauge", false) == true

        setContent {
            // Instantiate the root PlantViewModel managing state across all tabs
            val viewModel: PlantViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val dynamicColor by viewModel.dynamicColor.collectAsStateWithLifecycle()

            // Navigate directly to the detailed Tracker screen if launched from the light status notification
            LaunchedEffect(openGauge) {
                if (openGauge) {
                    viewModel.selectTab(NavTab.DETAIL)
                }
            }

            // Determine effective dark/light theme state based on user preference and system configuration
            val isDark = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
            }

            // Apply centralized M3 theme with sanitized high-contrast Monet dynamic color support
            PlantHubTheme(
                darkTheme = isDark,
                dynamicColor = dynamicColor
            ) {
                PlantHubApp(viewModel = viewModel)
            }
        }
    }

    /**
     * Handles incoming intents when the activity is already running in the background (single-top).
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

/**
 * Primary UI coordinator for PlantHub.
 *
 * Responsibilities:
 * - Checks onboarding completion status; renders OnboardingScreen carousel if first launch.
 * - Manages runtime notification permission requests on Android 13+ (Tiramisu).
 * - Synchronizes photoperiod ticker state when the application resumes from background.
 * - Handles Android system back gestures, routing sub-screens back to the Home tab.
 * - Displays the Scaffold with floating bottom navigation and dynamically swaps between screens.
 */
@Composable
fun PlantHubApp(viewModel: PlantViewModel) {
    // Check if the user has completed or skipped the first-launch tutorial carousel
    val hasSeenOnboarding by viewModel.hasSeenOnboarding.collectAsStateWithLifecycle()

    if (!hasSeenOnboarding) {
        // Display full-screen interactive onboarding carousel
        OnboardingScreen(
            onComplete = { openAddPlant ->
                viewModel.setOnboardingCompleted(true)
                if (openAddPlant) {
                    // Seamlessly launch the Add Plant dialog if user tapped "Add First Plant"
                    viewModel.triggerAddPlantDialog()
                }
            }
        )
        return
    }

    // Active bottom navigation tab selection
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    // Observable list of all cultivars currently persisted in Room SQLite
    val plants by viewModel.allPlants.collectAsStateWithLifecycle()
    // Currently highlighted or active cultivar
    val selectedPlant by viewModel.selectedPlant.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Permission response handled by system */ }

    // Lifecycle observer to recalculate exact photoperiod timer state upon returning to foreground
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.refreshTimerState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Request POST_NOTIFICATIONS permission for Android 13+ devices
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Android system Back button handler: return to Home tab if on any sub-screen (Detail, Vault, Settings)
    BackHandler(enabled = currentTab != NavTab.HOME) {
        viewModel.selectTab(NavTab.HOME)
    }

    // Core application structure with floating bottom navigation bar
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.statusBars,
        bottomBar = {
            FloatingBottomBar(
                currentTab = currentTab,
                onTabSelected = { tab -> viewModel.selectTab(tab) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            // Screen router driven by the active NavTab state
            when (currentTab) {
                NavTab.HOME -> {
                    HomeScreen(
                        viewModel = viewModel,
                        plants = plants,
                        onSelectPlant = { plant ->
                            viewModel.selectPlant(plant.id)
                            viewModel.selectTab(NavTab.DETAIL)
                        }
                    )
                }
                NavTab.DETAIL -> {
                    PlantDetailScreen(
                        viewModel = viewModel,
                        plants = plants,
                        selectedPlant = selectedPlant
                    )
                }
                NavTab.VAULT -> {
                    PhotoVaultScreen(
                        viewModel = viewModel,
                        plants = plants,
                        selectedPlant = selectedPlant
                    )
                }
                NavTab.SETTINGS -> {
                    SettingsScreen(viewModel = viewModel)
                }
            }
        }
    }
}
