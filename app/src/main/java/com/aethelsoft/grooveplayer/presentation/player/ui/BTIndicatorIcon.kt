package com.aethelsoft.grooveplayer.presentation.player.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.aethelsoft.grooveplayer.utils.helpers.BluetoothHelpers

@Composable
fun BTIndicatorIcon(
    modifier: Modifier = Modifier,
    connectedDeviceName: String?,
    isConnected: Boolean,
    tint: Color = if (isConnected) Color.Black else Color.White,
) {
    Box {
        if (isConnected) {
            // Strong glow layers
            repeat(5) { i ->
                Icon(
                    imageVector = BluetoothHelpers.connectedBtIconFromName(connectedDeviceName),
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 1f),
                    modifier = modifier
                        .scale(1f)
                        .blur((1).dp)
                )
            }
        }

        // Main icon
        Icon(
            imageVector = BluetoothHelpers.connectedBtIconFromName(connectedDeviceName),
            contentDescription = "More",
            tint = tint,
            modifier = modifier
        )
    }
}