package com.aethelworks.grooveplayer.presentation.common

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aethelworks.grooveplayer.utils.DeviceType
import com.aethelworks.grooveplayer.utils.rememberDeviceType
import com.aethelworks.grooveplayer.utils.theme.icons.XChevronUp
import com.aethelworks.grooveplayer.utils.theme.icons.XMusic
import com.aethelworks.grooveplayer.utils.theme.ui.PoppinsFontFamily
import com.aethelworks.grooveplayer.utils.theme.ui.SoftWhite

/** Tablet and large-tablet content column. Phone stays full width. */
val FlatContentMaxWidth: Dp = 560.dp

private val FlatRowRadius = 12.dp
private val FlatRowHorizontalPadding = 20.dp

@Composable
fun flatBarTitleStyle(): TextStyle = TextStyle(
    fontFamily = PoppinsFontFamily,
    fontSize = 18.sp,
    fontWeight = FontWeight.SemiBold,
    color = SoftWhite,
)

@Composable
fun flatTitleStyle(): TextStyle = TextStyle(
    fontFamily = PoppinsFontFamily,
    fontSize = 16.sp,
    fontWeight = FontWeight.Medium,
    color = SoftWhite,
)

@Composable
fun flatSubtitleStyle(): TextStyle = TextStyle(
    fontFamily = PoppinsFontFamily,
    fontSize = 13.sp,
    fontWeight = FontWeight.Normal,
    color = SoftWhite.copy(alpha = 0.55f),
)

@Composable
fun flatValueStyle(): TextStyle = TextStyle(
    fontFamily = PoppinsFontFamily,
    fontSize = 13.sp,
    fontWeight = FontWeight.Medium,
    color = SoftWhite.copy(alpha = 0.60f),
)

/**
 * Centers a 560dp column on tablet and large tablet. Phone uses the full width.
 */
@Composable
fun FlatCentered(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val wide = rememberDeviceType() != DeviceType.PHONE
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .then(if (wide) Modifier.widthIn(max = FlatContentMaxWidth) else Modifier)
                .fillMaxWidth(),
            content = content,
        )
    }
}

/**
 * Black page with the faint top wash used by the flat screens.
 * Settings sits outside [GrooveScreen], so it uses this scaffold directly.
 */
@Composable
fun FlatPage(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0A0A0A), Color(0xFF000000)),
                ),
            ),
        contentAlignment = Alignment.TopCenter,
    ) {
        FlatCentered(Modifier.fillMaxHeight(), content = content)
    }
}

@Composable
fun OverlineLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text.uppercase(),
        style = TextStyle(
            fontFamily = PoppinsFontFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.8.sp,
            color = SoftWhite.copy(alpha = 0.55f),
        ),
        modifier = modifier.padding(
            start = FlatRowHorizontalPadding,
            end = FlatRowHorizontalPadding,
            top = 24.dp,
            bottom = 8.dp,
        ),
    )
}

@Composable
fun HairlineDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(start = 16.dp)
            .fillMaxWidth()
            .height(1.dp)
            .background(SoftWhite.copy(alpha = 0.08f)),
    )
}

@Composable
fun FlatChevron(
    expanded: Boolean = false,
    contentDescription: String? = null,
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 0f else 90f,
        label = "flatChevron",
    )
    Icon(
        imageVector = XChevronUp,
        contentDescription = contentDescription,
        tint = SoftWhite.copy(alpha = 0.40f),
        modifier = Modifier
            .size(18.dp)
            .graphicsLayer { rotationZ = rotation },
    )
}

@Composable
fun FlatLeadingIcon(
    imageVector: ImageVector,
    contentDescription: String? = null,
) {
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        tint = SoftWhite.copy(alpha = 0.80f),
        modifier = Modifier.size(22.dp),
    )
}

/**
 * Flat settings / share row. No slab behind it. Pressed state is a white 6% wash.
 */
