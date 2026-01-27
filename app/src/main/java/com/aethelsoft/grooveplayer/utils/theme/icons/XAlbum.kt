package com.aethelsoft.grooveplayer.utils.theme.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val XAlbum: ImageVector
    get() {
        if (_xAlbum != null) return _xAlbum!!
        _xAlbum = ImageVector.Builder(
            name = "XAlbum",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {

            // Outer circle r=10
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(22f, 12f)
                arcToRelative(10f, 10f, 0f, true, true, -20f, 0f)
                arcToRelative(10f, 10f, 0f, true, true, 20f, 0f)
            }

            // Left inner curve
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6f, 12f)
                curveTo(6f, 10.3f, 6.7f, 8.8f, 7.8f, 7.8f)
            }

            // Center circle r=2
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f
            ) {
                moveTo(14f, 12f)
                arcToRelative(2f, 2f, 0f, true, true, -4f, 0f)
                arcToRelative(2f, 2f, 0f, true, true, 4f, 0f)
            }

            // Right inner curve
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(18f, 12f)
                curveTo(18f, 13.7f, 17.3f, 15.2f, 16.2f, 16.2f)
            }

        }.build()
        return _xAlbum!!
    }

private var _xAlbum: ImageVector? = null