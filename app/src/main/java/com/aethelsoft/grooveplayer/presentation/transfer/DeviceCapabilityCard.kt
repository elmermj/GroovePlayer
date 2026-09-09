package com.aethelsoft.grooveplayer.presentation.transfer

import XCheckCircle
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aethelsoft.grooveplayer.presentation.common.GrooveCardSubtitle
import com.aethelsoft.grooveplayer.presentation.common.GrooveCardTitle
import com.aethelsoft.grooveplayer.presentation.common.GrooveSurfaceCard
import com.aethelsoft.grooveplayer.utils.XS_PADDING
import com.aethelsoft.grooveplayer.utils.helpers.NearbyDeviceCapability
import com.aethelsoft.grooveplayer.utils.theme.icons.XClose
import com.aethelsoft.grooveplayer.utils.theme.ui.SoftWhite

@Composable
fun DeviceCapabilityCard(
    capability: NearbyDeviceCapability,
    modifier: Modifier = Modifier,
) {
    GrooveSurfaceCard(modifier = modifier) {
        GrooveCardTitle("Device capability")
        Spacer(modifier = Modifier.height(XS_PADDING / 2))
        GrooveCardSubtitle(capability.summary)
        Spacer(modifier = Modifier.height(XS_PADDING))

        CapabilityRow(label = "Wi‑Fi Direct", supported = capability.wifiDirectSupported)
        CapabilityRow(label = "Wi‑Fi P2P service", supported = capability.wifiP2pAvailable)
        CapabilityRow(label = "Bluetooth", supported = capability.bluetoothSupported)
        CapabilityRow(label = "Bluetooth enabled", supported = capability.bluetoothEnabled)
        CapabilityRow(
            label = "Google Play Services (Nearby)",
            supported = capability.nearbyConnectionsAvailable,
        )
    }
}

@Composable
private fun CapabilityRow(
    label: String,
    supported: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (supported) XCheckCircle else XClose,
            contentDescription = null,
            tint = if (supported) SoftWhite else SoftWhite.copy(alpha = 0.35f),
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(XS_PADDING))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (supported) Color.White else SoftWhite.copy(alpha = 0.45f),
        )
    }
}
