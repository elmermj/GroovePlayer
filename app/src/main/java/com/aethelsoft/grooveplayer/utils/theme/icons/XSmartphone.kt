package com.aethelsoft.grooveplayer.utils.theme.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val XSmartphone: ImageVector
    get() {
        if (_XSmartphone != null) return _XSmartphone!!
        _XSmartphone = ImageVector.Builder(
            name = "SmartphoneIcon",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {

            // Outer rounded rect
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(7f, 2f)
                horizontalLineTo(17f)
                curveTo(18.1046f, 2f, 19f, 2.8954f, 19f, 4f)
                verticalLineTo(20f)
                curveTo(19f, 21.1046f, 18.1046f, 22f, 17f, 22f)
                horizontalLineTo(7f)
                curveTo(5.8954f, 22f, 5f, 21.1046f, 5f, 20f)
                verticalLineTo(4f)
                curveTo(5f, 2.8954f, 5.8954f, 2f, 7f, 2f)
                close()
            }

            // Bottom dot (M12 18 h .01)
            path(
                fill = SolidColor(Color.Black),
                stroke = null
            ) {
                moveTo(12f, 18f)
                lineTo(12.01f, 18f)
            }

        }.build()
        return _XSmartphone!!
    }

private var _XSmartphone: ImageVector? = null