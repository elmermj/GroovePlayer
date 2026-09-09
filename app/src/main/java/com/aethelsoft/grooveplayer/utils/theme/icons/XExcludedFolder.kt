package com.aethelsoft.grooveplayer.utils.theme.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val XExcludedFolder: ImageVector
    get() {
        if (_xExcludedFolder != null) return _xExcludedFolder!!
        _xExcludedFolder = ImageVector.Builder(
            name = "XExcludedFolder",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Folder body
            path(
                fill = SolidColor(Color(0xFFFBBF24)),
                stroke = SolidColor(Color(0xFFF59E0B)),
                strokeLineWidth = 1f,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3f, 7.5f)
                curveTo(3f, 6.6716f, 3.6716f, 6f, 4.5f, 6f)
                horizontalLineTo(9f)
                lineTo(11f, 8.2f)
                horizontalLineTo(19.5f)
                curveTo(20.3284f, 8.2f, 21f, 8.8716f, 21f, 9.7f)
                verticalLineTo(18f)
                curveTo(21f, 18.8284f, 20.3284f, 19.5f, 19.5f, 19.5f)
                horizontalLineTo(4.5f)
                curveTo(3.6716f, 19.5f, 3f, 18.8284f, 3f, 18f)
                close()
            }
            // Tab accent
            path(
                fill = SolidColor(Color(0xFFFDE68A)),
                stroke = SolidColor(Color.Transparent)
            ) {
                moveTo(4.5f, 6f)
                horizontalLineTo(9f)
                lineTo(10.2f, 7.4f)
                horizontalLineTo(4.5f)
                close()
            }
            // Exclusion X
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color(0xFFEF4444)),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(9.5f, 12f)
                lineTo(14.5f, 17f)
                moveTo(14.5f, 12f)
                lineTo(9.5f, 17f)
            }
        }.build()
        return _xExcludedFolder!!
    }

private var _xExcludedFolder: ImageVector? = null
