package com.aethelsoft.grooveplayer.utils.theme.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val XMiniPlayer: ImageVector
    get() {
        if (_xMiniPlayer != null) return _xMiniPlayer!!
        _xMiniPlayer = ImageVector.Builder(
            name = "XMiniPlayer",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Bar background
            path(
                fill = SolidColor(Color(0xFF6366F1)),
                stroke = SolidColor(Color.Transparent)
            ) {
                moveTo(3f, 14f)
                curveTo(3f, 12.8954f, 3.8954f, 12f, 5f, 12f)
                horizontalLineTo(19f)
                curveTo(20.1046f, 12f, 21f, 12.8954f, 21f, 14f)
                verticalLineTo(18f)
                curveTo(21f, 19.1046f, 20.1046f, 20f, 19f, 20f)
                horizontalLineTo(5f)
                curveTo(3.8954f, 20f, 3f, 19.1046f, 3f, 18f)
                close()
            }
            // Artwork square
            path(
                fill = SolidColor(Color(0xFFA5B4FC)),
                stroke = SolidColor(Color.Transparent)
            ) {
                moveTo(5.5f, 13.5f)
                horizontalLineTo(9.5f)
                verticalLineTo(18.5f)
                horizontalLineTo(5.5f)
                close()
            }
            // Play triangle
            path(
                fill = SolidColor(Color(0xFFE0E7FF)),
                stroke = SolidColor(Color.Transparent)
            ) {
                moveTo(12.5f, 14.2f)
                lineTo(17f, 16f)
                lineTo(12.5f, 17.8f)
                close()
            }
            // Soft top indicator
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color(0xFF818CF8)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(7f, 7f)
                horizontalLineTo(17f)
            }
        }.build()
        return _xMiniPlayer!!
    }

private var _xMiniPlayer: ImageVector? = null
