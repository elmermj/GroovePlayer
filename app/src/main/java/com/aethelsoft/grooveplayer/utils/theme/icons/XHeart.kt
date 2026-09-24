package com.aethelsoft.grooveplayer.utils.theme.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val XHeart: ImageVector
    get() {
        if (_Heart != null) return _Heart!!
        _Heart = ImageVector.Builder(
            name = "Heart",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 20.2f)
                curveTo(12f, 20.2f, 4f, 14.8f, 4f, 9.4f)
                curveTo(4f, 6.7f, 6f, 4.8f, 8.5f, 4.8f)
                curveTo(10f, 4.8f, 11.2f, 5.6f, 12f, 6.8f)
                curveTo(12.8f, 5.6f, 14f, 4.8f, 15.5f, 4.8f)
                curveTo(18f, 4.8f, 20f, 6.7f, 20f, 9.4f)
                curveTo(20f, 14.8f, 12f, 20.2f, 12f, 20.2f)
                close()
            }
        }.build()
        return _Heart!!
    }

private var _Heart: ImageVector? = null

val XHeartFilled: ImageVector
    get() {
        if (_HeartFilled != null) return _HeartFilled!!
        _HeartFilled = ImageVector.Builder(
            name = "HeartFilled",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 20.2f)
                curveTo(12f, 20.2f, 4f, 14.8f, 4f, 9.4f)
                curveTo(4f, 6.7f, 6f, 4.8f, 8.5f, 4.8f)
                curveTo(10f, 4.8f, 11.2f, 5.6f, 12f, 6.8f)
                curveTo(12.8f, 5.6f, 14f, 4.8f, 15.5f, 4.8f)
                curveTo(18f, 4.8f, 20f, 6.7f, 20f, 9.4f)
                curveTo(20f, 14.8f, 12f, 20.2f, 12f, 20.2f)
                close()
            }
        }.build()
        return _HeartFilled!!
    }

private var _HeartFilled: ImageVector? = null
