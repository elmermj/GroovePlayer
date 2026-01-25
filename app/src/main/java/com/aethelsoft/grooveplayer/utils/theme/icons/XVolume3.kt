package com.aethelsoft.grooveplayer.utils.theme.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val XVolume3: ImageVector
    get() {
        if (_XVolume3 != null) return _XVolume3!!

        _XVolume3 = ImageVector.Builder(
            name = "volume-3",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {

            // Speaker body
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(11f, 4.702f)
                arcToRelative(0.705f, 0.705f, 0f, false, false, -1.203f, -0.498f)
                lineTo(6.413f, 7.587f)
                arcTo(1.4f, 1.4f, 0f, false, true, 5.416f, 8f)
                horizontalLineTo(3f)
                arcToRelative(1f, 1f, 0f, false, false, -1f, 1f)
                verticalLineToRelative(6f)
                arcToRelative(1f, 1f, 0f, false, false, 1f, 1f)
                horizontalLineToRelative(2.416f)
                arcToRelative(1.4f, 1.4f, 0f, false, true, 0.997f, 0.413f)
                lineToRelative(3.383f, 3.384f)
                arcTo(0.705f, 0.705f, 0f, false, false, 11f, 19.298f)
                close()
            }

            // Inner wave
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(16f, 9f)
                arcToRelative(5f, 5f, 0f, false, true, 0f, 6f)
            }

            // Middle wave
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(19.364f, 18.364f)
                arcToRelative(9f, 9f, 0f, false, false, 0f, -12.728f)
            }

            // OUTER wave (third curve)
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // Start near bottom-right
                moveTo(22f, 21f)

                // Large arc upward (relative)
                arcToRelative(
                    12f,   // a = horizontal radius
                    12f,   // b = vertical radius
                    0f,    // theta
                    true,  // isMoreThanHalf (large arc)
                    false, // sweep direction (matches Lucide)
                    0f,    // dx (22 -> 22)
                    -18f   // dy (21 -> 3)
                )
            }
        }.build()

        return _XVolume3!!
    }

private var _XVolume3: ImageVector? = null