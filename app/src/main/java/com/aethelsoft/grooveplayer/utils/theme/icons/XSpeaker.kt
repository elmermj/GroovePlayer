package com.aethelsoft.grooveplayer.utils.theme.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val XSpeaker: ImageVector
    get() {
        if (_XSpeaker != null) return _XSpeaker!!
        _XSpeaker = ImageVector.Builder(
            name = "SpeakerIcon",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {

            // Outer rect
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(6f, 2f)
                horizontalLineTo(18f)
                curveTo(19.1046f, 2f, 20f, 2.8954f, 20f, 4f)
                verticalLineTo(20f)
                curveTo(20f, 21.1046f, 19.1046f, 22f, 18f, 22f)
                horizontalLineTo(6f)
                curveTo(4.8954f, 22f, 4f, 21.1046f, 4f, 20f)
                verticalLineTo(4f)
                curveTo(4f, 2.8954f, 4.8954f, 2f, 6f, 2f)
                close()
            }

            // Top small dot
            path(
                fill = SolidColor(Color.Black),
                stroke = null
            ) {
                moveTo(12f, 6f)
                lineTo(12.01f, 6f)
            }

            // Big circle using arcs
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f
            ) {
                moveTo(16f, 14f)
                arcToRelative(4f, 4f, 0f, false, true, -8f, 0f)
                arcToRelative(4f, 4f, 0f, false, true, 8f, 0f)
            }

            // Center dot
            path(
                fill = SolidColor(Color.Black),
                stroke = null
            ) {
                moveTo(12f, 14f)
                lineTo(12.01f, 14f)
            }

        }.build()
        return _XSpeaker!!
    }

private var _XSpeaker: ImageVector? = null