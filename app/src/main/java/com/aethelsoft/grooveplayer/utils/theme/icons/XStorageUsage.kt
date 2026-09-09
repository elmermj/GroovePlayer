package com.aethelsoft.grooveplayer.utils.theme.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val XStorageUsage: ImageVector
    get() {
        if (_xStorageUsage != null) return _xStorageUsage!!
        _xStorageUsage = ImageVector.Builder(
            name = "XStorageUsage",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Drive body
            path(
                fill = SolidColor(Color(0xFF38BDF8)),
                stroke = SolidColor(Color(0xFF0284C7)),
                strokeLineWidth = 1f,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4f, 8f)
                curveTo(4f, 6.8954f, 4.8954f, 6f, 6f, 6f)
                horizontalLineTo(18f)
                curveTo(19.1046f, 6f, 20f, 6.8954f, 20f, 8f)
                verticalLineTo(16f)
                curveTo(20f, 17.1046f, 19.1046f, 18f, 18f, 18f)
                horizontalLineTo(6f)
                curveTo(4.8954f, 18f, 4f, 17.1046f, 4f, 16f)
                close()
            }
            // Usage segment
            path(
                fill = SolidColor(Color(0xFF22D3EE)),
                stroke = SolidColor(Color.Transparent)
            ) {
                moveTo(5.5f, 13.5f)
                horizontalLineTo(13.5f)
                verticalLineTo(15.5f)
                horizontalLineTo(5.5f)
                close()
            }
            // Free segment
            path(
                fill = SolidColor(Color(0xFF0C4A6E)),
                stroke = SolidColor(Color.Transparent)
            ) {
                moveTo(14f, 13.5f)
                horizontalLineTo(18.5f)
                verticalLineTo(15.5f)
                horizontalLineTo(14f)
                close()
            }
            // Activity LED
            path(
                fill = SolidColor(Color(0xFF4ADE80)),
                stroke = SolidColor(Color.Transparent)
            ) {
                moveTo(17.5f, 9.5f)
                arcToRelative(1f, 1f, 0f, true, true, 0f, 0.01f)
                close()
            }
        }.build()
        return _xStorageUsage!!
    }

private var _xStorageUsage: ImageVector? = null
