package com.example

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.example.data.local.AppDatabase
import com.example.data.local.getIosDatabase
import com.example.ui.screens.PlantHubSharedHost
import platform.UIKit.UIViewController

/**
 * Compose Multiplatform iOS UIViewController host exported via SharedKit framework.
 */
fun MainViewController(): UIViewController = ComposeUIViewController {
    val database = remember { AppDatabase.getIosDatabase() }
    PlantHubSharedHost(
        database = database,
        platformLabel = "iOS"
    )
}
