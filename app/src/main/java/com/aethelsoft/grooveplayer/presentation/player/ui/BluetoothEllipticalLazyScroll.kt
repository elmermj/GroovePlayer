package com.aethelsoft.grooveplayer.presentation.player.ui

import XCheckCircle
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.aethelsoft.grooveplayer.domain.model.BluetoothDevice
import com.aethelsoft.grooveplayer.utils.theme.icons.*
import com.aethelsoft.grooveplayer.utils.theme.shader.ELLIPSE_SHADER
import com.aethelsoft.grooveplayer.utils.theme.ui.RunningText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin


/* ───────────────────────── CONSTANTS ───────────────────────── */

private const val BASE_ANGLE = 270f      // 12 o’clock
private const val DEGREE_SPACING = 20f   // per item

/* ───────────────────────── MAIN COMPOSABLE ───────────────────────── */

@Composable
fun BluetoothEllipticalLazyScroll(
    availableDevices: List<BluetoothDevice>,
    connectedDevice: BluetoothDevice?,
    onDeviceClick: (BluetoothDevice) -> Unit,
    maxHeight: Dp,
    modifier: Modifier = Modifier,
    connectingDeviceAddress: String? = null,
    connectionSuccessDisplay: Boolean = false,
    connectionFailedDisplay: Boolean = false,
    hasBluetoothPermissions: Boolean = true,
    onRequestBluetoothPermission: () -> Unit = {}
) {
    val density = LocalDensity.current

    /* ───── SHADER SCALE ───── */
    val shaderScale = remember { Animatable(0f) }
    val colorMix = remember { Animatable(0f) }
    val colorMixFail = remember { Animatable(0f) }
    var isInteracting by remember { mutableStateOf(false) }
    var hitLimit by remember { mutableStateOf(false) }

    val isConnecting = connectingDeviceAddress != null
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val shaderPulse = if (isConnecting) pulse else 1f

    LaunchedEffect(Unit) {
        shaderScale.animateTo(1f, tween(300, easing = FastOutSlowInEasing))
    }

    LaunchedEffect(connectionSuccessDisplay) {
        if (connectionSuccessDisplay) {
            colorMixFail.snapTo(0f)
            colorMix.animateTo(1f, tween(300, easing = FastOutSlowInEasing))
            delay(1000)
            colorMix.animateTo(0f, tween(400, easing = FastOutSlowInEasing))
        } else {
            colorMix.snapTo(0f)
        }
    }

    LaunchedEffect(connectionFailedDisplay) {
        if (connectionFailedDisplay) {
            colorMix.snapTo(0f)
            colorMixFail.animateTo(1f, tween(300, easing = FastOutSlowInEasing))
            delay(1000)
            colorMixFail.animateTo(0f, tween(400, easing = FastOutSlowInEasing))
        } else {
            colorMixFail.snapTo(0f)
        }
    }

    /* ───── ELLIPSE GEOMETRY ───── */
    val radiusDp = maxHeight * 0.42f
    val radiusPx = with(density) { radiusDp.toPx() }

    val p = 0.55f
    val q = 1.0f

    val panelWidthPx = with(density) { (360.dp + 32.dp).toPx() }
    val centerXPx = panelWidthPx
    val centerYPx = with(density) { (maxHeight * 0.5f).toPx() }

    /* ───── SCROLL LIMITS ───── */
    val angleSpanFromTop = BASE_ANGLE - 180f
    val minOffset = -(angleSpanFromTop / DEGREE_SPACING)
    val maxOffset = (availableDevices.size - 1) - (angleSpanFromTop / DEGREE_SPACING)

    /* ───── SCROLL POSITION ───── */
    var scrollOffset by remember { mutableFloatStateOf(0f) }

    val overshoot = 0.15f

    /* ───── SCROLL HANDLER ───── */
    val scrollState = rememberScrollableState { delta ->
        if (!isInteracting) isInteracting = true
        val nextOffset = scrollOffset + delta * 0.01f

        hitLimit = nextOffset < minOffset - overshoot || nextOffset > maxOffset + overshoot

        scrollOffset = when {
            nextOffset < minOffset ->
                minOffset + (nextOffset - minOffset) * 0.35f
            nextOffset > maxOffset ->
                maxOffset + (nextOffset - maxOffset) * 0.35f
            else -> nextOffset
        }

        delta
    }

    /* ───── SCALE UP ON INTERACT ───── */
    LaunchedEffect(isInteracting) {
        if (isInteracting) {
            shaderScale.animateTo(1f, tween(150))
        }
    }

    /* ───── SCALE UP ON LIMIT ───── */
    LaunchedEffect(hitLimit) {
        if (hitLimit) {
            shaderScale.animateTo(1.15f, tween(150))
        }
    }

    /* ───── IDLE SHRINK ───── */
    LaunchedEffect(scrollState.isScrollInProgress) {
        if (!scrollState.isScrollInProgress) {
            delay(100)
            if (!scrollState.isScrollInProgress) {
                shaderScale.animateTo(0.5f, tween(150, easing = FastOutSlowInEasing))
                isInteracting = false
            }
        }
    }

    /* ───── PERMISSION UI ───── */
    if (!hasBluetoothPermissions) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Bluetooth access required",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        "Grant Bluetooth permission to scan and connect to audio devices.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Button(
                        onClick = onRequestBluetoothPermission,
                        shape = RoundedCornerShape(100.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        )
                    ) {
                        Text("Grant access")
                    }
                }
            }
        }
        return
    }

    /* ───── UI ───── */
    Box(
        modifier = modifier
            .fillMaxSize()
            .scrollable(
                state = scrollState,
                orientation = Orientation.Vertical
            )
            .clipToBounds()
    ) {
        EllipticalGradientBackground(
            center = Offset(centerXPx, centerYPx),
            radiusX = radiusPx,
            radiusY = radiusPx * 1.15f,
            p = p,
            q = q,
            shaderScale = shaderScale.value,
            pulse = shaderPulse,
            colorMix = colorMix.value,
            colorMixFail = colorMixFail.value,
            modifier = Modifier.matchParentSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 48.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            AnimatedVisibility(
                visible = connectionSuccessDisplay,
                enter = fadeIn(animationSpec = tween(200)) +
                        slideInVertically(
                            initialOffsetY = { it / 4 },
                            animationSpec = tween(300, easing = FastOutSlowInEasing)
                        ),
                exit = fadeOut(animationSpec = tween(300)) +
                        slideOutVertically(
                            targetOffsetY = { it / 4 },
                            animationSpec = tween(300, easing = FastOutSlowInEasing)
                        )
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20).copy(alpha = 0.9f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            XCheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            "BT connection successful",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White
                        )
                    }
                }
            }
            AnimatedVisibility(
                visible = connectionFailedDisplay,
                enter = fadeIn(animationSpec = tween(200)) +
                        slideInVertically(
                            initialOffsetY = { it / 4 },
                            animationSpec = tween(300, easing = FastOutSlowInEasing)
                        ),
                exit = fadeOut(animationSpec = tween(300)) +
                        slideOutVertically(
                            targetOffsetY = { it / 4 },
                            animationSpec = tween(300, easing = FastOutSlowInEasing)
                        )
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFB71C1C).copy(alpha = 0.9f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            XClose,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            "BT connection failed",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White
                        )
                    }
                }
            }
        }

        availableDevices.forEachIndexed { index, device ->
            val virtualIndex = index - scrollOffset
            val angleDeg = BASE_ANGLE - virtualIndex * DEGREE_SPACING
            if (angleDeg !in 90f..270f) return@forEachIndexed

            val pos = ellipsePoint(
                angleDeg = angleDeg,
                center = Offset(centerXPx, centerYPx),
                radius = radiusPx,
                p = p,
                q = q
            )

            val (scale, alpha) = xToScaleAlpha(
                x = pos.x,
                minX = centerXPx - p * radiusPx,
                maxX = centerXPx
            )

            val isConnectingThis = connectingDeviceAddress == device.address

            BluetoothDeviceCircle(
                device = device,
                isConnected = connectedDevice?.address == device.address,
                isConnecting = isConnectingThis,
                onClick = { onDeviceClick(device) },
                modifier = Modifier
                    .offset(
                        x = with(density) { pos.x.toDp() - 40.dp },
                        y = with(density) { pos.y.toDp() - 40.dp }
                    )
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
            )
        }
    }
}