@Composable
fun FlatRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    minHeight: Dp = 56.dp,
    titleMaxLines: Int = 2,
    showDivider: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight)
                .clip(RoundedCornerShape(FlatRowRadius))
                .background(if (pressed) SoftWhite.copy(alpha = 0.06f) else Color.Transparent)
                .then(
                    if (onClick != null) {
                        Modifier.clickable(
                            interactionSource = interaction,
                            indication = ripple(color = Color.White),
                            role = Role.Button,
                            onClick = onClick,
                        )
                    } else {
                        Modifier
                    },
                )
                .padding(horizontal = FlatRowHorizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) {
                Box(contentAlignment = Alignment.Center) {
                    leading()
                }
                Spacer(Modifier.size(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = flatTitleStyle(),
                    maxLines = titleMaxLines,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = flatSubtitleStyle(),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (trailing != null) {
                Spacer(Modifier.size(12.dp))
                trailing()
            }
        }
        if (showDivider) {
            HairlineDivider()
        }
    }
}

@Composable
fun FlatProgress(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = modifier
            .fillMaxWidth()
            .height(2.dp),
        color = SoftWhite,
        trackColor = SoftWhite.copy(alpha = 0.15f),
        drawStopIndicator = {},
        gapSize = 0.dp,
    )
}

@Composable
fun FlatPillButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
) {
    val shape = RoundedCornerShape(percent = 50)
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(shape)
            .background(if (filled) Color.White else Color.Transparent)
            .then(
                if (filled) {
                    Modifier
                } else {
                    Modifier.border(1.dp, SoftWhite.copy(alpha = 0.35f), shape)
                },
            )
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = if (filled) Color.Black else Color.White),
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = TextStyle(
                fontFamily = PoppinsFontFamily,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = if (filled) Color.Black else SoftWhite,
            ),
        )
    }
}

/**
 * 120dp thin white ring. Two concentric 1dp rings expand and fade.
 * Static ring only when the system animator scale is off.
 */
@Composable
fun PulsingRing(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val motionEnabled = remember {
        val scale = runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            )
        }.getOrDefault(1f)
        scale != 0f
    }
    val ring = SoftWhite
    val transition = rememberInfiniteTransition(label = "nearbyPulse")
    val first by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
        ),
        label = "pulseA",
    )
    val second by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            initialStartOffset = StartOffset(1100),
        ),
        label = "pulseB",
    )
    Canvas(modifier.size(120.dp)) {
        val stroke = 1.dp.toPx()
        val outer = size.minDimension / 2f - stroke
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(
            color = ring.copy(alpha = 0.92f),
            radius = outer,
            center = center,
            style = Stroke(width = stroke),
        )
        if (motionEnabled) {
            drawPulse(ring, first, outer, stroke, center)
            drawPulse(ring, second, outer, stroke, center)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPulse(
    color: Color,
    progress: Float,
    outer: Float,
    stroke: Float,
    center: Offset,
) {
    drawCircle(
        color = color.copy(alpha = (1f - progress).coerceIn(0f, 1f)),
        radius = outer * (0.35f + 0.65f * progress),
        center = center,
        style = Stroke(width = stroke),
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 390, name = "Flat row")
@Composable
private fun FlatRowPreview() {
    Column(Modifier.background(Color.Black)) {
        OverlineLabel("Playback")
        FlatRow(
            title = "Repeat mode",
            subtitle = "Toggle repeat mode",
            leading = { FlatLeadingIcon(XMusic) },
            trailing = { FlatChevron() },
            onClick = {},
            showDivider = true,
        )
        FlatRow(
            title = "Cross-fade mode",
            subtitle = "Enable and set duration for smooth transitions between songs",
            leading = { FlatLeadingIcon(XMusic) },
            trailing = { Text("4 s", style = flatValueStyle()) },
            onClick = {},
            showDivider = true,
        )
        Spacer(Modifier.height(16.dp))
        FlatProgress(progress = 0.42f, modifier = Modifier.padding(horizontal = 20.dp))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 840, name = "Flat row tablet width")
@Composable
private fun FlatRowTabletPreview() {
    FlatPage {
        Column {
            OverlineLabel("Share or receive media")
            FlatRow(
                title = "Receive from nearby device (P2P)",
                leading = { FlatLeadingIcon(XMusic) },
                trailing = { FlatChevron() },
                onClick = {},
                showDivider = true,
            )
            FlatPillButton(label = "Grant permissions", onClick = {}, filled = true)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 200, name = "Pulsing ring")
@Composable
private fun PulsingRingPreview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        PulsingRing()
    }
}
