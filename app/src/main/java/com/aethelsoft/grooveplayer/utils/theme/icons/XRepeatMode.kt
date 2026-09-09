package com.aethelsoft.grooveplayer.utils.theme.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val XRepeatMode: ImageVector
    get() {
        if (_xRepeatMode != null) return _xRepeatMode!!
        _xRepeatMode = ImageVector.Builder(
            name = "XRepeatMode",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Top loop
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color(0xFF34D399)),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(17f, 2f)
                lineTo(21f, 6f)
                lineTo(17f, 10f)
                moveTo(3f, 11f)
                verticalLineTo(10f)
                arcToRelative(4f, 4f, 0f, false, true, 4f, -4f)
                horizontalLineTo(21f)
            }
            // Bottom loop
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color(0xFF10B981)),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(7f, 22f)
                lineTo(3f, 18f)
                lineTo(7f, 14f)
                moveTo(21f, 13f)
                verticalLineTo(14f)
                arcToRelative(4f, 4f, 0f, false, true, -4f, 4f)
                horizontalLineTo(3f)
            }
        }.build()
        return _xRepeatMode!!
    }

private var _xRepeatMode: ImageVector? = null
