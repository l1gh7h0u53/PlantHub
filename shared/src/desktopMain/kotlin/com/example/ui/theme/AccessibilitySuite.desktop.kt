package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.awt.Toolkit

/**
 * Desktop JVM actual for [rememberSystemAccessibilityState].
 *
 * Checks JVM high-contrast desktop hints where supported and defaults to standard color vision
 * mode cleanly across Linux, macOS, and Windows Compose Desktop windows.
 */
@Composable
actual fun rememberSystemAccessibilityState(): SystemAccessibilityState {
    return remember {
        val highContrast = runCatching {
            val raw = Toolkit.getDefaultToolkit().getDesktopProperty("win.highContrast.on")
            raw == true
        }.getOrDefault(false)
        SystemAccessibilityState(highContrastText = highContrast)
    }
}
