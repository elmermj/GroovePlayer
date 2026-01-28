package com.aethelsoft.grooveplayer.presentation.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aethelsoft.grooveplayer.domain.model.BluetoothDevice
import com.aethelsoft.grooveplayer.utils.helpers.BluetoothHelpers
import com.aethelsoft.grooveplayer.utils.theme.icons.*
import com.aethelsoft.grooveplayer.utils.theme.ui.RunningText
import kotlin.math.cos
import kotlin.math.sin

private const val DEGREE_STEP = 15f
private const val START_ANGLE = 270f

// BluetoothDeviceType moved to BluetoothHelpers (DRY)

@Composable
fun BluetoothRadialComponent(
    availableDevices: List<BluetoothDevice>,
    connectedDevice: BluetoothDevice?,
    isScanning: Boolean,
    onDeviceClick: (BluetoothDevice) -> Unit,
    maxHeight: Dp,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    val radiusDp = maxHeight * 0.42f
    val radiusPx = with(density) { radiusDp.toPx() }

    // Ellipse shape: squashed horizontally
    val p = 0.55f   // horizontal radius multiplier
    val q = 1.0f    // vertical radius multiplier

    // Center pushed to the RIGHT so only LEFT half is visible
    val centerXPx = with(density) { radiusDp.toPx() }
    val centerYPx = with(density) { (maxHeight * 0.5f).toPx() }

    // SCROLL / REVOLVER

    var scrollIndex by remember { mutableStateOf(0) }
    val maxIndex = maxOf(0, availableDevices.size - 1)

    LaunchedEffect(scrollIndex, availableDevices.size) {
        scrollIndex = scrollIndex.coerceIn(0, maxIndex)
    }

    val scrollState = rememberScrollableState { delta ->
        scrollIndex += if (delta > 0f) 1 else -1
        delta
    }

    // ROOT

    Box(
        modifier = modifier
            .fillMaxSize()
            .scrollable(
                state = scrollState,
                orientation = Orientation.Vertical,
                enabled = availableDevices.size > 1
            )
            .drawBehind {
                drawLeftHalfEllipseGradient(
                    center = Offset(centerXPx, centerYPx),
                    radius = radiusPx,
                    p = p,
                    q = q
                )
            }
    ) {

        // STATUS (LEFT HALF ONLY

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = (-radiusDp * 0.35f)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when {
                isScanning -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("Scanning", color = Color.White)
                }

                connectedDevice != null -> {
                    Icon(
                        XBluetoothConnected,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("Connected", color = Color.White)
                }

                else -> {
                    Icon(
                        XBluetooth,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("Ready", color = Color.White)
                }
            }
        }

        // DEVICES (ELLIPSE EDGE)

        availableDevices.forEachIndexed { index, device ->

            val angleDeg =
                START_ANGLE - (index + scrollIndex) * DEGREE_STEP

            val pos = ellipsePoint(
                angleDeg = angleDeg,
                center = Offset(centerXPx, centerYPx),
                radius = radiusPx,
                p = p,
                q = q
            )

            BluetoothDeviceCircle(
                device = device,
                isConnected = connectedDevice?.address == device.address,
                onClick = { onDeviceClick(device) },
                modifier = Modifier.offset(
                    x = with(density) { pos.x.toDp() - 40.dp },
                    y = with(density) { pos.y.toDp() - 40.dp }
                )
            )
        }
    }
}

// DEVICE CIRCLE
@Composable
private fun BluetoothDeviceCircle(
    device: BluetoothDevice,
    isConnected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val type = BluetoothHelpers.detectBluetoothDeviceType(device.name)

    Column(
        modifier = modifier
            .size(80.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    if (isConnected)
                        Color.White.copy(alpha = 0.95f)
                    else
                        Color.White.copy(alpha = 0.7f)
                )
                .border(
                    width = if (isConnected) 2.dp else 1.dp,
                    color = Color.White,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = BluetoothHelpers.bluetoothIconFor(type),
                contentDescription = device.name,
                tint = if (isConnected) Color(0xFF2196F3) else Color(0xFF1A1A1A),
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(Modifier.height(4.dp))

        RunningText(
            text = device.name,
            maxChars = 14,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            modifier = Modifier.width(80.dp)
        )
    }
}

// GEOMETRY
private fun ellipsePoint(
    angleDeg: Float,
    center: Offset,
    radius: Float,
    p: Float,
    q: Float
): Offset {
    val t = Math.toRadians(angleDeg.toDouble())
    return Offset(
        x = center.x + p * cos(t).toFloat() * radius,
        y = center.y - q * sin(t).toFloat() * radius
    )
}

// GRADIENT
private fun DrawScope.drawLeftHalfEllipseGradient(
    center: Offset,
    radius: Float,
    p: Float,
    q: Float
) {
    val gradient = Brush.radialGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.35f),
            Color(0xFF87CEEB),
            Color(0xFF5BA3D0),
            Color(0xFF2E7DB5),
            Color.Transparent
        ),
        center = center,
        radius = radius
    )

    val path = Path().apply {
        moveTo(center.x, center.y)
        arcTo(
            Rect(
                center.x - p * radius,
                center.y - q * radius,
                center.x + p * radius,
                center.y + q * radius
            ),
            startAngleDegrees = 90f,
            sweepAngleDegrees = 180f,
            forceMoveTo = false
        )
        close()
    }

    drawPath(path, brush = gradient)
}

// Bluetooth type detection + icon mapping moved to BluetoothHelpers (DRY)