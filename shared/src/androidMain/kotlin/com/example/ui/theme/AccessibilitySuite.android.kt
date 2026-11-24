package com.example.ui.theme

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * Android Settings.Secure key names for Display Color Correction (Daltonizer) and High Contrast Text.
 *
 * Kept as explicit constants rather than `@hide` platform symbols so queries remain cleanly
 * supported across all Android API 24–36 builds without reflection or restricted APIs.
 */
private const val SECURE_DALTONIZER_ENABLED = "accessibility_display_daltonizer_enabled"
private const val SECURE_DALTONIZER_MODE = "accessibility_display_daltonizer"
private const val SECURE_HIGH_TEXT_CONTRAST_ENABLED = "high_text_contrast_enabled"

/**
 * Maps Android's `accessibility_display_daltonizer` integer codes to [ColorVisionMode].
 *
 * Official Android daltonizer constants:
 * - `0`: Monochromacy / Grayscale (`DISPLAY_DALTONIZER_MONOCHROMACY`)
 * - `11`: Protanomaly / Red-Green red-weak (`DISPLAY_DALTONIZER_PROTANOMALY`)
 * - `12`: Deuteranomaly / Red-Green green-weak (`DISPLAY_DALTONIZER_DEUTERANOMALY`)
 * - `13`: Tritanomaly / Blue-Yellow blue-weak (`DISPLAY_DALTONIZER_TRITANOMALY`)
 */
internal fun mapAndroidDaltonizerToVisionMode(
    enabled: Boolean,
    daltonizerCode: Int
): ColorVisionMode {
    if (!enabled) return ColorVisionMode.NONE
    return when (daltonizerCode) {
        0 -> ColorVisionMode.MONOCHROMACY
        1, 11 -> ColorVisionMode.PROTAN
        2, 12 -> ColorVisionMode.DEUTAN
        3, 13 -> ColorVisionMode.TRITAN
        else -> ColorVisionMode.DEUTAN
    }
}

/**
 * Reads the current Android OS Accessibility configuration synchronously from [Context].
 *
 * Safe in headless/Robolectric environments via exception guards when secure settings keys
 * are unpopulated in simulated providers.
 */
fun readAndroidAccessibilityState(context: Context): SystemAccessibilityState {
    val resolver = context.contentResolver
    val daltonizerEnabled = runCatching {
        Settings.Secure.getInt(resolver, SECURE_DALTONIZER_ENABLED, 0) == 1
    }.getOrDefault(false)
    val daltonizerCode = runCatching {
        Settings.Secure.getInt(resolver, SECURE_DALTONIZER_MODE, -1)
    }.getOrDefault(-1)
    val highContrastText = runCatching {
        Settings.Secure.getInt(resolver, SECURE_HIGH_TEXT_CONTRAST_ENABLED, 0) == 1
    }.getOrDefault(false)

    val am = runCatching {
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
    }.getOrNull()
    val screenReaderActive = am != null && (am.isTouchExplorationEnabled || am.isEnabled)

    return SystemAccessibilityState(
        colorVisionMode = mapAndroidDaltonizerToVisionMode(daltonizerEnabled, daltonizerCode),
        highContrastText = highContrastText,
        screenReaderActive = screenReaderActive
    )
}

/**
 * Live Android actual for [rememberSystemAccessibilityState].
 *
 * Attaches a [ContentObserver] onto `Settings.Secure` URIs for Color Correction and High Contrast
 * Text, alongside [AccessibilityManager] state listeners, so toggling accessibility features in
 * Android System Settings or Quick Settings immediately re-themes PlantHub without restart.
 */
@Composable
actual fun rememberSystemAccessibilityState(): SystemAccessibilityState {
    val context = LocalContext.current
    var state by remember(context) { mutableStateOf(readAndroidAccessibilityState(context)) }

    DisposableEffect(context) {
        val mainHandler = Handler(Looper.getMainLooper())
        val refresh = {
            state = readAndroidAccessibilityState(context)
        }
        val observer = object : ContentObserver(mainHandler) {
            override fun onChange(selfChange: Boolean) {
                refresh()
            }

            override fun onChange(selfChange: Boolean, uri: Uri?) {
                refresh()
            }
        }

        val resolver = context.contentResolver
        runCatching {
            Settings.Secure.getUriFor(SECURE_DALTONIZER_ENABLED)?.let {
                resolver.registerContentObserver(it, false, observer)
            }
            Settings.Secure.getUriFor(SECURE_DALTONIZER_MODE)?.let {
                resolver.registerContentObserver(it, false, observer)
            }
            Settings.Secure.getUriFor(SECURE_HIGH_TEXT_CONTRAST_ENABLED)?.let {
                resolver.registerContentObserver(it, false, observer)
            }
        }

        val am = runCatching {
            context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        }.getOrNull()
        val a11yListener = AccessibilityManager.AccessibilityStateChangeListener { refresh() }
        val touchListener = AccessibilityManager.TouchExplorationStateChangeListener { refresh() }
        am?.addAccessibilityStateChangeListener(a11yListener)
        am?.addTouchExplorationStateChangeListener(touchListener)

        onDispose {
            runCatching { resolver.unregisterContentObserver(observer) }
            am?.removeAccessibilityStateChangeListener(a11yListener)
            am?.removeTouchExplorationStateChangeListener(touchListener)
        }
    }

    return state
}
