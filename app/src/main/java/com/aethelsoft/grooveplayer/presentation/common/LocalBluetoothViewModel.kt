package com.aethelsoft.grooveplayer.presentation.common

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aethelsoft.grooveplayer.presentation.player.BluetoothViewModel

/**
 * CompositionLocal for providing a single activity-scoped BluetoothViewModel
 * so all composables (player layouts, mini bar, bottom sheet, etc.) share the
 * same instance and see the same Bluetooth status.
 */
val LocalBluetoothViewModel = compositionLocalOf<BluetoothViewModel?> { null }

/**
 * Returns the shared BluetoothViewModel. Use this instead of hiltViewModel()
 * so you get the same instance provided by MainActivity (connected device, scanning, etc.).
 */
@Composable
fun rememberBluetoothViewModel(): BluetoothViewModel {
    LocalBluetoothViewModel.current?.let { return it }
    val activity = LocalActivity.current as ComponentActivity
    return viewModel(viewModelStoreOwner = activity)
}
