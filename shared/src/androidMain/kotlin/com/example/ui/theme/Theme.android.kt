package com.example.ui.theme

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun platformDynamicColorScheme(darkTheme: Boolean): ColorScheme? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    val context = LocalContext.current
    return if (darkTheme) {
        dynamicDarkColorScheme(context).copy(
            onSurface = Color(0xFFF0F6FC),
            onSurfaceVariant = Color(0xFFD0D7DE),
            onBackground = Color(0xFFF0F6FC),
            onPrimaryContainer = Color(0xFFE8F5E9),
            onSecondaryContainer = Color(0xFFE6F4EA),
            onTertiaryContainer = Color(0xFFFFF8E1)
        )
    } else {
        dynamicLightColorScheme(context)
    }
}
