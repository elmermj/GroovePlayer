package com.aethelsoft.grooveplayer.utils.theme.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val XEqualizer: ImageVector
    get() {
        if (_xEqualizer != null) return _xEqualizer!!
        _xEqualizer = ImageVector.Builder(
            name = "XEqualizer",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Left slider track
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color(0xFF38BDF8)),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(6f, 4f)
                verticalLineTo(20f)
            }
            // Mid slider track
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color(0xFFA78BFA)),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(12f, 4f)
                verticalLineTo(20f)
            }
            // Right slider track
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color(0xFFF472B6)),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(18f, 4f)
                verticalLineTo(20f)
            }
            // Knobs
            path(
                fill = SolidColor(Color(0xFF7DD3FC)),
                stroke = SolidColor(Color.Transparent)
            ) {
                moveTo(6f, 9f)
                arcToRelative(1.8f, 1.8f, 0f, true, true, 0f, 0.01f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFFC4B5FD)),
                stroke = SolidColor(Color.Transparent)
            ) {
                moveTo(12f, 15f)
                arcToRelative(1.8f, 1.8f, 0f, true, true, 0f, 0.01f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFFF9A8D4)),
                stroke = SolidColor(Color.Transparent)
            ) {
                moveTo(18f, 11f)
                arcToRelative(1.8f, 1.8f, 0f, true, true, 0f, 0.01f)
                close()
            }
        }.build()
        return _xEqualizer!!
    }

private var _xEqualizer: ImageVector? = null
