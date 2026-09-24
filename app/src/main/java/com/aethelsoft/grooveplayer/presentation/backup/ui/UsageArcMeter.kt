package com.aethelsoft.grooveplayer.presentation.backup.ui

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aethelsoft.grooveplayer.domain.model.StorageEntitlement
import com.aethelsoft.grooveplayer.utils.StorageFormatUtils
import com.aethelsoft.grooveplayer.utils.theme.ui.GrooveTheme
import com.aethelsoft.grooveplayer.utils.theme.ui.SoftWhite
import kotlin.math.min

/**
 * Single 270° circular arc meter (bottom gap) — one track stroke + one fill stroke only.
 * Tachometer-style fill animation from 0 → used/quota on appear.
 * Honors system reduced-motion (animator duration scale / accessibility).
 */
@Composable
fun UsageArcMeter(
    storage: StorageEntitlement,
    modifier: Modifier = Modifier,
) {
    val total = storage.quotaBytes.coerceAtLeast(1L)
    val used = storage.usedBytes.coerceAtLeast(0L)
    val target = (used.toFloat() / total.toFloat()).coerceIn(0f, 1f)

    val reduceMotion = rememberReduceMotion()
    val progress = remember { Animatable(0f) }

    LaunchedEffect(target, reduceMotion, used, total) {
        if (reduceMotion) {
            progress.snapTo(target)
        } else {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = target,
                animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            )
        }
    }

    val trackColor = GrooveTheme.colors.surfaceRaised.copy(alpha = 0.55f)
    val accent = GrooveTheme.colors.accent
    val warn = Color(0xFFFFB74D)
    val error = Color(0xFFFF5252)
    val fillColor = when {
        storage.overQuota || storage.hardStop -> error
        storage.softWarn -> warn
        else -> accent
    }
    val (usedLabel, quotaWithUnit) = StorageFormatUtils.formatQuotaPair(used, total)
    val onSurface = GrooveTheme.colors.onSurface

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(200.dp)) {
            val stroke = 16.dp.toPx()
            val diameter = min(size.width, size.height) - stroke
            val topLeft = Offset(
                (size.width - diameter) / 2f,
                (size.height - diameter) / 2f,
            )
            val arcSize = Size(diameter, diameter)
            // Single ring: 270° sweep, gap centered at bottom.
            // Compose angles: 0° = 3 o'clock, clockwise positive → start at 135°.
            val startAngle = 135f
            val fullSweep = 270f

            // 1) Track only
            drawArc(
                color = trackColor,
                startAngle = startAngle,
                sweepAngle = fullSweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )

            // 2) Fill only (same radius/stroke — never additional rings)
            val animatedSweep = fullSweep * progress.value
            if (animatedSweep > 0.05f) {
                drawArc(
                    color = fillColor,
                    startAngle = startAngle,
                    sweepAngle = animatedSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 8.dp),
        ) {
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            color = onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                        ),
                    ) {
                        append(usedLabel)
                    }
                    withStyle(
                        SpanStyle(
                            color = SoftWhite.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Medium,
                            fontSize = 18.sp,
                        ),
                    ) {
                        append("/")
                        append(quotaWithUnit)
                    }
                },
            )
            Text(
                text = "Cloud usage",
                style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
                color = SoftWhite.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
fun rememberReduceMotion(): Boolean {
    val accessibilityManager = LocalAccessibilityManager.current
    val context = LocalContext.current
    return remember(accessibilityManager) {
        val durationScale = try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            )
        } catch (_: Throwable) {
            1f
        }
        val transitionScale = try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.TRANSITION_ANIMATION_SCALE,
                1f,
            )
        } catch (_: Throwable) {
            1f
        }
        durationScale == 0f || transitionScale == 0f || accessibilityManager == null && false
    }
}
