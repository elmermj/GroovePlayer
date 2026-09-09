package com.aethelsoft.grooveplayer.utils.theme.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val XPrivacyPolicy: ImageVector
    get() {
        if (_xPrivacyPolicy != null) return _xPrivacyPolicy!!
        _xPrivacyPolicy = ImageVector.Builder(
            name = "XPrivacyPolicy",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Shield
            path(
                fill = SolidColor(Color(0xFF34D399)),
                stroke = SolidColor(Color(0xFF059669)),
                strokeLineWidth = 1f,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 2.5f)
                lineTo(20f, 6f)
                verticalLineTo(11.5f)
                curveTo(20f, 16.2f, 16.8f, 20.2f, 12f, 21.5f)
                curveTo(7.2f, 20.2f, 4f, 16.2f, 4f, 11.5f)
                verticalLineTo(6f)
                close()
            }
            // Inner panel
            path(
                fill = SolidColor(Color(0xFFA7F3D0)),
                stroke = SolidColor(Color.Transparent)
            ) {
                moveTo(12f, 5f)
                lineTo(17.5f, 7.4f)
                verticalLineTo(11.3f)
                curveTo(17.5f, 14.7f, 15.1f, 17.7f, 12f, 18.8f)
                curveTo(8.9f, 17.7f, 6.5f, 14.7f, 6.5f, 11.3f)
                verticalLineTo(7.4f)
                close()
            }
            // Lock body
            path(
                fill = SolidColor(Color(0xFF065F46)),
                stroke = SolidColor(Color.Transparent)
            ) {
                moveTo(9.5f, 12f)
                horizontalLineTo(14.5f)
                verticalLineTo(15.5f)
                curveTo(14.5f, 15.7761f, 14.2761f, 16f, 14f, 16f)
                horizontalLineTo(10f)
                curveTo(9.7239f, 16f, 9.5f, 15.7761f, 9.5f, 15.5f)
                close()
            }
            // Lock shackle
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color(0xFF065F46)),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(10.5f, 12f)
                verticalLineTo(10.5f)
                curveTo(10.5f, 9.6716f, 11.1716f, 9f, 12f, 9f)
                curveTo(12.8284f, 9f, 13.5f, 9.6716f, 13.5f, 10.5f)
                verticalLineTo(12f)
            }
        }.build()
        return _xPrivacyPolicy!!
    }

private var _xPrivacyPolicy: ImageVector? = null
