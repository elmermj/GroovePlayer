package com.aethelsoft.grooveplayer.utils.theme.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val XNotifications: ImageVector
    get() {
        if (_xNotifications != null) return _xNotifications!!
        _xNotifications = ImageVector.Builder(
            name = "XNotifications",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Bell body
            path(
                fill = SolidColor(Color(0xFF38BDF8)),
                stroke = SolidColor(Color(0xFF0284C7)),
                strokeLineWidth = 1f,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6f, 9.5f)
                curveTo(6f, 6.1863f, 8.6863f, 3.5f, 12f, 3.5f)
                curveTo(15.3137f, 3.5f, 18f, 6.1863f, 18f, 9.5f)
                verticalLineTo(14f)
                lineTo(19.5f, 16f)
                curveTo(19.8f, 16.4f, 19.5f, 17f, 19f, 17f)
                horizontalLineTo(5f)
                curveTo(4.5f, 17f, 4.2f, 16.4f, 4.5f, 16f)
                lineTo(6f, 14f)
                close()
            }
            // Bell highlight
            path(
                fill = SolidColor(Color(0xFF7DD3FC)),
                stroke = SolidColor(Color.Transparent)
            ) {
                moveTo(9f, 6.8f)
                curveTo(9.8f, 5.9f, 10.9f, 5.4f, 12f, 5.4f)
                curveTo(13.5f, 5.4f, 14.8f, 6.2f, 15.5f, 7.4f)
                curveTo(14.2f, 6.5f, 12.6f, 6.2f, 11f, 6.6f)
                curveTo(10.3f, 6.8f, 9.6f, 7.2f, 9f, 7.8f)
                close()
            }
            // Clapper
            path(
                fill = SolidColor(Color(0xFFFBBF24)),
                stroke = SolidColor(Color(0xFFF59E0B)),
                strokeLineWidth = 0.6f
            ) {
                moveTo(12f, 21f)
                arcToRelative(2.2f, 2.2f, 0f, false, false, 2.1f, -1.6f)
                horizontalLineTo(9.9f)
                arcTo(2.2f, 2.2f, 0f, false, false, 12f, 21f)
                close()
            }
            // Active indicator dot
            path(
                fill = SolidColor(Color(0xFFF43F5E)),
                stroke = SolidColor(Color.Transparent)
            ) {
                moveTo(18.5f, 5.5f)
                arcToRelative(1.6f, 1.6f, 0f, true, true, 0f, 0.01f)
                close()
            }
            // Subtle top arc accent
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color(0xFFBAE6FD)),
                strokeLineWidth = 1.4f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(10f, 3.2f)
                curveTo(10.6f, 2.7f, 11.3f, 2.4f, 12f, 2.4f)
                curveTo(12.7f, 2.4f, 13.4f, 2.7f, 14f, 3.2f)
            }
        }.build()
        return _xNotifications!!
    }

private var _xNotifications: ImageVector? = null
