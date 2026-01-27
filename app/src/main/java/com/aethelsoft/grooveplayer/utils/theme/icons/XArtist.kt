package com.aethelsoft.grooveplayer.utils.theme.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val XArtist: ImageVector
    get() {
        if (_xArtist != null) return _xArtist!!
        _xArtist = ImageVector.Builder(
            name = "XArtist",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {

            // Head circle
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(17f, 8f)
                arcToRelative(5f, 5f, 0f, true, true, -10f, 0f)
                arcToRelative(5f, 5f, 0f, true, true, 10f, 0f)
            }

            // Body curve: M20 21a8 8 0 0 0-16 0
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(20f, 21f)
                arcToRelative(8f, 8f, 0f, false, false, -16f, 0f)
            }

        }.build()
        return _xArtist!!
    }

private var _xArtist: ImageVector? = null