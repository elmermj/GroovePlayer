package com.aethelsoft.grooveplayer.utils.theme.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val XShareMusic: ImageVector
    get() {
        if (_xShareMusic != null) return _xShareMusic!!
        _xShareMusic = ImageVector.Builder(
            name = "XShareMusic",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Outer broadcast arc
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color(0xFF38BDF8)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(4.5f, 8.5f)
                curveTo(8.5f, 4.5f, 15.5f, 4.5f, 19.5f, 8.5f)
            }
            // Mid broadcast arc
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color(0xFF22D3EE)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(7f, 11.5f)
                curveTo(9.5f, 9f, 14.5f, 9f, 17f, 11.5f)
            }
            // Music note body
            path(
                fill = SolidColor(Color(0xFF67E8F9)),
                stroke = SolidColor(Color.Transparent)
            ) {
                moveTo(14f, 9f)
                verticalLineTo(15.5f)
                arcToRelative(2.5f, 2.5f, 0f, true, true, -2f, -2.45f)
                verticalLineTo(10.2f)
                lineTo(18f, 9f)
                verticalLineTo(13.5f)
                arcToRelative(2.5f, 2.5f, 0f, true, true, -2f, -2.45f)
                verticalLineTo(9f)
                close()
            }
            // Note head accents
            path(
                fill = SolidColor(Color(0xFF0EA5E9)),
                stroke = SolidColor(Color.Transparent)
            ) {
                moveTo(14.5f, 17.5f)
                arcToRelative(1.5f, 1.5f, 0f, true, true, -3f, 0f)
                arcToRelative(1.5f, 1.5f, 0f, true, true, 3f, 0f)
            }
            path(
                fill = SolidColor(Color(0xFF0284C7)),
                stroke = SolidColor(Color.Transparent)
            ) {
                moveTo(20.5f, 15.5f)
                arcToRelative(1.5f, 1.5f, 0f, true, true, -3f, 0f)
                arcToRelative(1.5f, 1.5f, 0f, true, true, 3f, 0f)
            }
        }.build()
        return _xShareMusic!!
    }

private var _xShareMusic: ImageVector? = null
