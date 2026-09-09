package com.aethelsoft.grooveplayer.utils.theme.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val XConsolidateFolders: ImageVector
    get() {
        if (_xConsolidateFolders != null) return _xConsolidateFolders!!
        _xConsolidateFolders = ImageVector.Builder(
            name = "XConsolidateFolders",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Left folder
            path(
                fill = SolidColor(Color(0xFF34D399)),
                stroke = SolidColor(Color(0xFF059669)),
                strokeLineWidth = 0.8f,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(2f, 8f)
                curveTo(2f, 7.4477f, 2.4477f, 7f, 3f, 7f)
                horizontalLineTo(6.2f)
                lineTo(7.5f, 8.5f)
                horizontalLineTo(10.5f)
                curveTo(11.0523f, 8.5f, 11.5f, 8.9477f, 11.5f, 9.5f)
                verticalLineTo(15f)
                curveTo(11.5f, 15.5523f, 11.0523f, 16f, 10.5f, 16f)
                horizontalLineTo(3f)
                curveTo(2.4477f, 16f, 2f, 15.5523f, 2f, 15f)
                close()
            }
            // Right folder
            path(
                fill = SolidColor(Color(0xFF2DD4BF)),
                stroke = SolidColor(Color(0xFF0D9488)),
                strokeLineWidth = 0.8f,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12.5f, 9.5f)
                curveTo(12.5f, 8.9477f, 12.9477f, 8.5f, 13.5f, 8.5f)
                horizontalLineTo(16.8f)
                lineTo(18.1f, 10f)
                horizontalLineTo(21f)
                curveTo(21.5523f, 10f, 22f, 10.4477f, 22f, 11f)
                verticalLineTo(16.5f)
                curveTo(22f, 17.0523f, 21.5523f, 17.5f, 21f, 17.5f)
                horizontalLineTo(13.5f)
                curveTo(12.9477f, 17.5f, 12.5f, 17.0523f, 12.5f, 16.5f)
                close()
            }
            // Merge arrows into center
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color(0xFFA7F3D0)),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8f, 19.5f)
                lineTo(12f, 21.5f)
                lineTo(16f, 19.5f)
                moveTo(12f, 17.8f)
                verticalLineTo(21.5f)
            }
        }.build()
        return _xConsolidateFolders!!
    }

private var _xConsolidateFolders: ImageVector? = null
