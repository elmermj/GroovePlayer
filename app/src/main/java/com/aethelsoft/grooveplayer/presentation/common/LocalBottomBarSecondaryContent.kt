package com.aethelsoft.grooveplayer.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf

/**
 * CompositionLocal for optional secondary content in the bottom bar area.
 * When set, the content is displayed alongside MiniPlayerBar with a sliding transition,
 * pushing the mini player to the left.
 *
 * Screens (e.g. SongsScreen in selection mode, ShareConfirmationScreen) should set
 * [content] when they need to show confirmation/option UI in the bottom bar space.
 */
val LocalBottomBarSecondaryContent = compositionLocalOf<MutableState<(@Composable () -> Unit)?>> {
    error("No BottomBarSecondaryContentState provided")
}
