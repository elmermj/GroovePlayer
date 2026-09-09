package com.aethelsoft.grooveplayer.utils.theme.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val XShuffleMode: ImageVector
    get() {
        if (_xShuffleMode != null) return _xShuffleMode!!
        _xShuffleMode = ImageVector.Builder(
            name = "XShuffleMode",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color(0xFFA78BFA)),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(2f, 18f)
                horizontalLineTo(3.4f)
                curveTo(4.7f, 18f, 5.9f, 17.4f, 6.7f, 16.3f)
                lineTo(12.8f, 7.7f)
                curveTo(13.5f, 6.6f, 14.8f, 6f, 16.1f, 6f)
                horizontalLineTo(22f)
            }
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color(0xFFC084FC)),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(18f, 2f)
                lineTo(22f, 6f)
                lineTo(18f, 10f)
            }
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color(0xFFE879F9)),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(2f, 6f)
                horizontalLineTo(3.9f)
                curveTo(5.4f, 6f, 6.8f, 6.9f, 7.5f, 8.2f)
                moveTo(22f, 18f)
                horizontalLineTo(16.1f)
                curveTo(14.8f, 18f, 13.5f, 17.3f, 12.8f, 16.2f)
                lineTo(12.3f, 15.4f)
            }
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color(0xFFD946EF)),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(18f, 14f)
                lineTo(22f, 18f)
                lineTo(18f, 22f)
            }
        }.build()
        return _xShuffleMode!!
    }

private var _xShuffleMode: ImageVector? = null
