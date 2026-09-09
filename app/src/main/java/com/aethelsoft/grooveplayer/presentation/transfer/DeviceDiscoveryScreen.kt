package com.aethelsoft.grooveplayer.presentation.transfer

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.aethelsoft.grooveplayer.data.transfer.NearbyTransferManager
import com.aethelsoft.grooveplayer.presentation.common.GrooveActionButton
import com.aethelsoft.grooveplayer.presentation.common.GrooveCardSubtitle
import com.aethelsoft.grooveplayer.presentation.common.GrooveCardTitle
import com.aethelsoft.grooveplayer.presentation.common.GrooveMutedText
import com.aethelsoft.grooveplayer.presentation.common.GrooveScreen
import com.aethelsoft.grooveplayer.presentation.common.GrooveSurfaceCard
import com.aethelsoft.grooveplayer.presentation.common.GrooveTinySpacer
import com.aethelsoft.grooveplayer.utils.M_PADDING
import com.aethelsoft.grooveplayer.utils.S_PADDING
import com.aethelsoft.grooveplayer.utils.helpers.logShareNearbyP2PTag
import com.aethelsoft.grooveplayer.utils.theme.ui.GrooveTheme
import com.aethelsoft.grooveplayer.utils.theme.ui.SoftWhite

@Composable
fun DeviceDiscoveryScreen(
    isSender: Boolean,
    onNavigateBack: () -> Unit,
    onDeviceSelected: (String, String) -> Unit,
    onNavigateToTransferProgress: () -> Unit,
    viewModel: DiscoveryViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val tag = remember { logShareNearbyP2PTag(context) }
    val connectionState by viewModel.connectionState.collectAsState()
    val deviceCapability by viewModel.deviceCapability.collectAsState()
    val showBatteryPrompt by viewModel.showBatteryOptimizationPrompt.collectAsState()

    var hasPermissions by remember { mutableStateOf(viewModel.hasAllPermissions) }
    var hasRequestedPermissions by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasRequestedPermissions = true
        hasPermissions = results.values.all { it }
        if (hasPermissions) {
            Log.d(tag, "Permissions granted. Proceeding with nearby share.")
        } else {
            val denied = results.filter { !it.value }.keys
            Log.w(tag, "Permissions denied: $denied. Cannot discover/advertise nearby devices.")
        }
    }

    LaunchedEffect(Unit) {
        if (!viewModel.hasAllPermissions && !hasRequestedPermissions) {
            val missing = viewModel.getMissingPermissions()
            Log.d(tag, "Insufficient permissions. Prompting user for: ${missing.contentToString()}")
            permissionLauncher.launch(missing)
            hasRequestedPermissions = true
        } else {
            hasPermissions = viewModel.hasAllPermissions
            if (hasPermissions) Log.d(tag, "All permissions granted. Ready for nearby share.")
        }
    }

    LaunchedEffect(connectionState) {
        when (connectionState) {
            is NearbyTransferManager.ConnectionState.Connected ->
                Log.d(tag, "Connection established. Navigating to transfer progress.")
            is NearbyTransferManager.ConnectionState.Error ->
                Log.e(tag, "Connection error: ${(connectionState as NearbyTransferManager.ConnectionState.Error).message}")
            else -> {}
        }
        if (connectionState is NearbyTransferManager.ConnectionState.Connected) {
            onNavigateToTransferProgress()
        }
    }

    var filePaths by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(isSender) {
        if (isSender) {
            filePaths = viewModel.loadFilePathsFromShareIntent()
        }
    }

    if (showBatteryPrompt) {
        BatteryOptimizationDialog(
            onDismiss = { viewModel.dismissBatteryPrompt() },
            onOpenSettings = { viewModel.openBatterySettings() },
        )
    }

    GrooveScreen(
        title = if (isSender) "Share with nearby" else "Receive from nearby",
        onBackClick = onNavigateBack,
    ) {
        DeviceCapabilityCard(capability = deviceCapability)

        Spacer(modifier = Modifier.height(M_PADDING))

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                !hasPermissions -> PermissionRequiredView(
                    onRequestPermissions = {
                        Log.d(tag, "User requested permissions")
                        permissionLauncher.launch(viewModel.getMissingPermissions())
                    },
                )
                isSender && filePaths.isEmpty() -> SenderEmptyStateView(
                    onNavigateBack = onNavigateBack,
                )
                isSender && filePaths.isNotEmpty() -> SenderView(
                    viewModel = viewModel,
                    filePaths = filePaths,
                    onConnectionAccepted = onDeviceSelected,
                )
                else -> ReceiverView(
                    tag = tag,
                    viewModel = viewModel,
                    onDeviceSelected = onDeviceSelected,
                )
            }
        }
    }
}

