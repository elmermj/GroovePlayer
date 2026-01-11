package com.aethelsoft.grooveplayer.utils.theme.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val XShuffle: ImageVector
    get() {
        if (_Shuffle != null) return _Shuffle!!

        _Shuffle = ImageVector.Builder(
            name = "Shuffle",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(2f, 18f)
                horizontalLineToRelative(1.4f)
                curveToRelative(1.3f, 0f, 2.5f, -0.6f, 3.3f, -1.7f)
                lineToRelative(6.1f, -8.6f)
                curveToRelative(0.7f, -1.1f, 2f, -1.7f, 3.3f, -1.7f)
                horizontalLineTo(22f)
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveToRelative(18f, 2f)
                lineToRelative(4f, 4f)
                lineToRelative(-4f, 4f)
                moveTo(2f, 6f)
                horizontalLineToRelative(1.9f)
                curveToRelative(1.5f, 0f, 2.9f, 0.9f, 3.6f, 2.2f)
                moveTo(22f, 18f)
                horizontalLineToRelative(-5.9f)
                curveToRelative(-1.3f, 0f, -2.6f, -0.7f, -3.3f, -1.8f)
                lineToRelative(-0.5f, -0.8f)
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveToRelative(18f, 14f)
                lineToRelative(4f, 4f)
                lineToRelative(-4f, 4f)
            }
        }.build()

        return _Shuffle!!
    }

private var _Shuffle: ImageVector? = null

