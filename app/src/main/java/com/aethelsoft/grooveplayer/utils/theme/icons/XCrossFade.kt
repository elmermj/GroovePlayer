package com.aethelsoft.grooveplayer.utils.theme.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val XCrossFade: ImageVector
    get() {
        if (_xCrossFade != null) return _xCrossFade!!
        _xCrossFade = ImageVector.Builder(
            name = "XCrossFade",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Left wave fading out
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color(0xFFFB923C)),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(2f, 12f)
                curveTo(3.5f, 8f, 5f, 8f, 6.5f, 12f)
                curveTo(8f, 16f, 9.5f, 16f, 11f, 12f)
            }
            // Right wave fading in
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color(0xFFF472B6)),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(13f, 12f)
                curveTo(14.5f, 8f, 16f, 8f, 17.5f, 12f)
                curveTo(19f, 16f, 20.5f, 16f, 22f, 12f)
            }
            // Overlap highlight
            path(
                fill = SolidColor(Color(0xFFFBBF24)),
                stroke = SolidColor(Color.Transparent)
            ) {
                moveTo(12f, 12f)
                arcToRelative(1.6f, 1.6f, 0f, true, true, 0f, 0.01f)
                close()
            }
            // Soft fade ticks
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color(0xFFFDBA74)),
                strokeLineWidth = 1.6f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(9.5f, 5.5f)
                lineTo(9.5f, 7.5f)
                moveTo(14.5f, 16.5f)
                lineTo(14.5f, 18.5f)
            }
        }.build()
        return _xCrossFade!!
    }

private var _xCrossFade: ImageVector? = null