/* ───────────────────────── DEVICE ITEM ───────────────────────── */

@Composable
private fun BluetoothDeviceCircle(
    device: BluetoothDevice,
    isConnected: Boolean,
    isConnecting: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val type = detectBluetoothDeviceType(device.name)
    val pulseAlpha by rememberInfiniteTransition(label = "circlePulse").animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "circlePulse"
    )

    Column(
        modifier = modifier
            .size(80.dp)
            .clickable(enabled = !isConnecting, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isConnected -> Color.White.copy(alpha = 0.95f)
                        isConnecting -> Color.White.copy(alpha = 0.5f + pulseAlpha * 0.25f)
                        else -> Color.White.copy(alpha = 0.7f)
                    }
                )
                .border(
                    width = when {
                        isConnected -> 2.dp
                        isConnecting -> 2.dp
                        else -> 1.dp
                    },
                    color = if (isConnecting) Color.White.copy(alpha = pulseAlpha) else Color.White,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = bluetoothIconFor(type),
                contentDescription = device.name,
                tint = when {
                    isConnected -> Color(0xFF2196F3)
                    isConnecting -> Color(0xFF2196F3).copy(alpha = pulseAlpha)
                    else -> Color(0xFF1A1A1A)
                },
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(Modifier.height(4.dp))

        RunningText(
            text = when {
                isConnecting -> "Connecting…"
                else -> device.name
            },
            maxChars = 14,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            modifier = Modifier.width(80.dp)
        )
    }
}

