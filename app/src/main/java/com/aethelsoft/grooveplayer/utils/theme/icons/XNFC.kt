package com.aethelsoft.grooveplayer.utils.theme.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val XNFC: ImageVector
    get() {
        if (_xnfc != null) return _xnfc!!
        _xnfc = ImageVector.Builder(
            name = "XNFC",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {

            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                fill = SolidColor(Color.Transparent)
            ) {
                moveTo(5f, 8f)
                lineTo(10f, 16f)
            }

            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                fill = SolidColor(Color.Transparent)
            ) {
                moveTo(18.7224f, 20.5f)
                curveTo(20.2145f, 17.9157f, 21f, 14.9841f, 21f, 12f)
                curveTo(21f, 9.0159f, 20.2145f, 6.0843f, 18.7224f, 3.5f)
            }

            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                fill = SolidColor(Color.Transparent)
            ) {
                moveTo(14.3923f, 18f)
                curveTo(15.4455f, 16.1758f, 16f, 14.1064f, 16f, 12f)
                curveTo(16f, 9.8936f, 15.4455f, 7.8242f, 14.3923f, 6f)
            }

            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                fill = SolidColor(Color.Transparent)
            ) {
                moveTo(9.9282f, 16f)
                curveTo(10.6303f, 14.7838f, 11f, 13.4043f, 11f, 12f)
                curveTo(11f, 10.5957f, 10.6303f, 9.2162f, 9.9282f, 8f)
            }

            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                fill = SolidColor(Color.Transparent)
            ) {
                moveTo(5.0718f, 16f)
                curveTo(4.3697f, 14.7838f, 4f, 13.4043f, 4f, 12f)
                curveTo(4f, 10.5957f, 4.3697f, 9.2162f, 5.0718f, 8f)
            }
        }.build()

        return _xnfc!!
    }

private var _xnfc: ImageVector? = null