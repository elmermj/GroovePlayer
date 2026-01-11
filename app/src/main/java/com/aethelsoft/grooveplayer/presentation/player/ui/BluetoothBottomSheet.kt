package com.aethelsoft.grooveplayer.presentation.player.ui

import XCheckCircle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aethelsoft.grooveplayer.domain.model.BluetoothDevice
import com.aethelsoft.grooveplayer.presentation.player.BluetoothViewModel
import com.aethelsoft.grooveplayer.utils.rememberBluetoothPermissionState
import com.aethelsoft.grooveplayer.utils.theme.icons.XClose

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothBottomSheet(
    onDismiss: () -> Unit,
    viewModel: BluetoothViewModel = hiltViewModel()
) {
    val (hasPermissions, requestPermissions) = rememberBluetoothPermissionState()
    val availableDevices by viewModel.availableDevices.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val connectedDevice by viewModel.connectedDevice.collectAsState()

    val isBluetoothEnabled = viewModel.isBluetoothEnabled()
    val isBluetoothSupported = viewModel.isBluetoothSupported()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .safeContentPadding()
            .fillMaxHeight(1f) // max height, not forced height
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {

            /* ---------- HEADER ---------- */
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Bluetooth Devices",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(XClose, contentDescription = "Close")
                }
            }

            Spacer(Modifier.height(16.dp))

            /* ---------- STATUS ---------- */
            when {
                !isBluetoothSupported -> {
                    Text(
                        "Bluetooth is not supported on this device",
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(16.dp))
                }

                !isBluetoothEnabled -> {
                    Text(
                        "Please enable Bluetooth in settings",
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(16.dp))
                }

                !hasPermissions -> {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Bluetooth permissions are required")
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = requestPermissions,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Grant Permissions")
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }

            /* ---------- SCAN BUTTON ---------- */
            if (isBluetoothSupported && isBluetoothEnabled && hasPermissions) {
                Button(
                    onClick = {
                        if (isScanning) viewModel.stopScanning()
                        else viewModel.startScanning()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Scanning…")
                    } else {
                        Text("Scan for Devices")
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            /* ---------- CONNECTED DEVICE ---------- */
            connectedDevice?.let { device ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Connected", style = MaterialTheme.typography.labelSmall)
                            Text(device.name, fontWeight = FontWeight.Bold)
                            Text(
                                device.address,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.7f)
                            )
                        }
                        TextButton(onClick = viewModel::disconnectDevice) {
                            Text("Disconnect")
                        }
                    }
                }
            }

            /* ---------- DEVICE LIST ---------- */
            if (availableDevices.isEmpty() && !isScanning) {
                Text(
                    "No devices found. Tap 'Scan for Devices' to search.",
                    modifier = Modifier.padding(vertical = 24.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false), // ⭐ CRITICAL LINE
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    overscrollEffect = rememberOverscrollEffect()
                ) {
                    items(
                        items = availableDevices,
                        key = { device -> device.address }
                    ) { device ->
                        BluetoothDeviceItem(
                            device = device,
                            isConnected = connectedDevice?.address == device.address,
                            onClick = {
                                if (connectedDevice?.address == device.address) {
                                    viewModel.disconnectDevice()
                                } else {
                                    viewModel.connectToDevice(device)
                                }
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

/* ---------- LIST ITEM ---------- */

@Composable
fun BluetoothDeviceItem(
    device: BluetoothDevice,
    isConnected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    device.name,
                    fontWeight = if (isConnected) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    device.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                )
            }
            if (isConnected) {
                Icon(
                    XCheckCircle,
                    contentDescription = "Connected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