/* ───────────────────────── GEOMETRY ───────────────────────── */

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

/* ───────────────────────── SCALE / ALPHA FROM X ───────────────────────── */

private fun xToScaleAlpha(
    x: Float,
    minX: Float,
    maxX: Float
): Pair<Float, Float> {
    val t = ((x - minX) / (maxX - minX)).coerceIn(0f, 1f)
    val scale = 1f - t
    val alpha = 1f - t
    return scale to alpha
}

/* ───────────────────────── GRADIENT ───────────────────────── */

private fun DrawScope.drawEllipticalFieldGradient(
    center: Offset,
    radiusX: Float,
    radiusY: Float,
    p: Float,
    q: Float
) {
    val a = radiusX * p
    val b = radiusY * q

    val fadeWidth = 0.35f // thickness of glow inside ellipse

    for (y in 0 until size.height.toInt()) {
        for (x in 0 until size.width.toInt()) {

            val dx = x - center.x
            val dy = y - center.y

            val d = kotlin.math.sqrt(
                (dx * dx) / (a * a) +
                        (dy * dy) / (b * b)
            )

            // Only draw inside left half
            if (dx > 0f || d > 1f) continue

            val t = ((1f - d) / fadeWidth).coerceIn(0f, 1f)

            val color = Color(
                red = lerp(0.18f, 0.53f, t),
                green = lerp(0.45f, 0.81f, t),
                blue = lerp(0.70f, 0.92f, t),
                alpha = t * 0.65f
            )

            drawRect(
                color = color,
                topLeft = Offset(x.toFloat(), y.toFloat()),
                size = Size(1f, 1f)
            )
        }
    }
}

