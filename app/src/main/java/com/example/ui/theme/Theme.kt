package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PlantGreenPrimaryDark,
    onPrimary = PlantGreenOnPrimaryDark,
    primaryContainer = PlantGreenContainerDark,
    onPrimaryContainer = PlantGreenOnContainerDark,
    secondary = PlantSageSecondaryDark,
    onSecondary = PlantSageOnSecondaryDark,
    secondaryContainer = PlantSageContainerDark,
    onSecondaryContainer = PlantSageOnContainerDark,
    tertiary = PlantAmberDark,
    onTertiary = PlantAmberOnDark,
    tertiaryContainer = PlantAmberContainerDark,
    onTertiaryContainer = PlantAmberOnContainerDark,
    background = PlantDarkBackground,
    onBackground = androidx.compose.ui.graphics.Color(0xFFF0F6FC),
    surface = PlantDarkSurface,
    onSurface = androidx.compose.ui.graphics.Color(0xFFF0F6FC),
    surfaceVariant = PlantDarkSurfaceVariant,
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFD0D7DE),
    outline = PlantDarkOutline,
    outlineVariant = PlantDarkOutlineVariant
)

private val LightColorScheme = lightColorScheme(
    primary = PlantGreenPrimaryLight,
    onPrimary = PlantGreenOnPrimaryLight,
    primaryContainer = PlantGreenContainerLight,
    onPrimaryContainer = PlantGreenOnContainerLight,
    secondary = PlantSageSecondaryLight,
    onSecondary = PlantSageOnSecondaryLight,
    secondaryContainer = PlantSageContainerLight,
    onSecondaryContainer = PlantSageOnContainerLight,
    tertiary = PlantAmberLight,
    onTertiary = PlantAmberOnLight,
    tertiaryContainer = PlantAmberContainerLight,
    onTertiaryContainer = PlantAmberOnContainerLight,
    background = PlantLightBackground,
    onBackground = androidx.compose.ui.graphics.Color(0xFF1F2328),
    surface = PlantLightSurface,
    onSurface = androidx.compose.ui.graphics.Color(0xFF1F2328),
    surfaceVariant = PlantLightSurfaceVariant,
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF656D76),
    outline = PlantLightOutline,
    outlineVariant = PlantLightOutlineVariant
)

@Composable
fun PlantHubTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) {
                val dynamicDark = dynamicDarkColorScheme(context)
                dynamicDark.copy(
                    onSurface = androidx.compose.ui.graphics.Color(0xFFF0F6FC),
                    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFD0D7DE),
                    onBackground = androidx.compose.ui.graphics.Color(0xFFF0F6FC),
                    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFFE8F5E9),
                    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFFE6F4EA),
                    onTertiaryContainer = androidx.compose.ui.graphics.Color(0xFFFFF8E1)
                )
            } else {
                dynamicLightColorScheme(context)
            }
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
