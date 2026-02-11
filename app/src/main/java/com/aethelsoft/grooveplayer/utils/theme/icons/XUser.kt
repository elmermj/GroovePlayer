package com.aethelsoft.grooveplayer.utils.theme.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val XUser: ImageVector
    get() {
        if (_xUser != null) return _xUser!!
        _xUser = ImageVector.Builder(
            name = "XUser",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {

            // Body: M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(19f, 21f)
                verticalLineTo(19f)
                curveTo(19f, 16.7909f, 17.2091f, 15f, 15f, 15f)
                horizontalLineTo(9f)
                curveTo(6.7909f, 15f, 5f, 16.7909f, 5f, 19f)
                verticalLineTo(21f)
            }

            // Head circle: cx=12 cy=7 r=4
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f
            ) {
                moveTo(16f, 7f)
                arcToRelative(4f, 4f, 0f, true, true, -8f, 0f)
                arcToRelative(4f, 4f, 0f, true, true, 8f, 0f)
            }

        }.build()
        return _xUser!!
    }

private var _xUser: ImageVector? = null