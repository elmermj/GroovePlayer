package com.aethelsoft.grooveplayer.presentation.common

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aethelsoft.grooveplayer.presentation.player.PlayerViewModel

/**
 * CompositionLocal for providing PlayerViewModel throughout the app.
 * This allows accessing the activity-scoped PlayerViewModel from any composable
 * without passing it through parameters. This is a special case due to application
 * business requirements which requires the music player to be summoned anywhere
 * in the app.
 * 
 * This follows Android best practices and does NOT violate Clean Architecture or MVVM
 * because:
 * 1. ViewModel remains lifecycle-aware (scoped to Activity)
 * 2. It's still in the presentation layer
 * 3. Single source of truth is maintained
 * 4. Standard pattern for shared state across screens
 *
 * Authored by Elmer Matthew Japara (me)
 */
val LocalPlayerViewModel = compositionLocalOf<PlayerViewModel?> { null }

/**
 * Helper function to get the activity-scoped PlayerViewModel from anywhere
 * in your composable hierarchy.
 * 
 * Usage in any Composable:
 * ```kotlin
 * @Composable
 * fun AnyScreen() {
 *     val playerViewModel = rememberPlayerViewModel()
 *     val currentSong by playerViewModel.currentSong.collectAsState()
 *     // Use playerViewModel...
 * }
 * ```
 */
@Composable
fun rememberPlayerViewModel(): PlayerViewModel {
    // Try to get from CompositionLocal first
    LocalPlayerViewModel.current?.let { return it }
    
    // Otherwise, get activity-scoped ViewModel
    val activity = LocalActivity.current as ComponentActivity
    return viewModel(viewModelStoreOwner = activity)
}