private fun DrawScope.drawLeftHalfEllipseGradient(
    center: Offset,
    radiusX: Float,
    radiusY: Float
) {
    // Normalize space: scale Y so ellipse becomes circle in math-space
    val scaleY = radiusY / radiusX

    drawIntoCanvas { canvas ->
        canvas.save()

        canvas.translate(center.x, center.y)

        canvas.scale(1f, scaleY)

        canvas.translate(-center.x, -center.y)

        val path = Path().apply {
            moveTo(center.x, center.y)
            arcTo(
                Rect(
                    center.x - radiusX,
                    center.y - radiusX,
                    center.x + radiusX,
                    center.y + radiusX
                ),
                startAngleDegrees = 90f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false
            )
            close()
        }

        canvas.clipPath(path)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0xFF5BA3D0).copy(alpha = 0.25f),
                    Color(0xFF87CEEB).copy(alpha = 0.55f),
                    Color(0xFF5BA3D0).copy(alpha = 0.45f),
                    Color(0xFF2E7DB5).copy(alpha = 0.30f),
                    Color.Transparent
                ),
                center = center,
                radius = radiusX
            ),
            center = center,
            radius = radiusX
        )

        canvas.restore()
    }
}

/**
 * Canvas-only fallback for API 24–32. Draws the left-half ellipse glow with [pulse] scale,
 * [colorMix] (0 = blue, 1 = green), and [colorMixFail] (0 = normal, 1 = red) for connection failed.
 */
private fun DrawScope.drawLeftHalfEllipseGradientFallback(
    center: Offset,
    radiusX: Float,
    radiusY: Float,
    p: Float,
    q: Float,
    shaderScale: Float,
    pulse: Float,
    colorMix: Float,
    colorMixFail: Float
) {
    val effRadiusX = radiusX * p * shaderScale * pulse
    val effRadiusY = radiusY * q * shaderScale * pulse
    val scaleY = effRadiusY / effRadiusX

    val blueColors = listOf(
        Color.Transparent,
        Color(0xFF5BA3D0).copy(alpha = 0.25f),
        Color(0xFF87CEEB).copy(alpha = 0.55f),
        Color(0xFF5BA3D0).copy(alpha = 0.45f),
        Color(0xFF2E7DB5).copy(alpha = 0.30f),
        Color.Transparent
    )
    val greenColors = listOf(
        Color.Transparent,
        Color(0xFF38C059).copy(alpha = 0.25f),
        Color(0xFF59D973).copy(alpha = 0.55f),
        Color(0xFF38C059).copy(alpha = 0.45f),
        Color(0xFF1F8C47).copy(alpha = 0.30f),
        Color.Transparent
    )
    val redColors = listOf(
        Color.Transparent,
        Color(0xFFB71C1C).copy(alpha = 0.25f),
        Color(0xFFE53935).copy(alpha = 0.55f),
        Color(0xFFC62828).copy(alpha = 0.45f),
        Color(0xFFB71C1C).copy(alpha = 0.30f),
        Color.Transparent
    )
    val baseColors = blueColors.zip(greenColors) { b, g ->
        Color(
            red = lerp(b.red, g.red, colorMix),
            green = lerp(b.green, g.green, colorMix),
            blue = lerp(b.blue, g.blue, colorMix),
            alpha = lerp(b.alpha, g.alpha, colorMix)
        )
    }
    val colors = baseColors.zip(redColors) { base, r ->
        Color(
            red = lerp(base.red, r.red, colorMixFail),
            green = lerp(base.green, r.green, colorMixFail),
            blue = lerp(base.blue, r.blue, colorMixFail),
            alpha = lerp(base.alpha, r.alpha, colorMixFail)
        )
    }

    drawIntoCanvas { canvas ->
        canvas.save()
        canvas.translate(center.x, center.y)
        canvas.scale(1f, scaleY)
        canvas.translate(-center.x, -center.y)

        val path = Path().apply {
            moveTo(center.x, center.y)
            arcTo(
                Rect(
                    center.x - effRadiusX,
                    center.y - effRadiusX,
                    center.x + effRadiusX,
                    center.y + effRadiusX
                ),
                startAngleDegrees = 90f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false
            )
            close()
        }
        canvas.clipPath(path)

        drawCircle(
            brush = Brush.radialGradient(
                colors = colors,
                center = center,
                radius = effRadiusX
            ),
            center = center,
            radius = effRadiusX
        )
        canvas.restore()
    }
}

