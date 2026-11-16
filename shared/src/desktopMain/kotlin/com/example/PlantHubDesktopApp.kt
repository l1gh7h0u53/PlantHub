package com.example

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.example.data.local.AppDatabase
import com.example.data.local.getDesktopDatabase
import com.example.ui.screens.PlantHubSharedHost

/**
 * Compose Multiplatform JVM Desktop application entrypoint powered by Room KMP and BundledSQLiteDriver.
 */
fun main() = application {
    val database = AppDatabase.getDesktopDatabase()
    Window(
        onCloseRequest = ::exitApplication,
        title = "PlantHub Desktop",
        state = rememberWindowState(width = 960.dp, height = 760.dp)
    ) {
        PlantHubSharedHost(
            database = database,
            platformLabel = "Desktop"
        )
    }
}
