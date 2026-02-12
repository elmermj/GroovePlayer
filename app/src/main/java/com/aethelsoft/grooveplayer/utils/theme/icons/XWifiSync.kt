package com.aethelsoft.grooveplayer.utils.theme.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val XWifiSync: ImageVector
    get() {
        if (_xWifiSync != null) return _xWifiSync!!
        _xWifiSync = ImageVector.Builder(
            name = "XWifiSync",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {

            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            ) {
                moveTo(11.965f, 10.105f)
                verticalLineTo(14.105f)
                lineTo(13.5f, 12.5f)
                curveTo(15.5f, 10.5f, 19.5f, 10.8f, 21.5f, 14f)
            }

            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                fill = SolidColor(Color.Transparent)
            ) {
                moveTo(11.965f, 14.105f)
                horizontalLineTo(15.965f)
            }

            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            ) {
                moveTo(17.965f, 18.105f)
                horizontalLineTo(21.965f)
                lineTo(20.43f, 19.71f)
                curveTo(18.43f, 21.71f, 14.43f, 21.4f, 12.43f, 18.21f)
            }

            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                fill = SolidColor(Color.Transparent)
            ) {
                moveTo(2f, 8.82f)
                curveTo(8f, 3f, 16f, 3f, 22f, 8.82f)
            }

            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                fill = SolidColor(Color.Transparent)
            ) {
                moveTo(21.965f, 22.105f)
                verticalLineTo(18.105f)
            }

            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                fill = SolidColor(Color.Transparent)
            ) {
                moveTo(5f, 12.86f)
                curveTo(6f, 11.6f, 7f, 11f, 8f, 10.828f)
            }

            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                fill = SolidColor(Color.Transparent)
            ) {
                moveTo(8.5f, 16.429f)
                lineTo(8.51f, 16.429f)
            }
        }.build()

        return _xWifiSync!!
    }

private var _xWifiSync: ImageVector? = null