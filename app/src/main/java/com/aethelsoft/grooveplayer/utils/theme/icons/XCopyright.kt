package com.aethelsoft.grooveplayer.utils.theme.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val XCopyright: ImageVector
    get() {
        if (_xCopyright != null) return _xCopyright!!
        _xCopyright = ImageVector.Builder(
            name = "XCopyright",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Outer ring
            path(
                fill = SolidColor(Color(0xFFFBBF24)),
                stroke = SolidColor(Color(0xFFD97706)),
                strokeLineWidth = 1f
            ) {
                moveTo(12f, 22f)
                arcToRelative(10f, 10f, 0f, true, true, 0f, -20f)
                arcToRelative(10f, 10f, 0f, true, true, 0f, 20f)
            }
            // Inner disc
            path(
                fill = SolidColor(Color(0xFFFEF3C7)),
                stroke = SolidColor(Color.Transparent)
            ) {
                moveTo(12f, 19f)
                arcToRelative(7f, 7f, 0f, true, true, 0f, -14f)
                arcToRelative(7f, 7f, 0f, true, true, 0f, 14f)
            }
            // C mark
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color(0xFFB45309)),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(15.2f, 9.2f)
                curveTo(14.4f, 8.2f, 13.3f, 7.6f, 12f, 7.6f)
                curveTo(9.6f, 7.6f, 7.7f, 9.5f, 7.7f, 12f)
                curveTo(7.7f, 14.5f, 9.6f, 16.4f, 12f, 16.4f)
                curveTo(13.3f, 16.4f, 14.4f, 15.8f, 15.2f, 14.8f)
            }
        }.build()
        return _xCopyright!!
    }

private var _xCopyright: ImageVector? = null
