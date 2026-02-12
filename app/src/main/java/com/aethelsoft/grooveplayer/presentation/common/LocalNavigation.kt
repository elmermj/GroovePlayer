package com.aethelsoft.grooveplayer.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import com.aethelsoft.grooveplayer.domain.model.Song

/**
 * Navigation actions that can be accessed from anywhere in the app
 * without passing callbacks as parameters.
 */
data class NavigationActions(
    val currentRoute: String? = null,
    val goBack: () -> Unit = {},
    val openFullPlayer: () -> Unit = {},
    val closeFullPlayer: () -> Unit = {},
    val openProfile: () -> Unit = {},
    val openShare: () -> Unit = {},
    /** Prepares songs and navigates to Share options screen (user picks NFC or Nearby). */
    val openShareWithSongs: (List<Song>) -> Unit = {},
    /** Prepares songs and navigates to Share via NFC (Tap). */
    val openShareViaNfcWithSongs: (List<Song>) -> Unit = {},
    /** Prepares songs and navigates to Share with nearby device. */
    val openShareViaNearbyWithSongs: (List<Song>) -> Unit = {},
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
