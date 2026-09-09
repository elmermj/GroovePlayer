package com.aethelsoft.grooveplayer.utils.theme.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Palette / appearance icon for Profile → UI styling.
 * Multi-color strokes so [ProfileRowIcon] (tint Unspecified) stays visible on black.
 */
val XUiStyle: ImageVector
    get() {
        if (_xUiStyle != null) return _xUiStyle!!
        _xUiStyle = ImageVector.Builder(
            name = "XUiStyle",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Swatch / drop
            path(
                fill = SolidColor(Color(0xFFA78BFA)),
                stroke = SolidColor(Color(0xFFA78BFA)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(12f, 2.5f)
                curveTo(12f, 2.5f, 5.5f, 9.2f, 5.5f, 13.5f)
                curveTo(5.5f, 17.1f, 8.4f, 20f, 12f, 20f)
                curveTo(15.6f, 20f, 18.5f, 17.1f, 18.5f, 13.5f)
                curveTo(18.5f, 9.2f, 12f, 2.5f, 12f, 2.5f)
                close()
            }
            // Inner highlight
            path(
                fill = SolidColor(Color(0xFF38BDF8)),
                stroke = SolidColor(Color.Transparent),
                strokeLineWidth = 0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(10.2f, 11.5f)
                curveTo(10.2f, 10.3f, 11.1f, 9.2f, 12.2f, 8.5f)
                curveTo(11.1f, 10.1f, 10.5f, 11.6f, 10.5f, 13.2f)
                curveTo(10.5f, 14.5f, 11.3f, 15.6f, 12.4f, 16f)
                curveTo(11.1f, 15.5f, 10.2f, 14.3f, 10.2f, 12.8f)
                close()
            }
            // Accent bar (type / layout hint)
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color(0xFFDBDBDB)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(7f, 22f)
                horizontalLineTo(17f)
            }
        }.build()
        return _xUiStyle!!
    }

private var _xUiStyle: ImageVector? = null
