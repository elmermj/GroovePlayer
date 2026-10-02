package com.aethelworks.grooveplayer.presentation.transfer

import XCheckCircle
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aethelworks.grooveplayer.R
import com.aethelworks.grooveplayer.presentation.common.FlatChevron
import com.aethelworks.grooveplayer.presentation.common.FlatRow
import com.aethelworks.grooveplayer.presentation.common.HairlineDivider
import com.aethelworks.grooveplayer.presentation.common.flatTitleStyle
import com.aethelworks.grooveplayer.utils.helpers.NearbyDeviceCapability
import com.aethelworks.grooveplayer.utils.theme.ui.PoppinsFontFamily
import com.aethelworks.grooveplayer.utils.theme.ui.SoftWhite

/**
 * One compact capability row. Collapsed when the device is ready, expanded when
 * something is missing. Status is in the summary text as well as the dot.
 *
 * @param visibleWhenReady Share & Receive always shows the row. Share with nearby
 * shows it only while something is missing.
 */
@Composable
fun DeviceCapabilityRow(
    capability: NearbyDeviceCapability,
    modifier: Modifier = Modifier,
    visibleWhenReady: Boolean = true,
) {
    val missing = capability.hasMissingCapability
    if (!visibleWhenReady && !missing) return

    var expanded by remember(missing) { mutableStateOf(missing) }

    Column(modifier = modifier.fillMaxWidth()) {
        FlatRow(
            title = capability.summary,
            leading = {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            if (!missing) SoftWhite else SoftWhite.copy(alpha = 0.40f),
                            CircleShape,
                        ),
                )
            },
            trailing = { FlatChevron(expanded = expanded) },
            onClick = { expanded = !expanded },
            showDivider = !expanded,
        )
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                CapabilityLine("Wi‑Fi Direct", capability.wifiDirectSupported)
                CapabilityLine("Wi‑Fi P2P service", capability.wifiP2pAvailable)
                CapabilityLine("Bluetooth", capability.bluetoothSupported)
                CapabilityLine("Bluetooth enabled", capability.bluetoothEnabled)
                CapabilityLine(
                    "Google Play Services (Nearby)",
                    capability.nearbyConnectionsAvailable,
                )
            }
        }
        if (expanded) {
            HairlineDivider()
        }
    }
}

@Composable
private fun CapabilityLine(
    label: String,
    supported: Boolean,
) {
    val status = stringResource(
        if (supported) R.string.cd_capability_ready else R.string.cd_capability_missing,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(horizontal = 20.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "$label, $status"
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (supported) {
            Icon(
                imageVector = XCheckCircle,
                contentDescription = null,
                tint = SoftWhite,
                modifier = Modifier.size(22.dp),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .border(1.dp, SoftWhite.copy(alpha = 0.80f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "!",
                    color = SoftWhite,
                    fontFamily = PoppinsFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = flatTitleStyle(),
            modifier = Modifier.clearAndSetSemantics {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 390, name = "Capability ready")
@Composable
private fun DeviceCapabilityReadyPreview() {
    Column(Modifier.background(Color.Black)) {
        DeviceCapabilityRow(capability = previewCapability(ready = true))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 390, name = "Capability missing")
@Composable
private fun DeviceCapabilityMissingPreview() {
    Column(Modifier.background(Color.Black)) {
        DeviceCapabilityRow(
            capability = previewCapability(ready = false),
            visibleWhenReady = false,
        )
    }
}

/** True when any checklist item is unavailable. */
val NearbyDeviceCapability.hasMissingCapability: Boolean
    get() = !wifiDirectSupported ||
        !wifiP2pAvailable ||
        !bluetoothSupported ||
        !bluetoothEnabled ||
        !nearbyConnectionsAvailable

private fun previewCapability(ready: Boolean) = NearbyDeviceCapability(
    wifiDirectSupported = true,
    wifiP2pAvailable = ready,
    bluetoothSupported = true,
    bluetoothEnabled = ready,
    nearbyConnectionsAvailable = ready,
)
