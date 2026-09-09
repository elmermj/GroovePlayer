package com.aethelsoft.grooveplayer.utils.theme.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val XAccountType: ImageVector
    get() {
        if (_xAccountType != null) return _xAccountType!!
        _xAccountType = ImageVector.Builder(
            name = "XAccountType",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Head
            path(
                fill = SolidColor(Color(0xFF60A5FA)),
                stroke = SolidColor(Color.Transparent)
            ) {
                moveTo(12f, 11f)
                arcToRelative(3.5f, 3.5f, 0f, true, true, 0f, -7f)
                arcToRelative(3.5f, 3.5f, 0f, true, true, 0f, 7f)
            }
            // Body
            path(
                fill = SolidColor(Color(0xFF3B82F6)),
                stroke = SolidColor(Color.Transparent)
            ) {
                moveTo(5f, 20.5f)
                verticalLineTo(19f)
                curveTo(5f, 16.2386f, 7.2386f, 14f, 10f, 14f)
                horizontalLineTo(14f)
                curveTo(16.7614f, 14f, 19f, 16.2386f, 19f, 19f)
                verticalLineTo(20.5f)
                curveTo(19f, 21.0523f, 18.5523f, 21.5f, 18f, 21.5f)
                horizontalLineTo(6f)
                curveTo(5.4477f, 21.5f, 5f, 21.0523f, 5f, 20.5f)
                close()
            }
            // Premium star badge
            path(
                fill = SolidColor(Color(0xFFFBBF24)),
                stroke = SolidColor(Color(0xFFF59E0B)),
                strokeLineWidth = 0.5f,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(18.5f, 3.5f)
                lineTo(19.3f, 5.7f)
                lineTo(21.5f, 6.5f)
                lineTo(19.3f, 7.3f)
                lineTo(18.5f, 9.5f)
                lineTo(17.7f, 7.3f)
                lineTo(15.5f, 6.5f)
                lineTo(17.7f, 5.7f)
                close()
            }
        }.build()
        return _xAccountType!!
    }

private var _xAccountType: ImageVector? = null
