package com.aethelsoft.grooveplayer.utils.theme.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val XAppVersion: ImageVector
    get() {
        if (_xAppVersion != null) return _xAppVersion!!
        _xAppVersion = ImageVector.Builder(
            name = "XAppVersion",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Tag body
            path(
                fill = SolidColor(Color(0xFF60A5FA)),
                stroke = SolidColor(Color(0xFF2563EB)),
                strokeLineWidth = 1f,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3.5f, 11.5f)
                lineTo(11.2f, 3.8f)
                curveTo(11.6f, 3.4f, 12.15f, 3.2f, 12.7f, 3.2f)
                horizontalLineTo(19.5f)
                curveTo(20.3284f, 3.2f, 21f, 3.8716f, 21f, 4.7f)
                verticalLineTo(11.5f)
                curveTo(21f, 12.05f, 20.8f, 12.6f, 20.4f, 13f)
                lineTo(12.7f, 20.7f)
                curveTo(12.1f, 21.3f, 11.1f, 21.3f, 10.5f, 20.7f)
                lineTo(3.5f, 13.7f)
                curveTo(2.9f, 13.1f, 2.9f, 12.1f, 3.5f, 11.5f)
                close()
            }
            // Hole
            path(
                fill = SolidColor(Color(0xFFDBEAFE)),
                stroke = SolidColor(Color.Transparent)
            ) {
                moveTo(16.5f, 8f)
                arcToRelative(1.5f, 1.5f, 0f, true, true, 0f, 0.01f)
                close()
            }
            // Version mark
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color(0xFFEFF6FF)),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8.2f, 13.2f)
                lineTo(10.5f, 15.5f)
                lineTo(14.8f, 10.8f)
            }
        }.build()
        return _xAppVersion!!
    }

private var _xAppVersion: ImageVector? = null
