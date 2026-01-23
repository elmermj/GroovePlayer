package com.aethelsoft.grooveplayer.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

/**
 * Navigation actions that can be accessed from anywhere in the app
 * without passing callbacks as parameters.
 */
data class NavigationActions(
    val openFullPlayer: () -> Unit = {},
    val closeFullPlayer: () -> Unit = {}
)

/**
 * CompositionLocal for providing navigation actions throughout the app.
 * This allows navigating without passing callbacks through parameters.
 */
val LocalNavigation = compositionLocalOf { NavigationActions() }

/**
 * Helper function to get navigation actions from anywhere.
 */
@Composable
fun rememberNavigationActions(): NavigationActions {
    return LocalNavigation.current
}
