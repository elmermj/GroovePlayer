package com.aethelsoft.grooveplayer.utils.theme.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val XClearCache: ImageVector
    get() {
        if (_xClearCache != null) return _xClearCache!!
        _xClearCache = ImageVector.Builder(
            name = "XClearCache",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Bin body
            path(
                fill = SolidColor(Color(0xFFF87171)),
                stroke = SolidColor(Color(0xFFDC2626)),
                strokeLineWidth = 1f,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(7f, 8f)
                horizontalLineTo(17f)
                lineTo(16f, 20f)
                curveTo(16f, 20.5523f, 15.5523f, 21f, 15f, 21f)
                horizontalLineTo(9f)
                curveTo(8.4477f, 21f, 8f, 20.5523f, 8f, 20f)
                close()
            }
            // Lid
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color(0xFFFCA5A5)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(4f, 7f)
                horizontalLineTo(20f)
            }
            // Handle
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color(0xFFFDBA74)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(9.5f, 7f)
                verticalLineTo(5.5f)
                curveTo(9.5f, 4.6716f, 10.1716f, 4f, 11f, 4f)
                horizontalLineTo(13f)
                curveTo(13.8284f, 4f, 14.5f, 4.6716f, 14.5f, 5.5f)
                verticalLineTo(7f)
            }
            // Inner lines
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color(0xFFFEE2E2)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(10.5f, 11f)
                verticalLineTo(17.5f)
                moveTo(13.5f, 11f)
                verticalLineTo(17.5f)
            }
        }.build()
        return _xClearCache!!
    }

private var _xClearCache: ImageVector? = null
