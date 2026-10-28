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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.createNotificationChannel(this)
        NotificationHelper.createLiveUpdateChannel(this)
        NotificationHelper.scheduleDailyCareAlerts(this)

        val openGauge = intent?.getBooleanExtra("extra_open_light_gauge", false) == true

        setContent {
            val viewModel: PlantViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val dynamicColor by viewModel.dynamicColor.collectAsStateWithLifecycle()

            LaunchedEffect(openGauge) {
                if (openGauge) {
                    viewModel.selectTab(NavTab.DETAIL)
                }
            }

            val isDark = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
            }

            PlantHubTheme(
                darkTheme = isDark,
                dynamicColor = dynamicColor
            ) {
                PlantHubApp(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

@Composable
fun PlantHubApp(viewModel: PlantViewModel) {
    val hasSeenOnboarding by viewModel.hasSeenOnboarding.collectAsStateWithLifecycle()

    if (!hasSeenOnboarding) {
        OnboardingScreen(
            onComplete = { openAddPlant ->
                viewModel.setOnboardingCompleted(true)
                if (openAddPlant) {
                    viewModel.triggerAddPlantDialog()
                }
            }
        )
        return
    }

    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val plants by viewModel.allPlants.collectAsStateWithLifecycle()
    val selectedPlant by viewModel.selectedPlant.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Permission handled */ }

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

    // Handle Android system Back button: return to Home tab if on any sub-screen
    BackHandler(enabled = currentTab != NavTab.HOME) {
        viewModel.selectTab(NavTab.HOME)
    }

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
