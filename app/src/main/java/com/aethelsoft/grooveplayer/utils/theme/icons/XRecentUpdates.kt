package com.aethelsoft.grooveplayer.utils.theme.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val XRecentUpdates: ImageVector
    get() {
        if (_xRecentUpdates != null) return _xRecentUpdates!!
        _xRecentUpdates = ImageVector.Builder(
            name = "XRecentUpdates",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Main sparkle
            path(
                fill = SolidColor(Color(0xFFFBBF24)),
                stroke = SolidColor(Color(0xFFF59E0B)),
                strokeLineWidth = 0.6f,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 2.5f)
                lineTo(13.6f, 8.2f)
                lineTo(19.5f, 9.5f)
                lineTo(13.6f, 10.8f)
                lineTo(12f, 16.5f)
                lineTo(10.4f, 10.8f)
                lineTo(4.5f, 9.5f)
                lineTo(10.4f, 8.2f)
                close()
            }
            // Small sparkles
            path(
                fill = SolidColor(Color(0xFFFDE68A)),
                stroke = SolidColor(Color.Transparent)
            ) {
                moveTo(19f, 3.5f)
                lineTo(19.7f, 5.2f)
                lineTo(21.5f, 5.8f)
                lineTo(19.7f, 6.4f)
                lineTo(19f, 8.2f)
                lineTo(18.3f, 6.4f)
                lineTo(16.5f, 5.8f)
                lineTo(18.3f, 5.2f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFFFCD34D)),
                stroke = SolidColor(Color.Transparent)
            ) {
                moveTo(6.5f, 15.5f)
                lineTo(7f, 16.8f)
                lineTo(8.4f, 17.3f)
                lineTo(7f, 17.8f)
                lineTo(6.5f, 19.2f)
                lineTo(6f, 17.8f)
                lineTo(4.6f, 17.3f)
                lineTo(6f, 16.8f)
                close()
            }
            // Orbit tick
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color(0xFFFDBA74)),
                strokeLineWidth = 1.6f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(16.5f, 17f)
                curveTo(17.8f, 15.8f, 18.5f, 14f, 18.5f, 12f)
            }
        }.build()
        return _xRecentUpdates!!
    }

private var _xRecentUpdates: ImageVector? = null