@Composable
private fun SenderEmptyStateView(
    onNavigateBack: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "No files selected",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
            )
            Spacer(modifier = Modifier.height(S_PADDING))
            GrooveMutedText("Go back and select files to share.")
            Spacer(modifier = Modifier.height(M_PADDING))
            GrooveActionButton(
                label = "Go back",
                onClick = onNavigateBack,
                isPrimary = true,
            )
        }
    }
}

@Composable
private fun SenderView(
    viewModel: DiscoveryViewModel,
    filePaths: List<String>,
    onConnectionAccepted: (String, String) -> Unit,
) {
    LaunchedEffect(filePaths) {
        viewModel.startAdvertising(filePaths) { endpointId, endpointName ->
            onConnectionAccepted(endpointId, endpointName)
        }
    }
    WaitingState(
        title = "Waiting for receiver…",
        subtitle = "Works without Wi‑Fi. Use Bluetooth or Wi‑Fi Direct.",
    )
}

@Composable
private fun ReceiverView(
    tag: String,
    viewModel: DiscoveryViewModel,
    onDeviceSelected: (String, String) -> Unit,
) {
    val discoveredEndpoints by viewModel.discoveredEndpoints.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.startDiscovery()
    }

    LaunchedEffect(discoveredEndpoints.size) {
        if (discoveredEndpoints.isNotEmpty()) {
            Log.d(tag, "Found ${discoveredEndpoints.size} device(s) nearby: ${discoveredEndpoints.map { it.name }}")
        }
    }

    if (discoveredEndpoints.isEmpty()) {
        WaitingState(
            title = "Searching for nearby devices…",
            subtitle = "Keep both phones unlocked and nearby.",
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(S_PADDING),
        ) {
            items(discoveredEndpoints) { endpoint ->
                GrooveSurfaceCard(
                    onClick = {
                        viewModel.requestConnection(endpoint.endpointId, endpoint.name)
                        onDeviceSelected(endpoint.endpointId, endpoint.name)
                    }
                ) {
                    GrooveCardTitle(endpoint.name)
                    GrooveTinySpacer()
                    GrooveCardSubtitle("Tap to connect")
                }
            }
        }
    }
}

@Composable
private fun WaitingState(
    title: String,
    subtitle: String,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = SoftWhite)
            Spacer(modifier = Modifier.height(M_PADDING))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
            )
            Spacer(modifier = Modifier.height(S_PADDING))
            GrooveMutedText(subtitle)
        }
    }
}

@Composable
private fun PermissionRequiredView(
    onRequestPermissions: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        GrooveSurfaceCard {
            GrooveCardTitle("Permissions required")
            GrooveTinySpacer()
            GrooveCardSubtitle(
                "Nearby sharing needs Bluetooth and location (or nearby devices) access to discover and connect."
            )
            Spacer(modifier = Modifier.height(M_PADDING))
            GrooveActionButton(
                label = "Grant permissions",
                onClick = onRequestPermissions,
                isPrimary = true,
            )
        }
    }
}

@Composable
private fun BatteryOptimizationDialog(
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GrooveTheme.colors.surface,
        titleContentColor = GrooveTheme.colors.onSurface,
        textContentColor = SoftWhite.copy(alpha = 0.8f),
        title = { Text("Battery optimization") },
        text = {
            Text(
                "For reliable transfers in the background, consider disabling battery optimization for this app."
            )
        },
        confirmButton = {
            TextButton(onClick = onOpenSettings) {
                Text("Open settings", color = SoftWhite)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Later", color = SoftWhite.copy(alpha = 0.65f))
            }
        },
    )
}
