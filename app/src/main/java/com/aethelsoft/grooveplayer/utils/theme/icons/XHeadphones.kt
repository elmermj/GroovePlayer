package com.aethelsoft.grooveplayer.utils.theme.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val XHeadphones: ImageVector
    get() {
        if (_headphones != null) return _headphones!!
        _headphones = ImageVector.Builder(
            name = "HeadphonesIcon",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(3f, 14f)
                horizontalLineTo(6f)
                curveTo(7.1046f, 14f, 8f, 14.8954f, 8f, 16f)
                verticalLineTo(19f)
                curveTo(8f, 20.1046f, 7.1046f, 21f, 6f, 21f)
                horizontalLineTo(5f)
                curveTo(3.8954f, 21f, 3f, 20.1046f, 3f, 19f)
                verticalLineTo(12f)
                curveTo(3f, 7.0294f, 7.0294f, 3f, 12f, 3f)
                curveTo(16.9706f, 3f, 21f, 7.0294f, 21f, 12f)
                verticalLineTo(19f)
                curveTo(21f, 20.1046f, 20.1046f, 21f, 19f, 21f)
                horizontalLineTo(18f)
                curveTo(16.8954f, 21f, 16f, 20.1046f, 16f, 19f)
                verticalLineTo(16f)
                curveTo(16f, 14.8954f, 16.8954f, 14f, 18f, 14f)
                horizontalLineTo(21f)
            }
        }.build()
        return _headphones!!
    }

private var _headphones: ImageVector? = null