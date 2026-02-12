package com.aethelsoft.grooveplayer.utils.theme.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val XEdit: ImageVector
    get() {
        if (_xEdit != null) return _xEdit!!
        _xEdit = ImageVector.Builder(
            name = "XEdit",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {

            // Line: M13 21h8
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(13f, 21f)
                horizontalLineTo(21f)
            }

            // Pen shape
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(21.174f, 6.812f)

                // a1 1 0 0 0-3.986-3.987  → approximated with curveTo
                curveTo(20.1f, 5.74f, 18.26f, 3.9f, 17.188f, 2.825f)

                // L3.842 16.174
                lineTo(3.842f, 16.174f)

                // a2 2 0 0 0-.5.83
                curveTo(3.6f, 16.4f, 3.3f, 16.7f, 3.342f, 17.004f)

                // l-1.321 4.352
                lineTo(2.021f, 21.356f)

                // a.5 .5 0 0 0 .623 .622
                curveTo(2.3f, 21.9f, 2.9f, 22.1f, 2.644f, 21.978f)

                // l4.353-1.32
                lineTo(6.997f, 20.658f)

                // a2 2 0 0 0 .83-.497
                curveTo(7.4f, 20.4f, 7.9f, 20.1f, 7.827f, 20.161f)

                close()
            }

        }.build()
        return _xEdit!!
    }

private var _xEdit: ImageVector? = null