/* ───────────────────────── HELPERS ───────────────────────── */

@Composable
private fun bluetoothIconFor(type: BluetoothDeviceType) = when (type) {
    BluetoothDeviceType.EARBUDS,
    BluetoothDeviceType.HEADPHONES -> XHeadphones
    BluetoothDeviceType.SPEAKER -> XSpeaker
    BluetoothDeviceType.PHONE -> XSmartphone
    BluetoothDeviceType.UNKNOWN -> XBluetooth
}

@Composable
fun EllipticalGradientBackground(
    center: Offset,
    radiusX: Float,
    radiusY: Float,
    p: Float,
    q: Float,
    modifier: Modifier = Modifier,
    shaderScale: Float,
    pulse: Float = 1f,
    colorMix: Float = 0f,
    colorMixFail: Float = 0f
) {
    if (Build.VERSION.SDK_INT >= 33) {
        EllipticalGradientBackgroundShader(
            center = center,
            radiusX = radiusX,
            radiusY = radiusY,
            p = p,
            q = q,
            modifier = modifier,
            shaderScale = shaderScale,
            pulse = pulse,
            colorMix = colorMix,
            colorMixFail = colorMixFail
        )
    } else {
        Canvas(modifier = modifier.fillMaxSize()) {
            drawLeftHalfEllipseGradientFallback(
                center = center,
                radiusX = radiusX,
                radiusY = radiusY,
                p = p,
                q = q,
                shaderScale = shaderScale,
                pulse = pulse,
                colorMix = colorMix,
                colorMixFail = colorMixFail
            )
        }
    }
}

@Composable
@RequiresApi(33)
private fun EllipticalGradientBackgroundShader(
    center: Offset,
    radiusX: Float,
    radiusY: Float,
    p: Float,
    q: Float,
    modifier: Modifier,
    shaderScale: Float,
    pulse: Float,
    colorMix: Float,
    colorMixFail: Float
) {
    val shader = remember {
        RuntimeShader(ELLIPSE_SHADER)
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        shader.setFloatUniform("resolution", size.width, size.height)
        shader.setFloatUniform("center", center.x, center.y)
        shader.setFloatUniform("radiusX", radiusX)
        shader.setFloatUniform("radiusY", radiusY)
        shader.setFloatUniform("p", p)
        shader.setFloatUniform("q", q)
        shader.setFloatUniform("scale", shaderScale)
        shader.setFloatUniform("pulse", pulse)
        shader.setFloatUniform("colorMix", colorMix)
        shader.setFloatUniform("failMix", colorMixFail)

        drawRect(
            brush = ShaderBrush(shader),
            size = size
        )
    }
}

private fun detectBluetoothDeviceType(name: String): BluetoothDeviceType {
    val n = name.lowercase()
    return when {
        n.contains("bud") || n.contains("airpod") || n.contains("tws") ->
            BluetoothDeviceType.EARBUDS
        n.contains("headphone") || n.contains("xm") || n.contains("beats") ->
            BluetoothDeviceType.HEADPHONES
        n.contains("speaker") || n.contains("jbl") || n.contains("sound") ->
            BluetoothDeviceType.SPEAKER
        n.contains("phone") || n.contains("pixel") || n.contains("iphone") ->
            BluetoothDeviceType.PHONE
        else -> BluetoothDeviceType.UNKNOWN
    }
}