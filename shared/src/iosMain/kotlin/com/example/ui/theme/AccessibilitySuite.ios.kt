package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * iOS actual for [rememberSystemAccessibilityState].
 *
 * Supplies default accessibility state cleanly inside KMP SharedKit so iOS builds remain
 * decoupled while sharing the full semantic token layer.
 */
@Composable
actual fun rememberSystemAccessibilityState(): SystemAccessibilityState {
    return remember { SystemAccessibilityState() }
}
