package com.aethelsoft.grooveplayer.utils.theme.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val XSearch: ImageVector
    get() {
        if (_xSearch != null) return _xSearch!!
        _xSearch = ImageVector.Builder(
            name = "XSearch",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {

            // Line: m21 21 -4.34 -4.34
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(21f, 21f)
                lineTo(16.66f, 16.66f)
            }

            // Circle: cx=11 cy=11 r=8
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f
            ) {
                moveTo(19f, 11f)
                arcToRelative(8f, 8f, 0f, true, true, -16f, 0f)
                arcToRelative(8f, 8f, 0f, true, true, 16f, 0f)
            }

        }.build()
        return _xSearch!!
    }

private var _xSearch: ImageVector? = null