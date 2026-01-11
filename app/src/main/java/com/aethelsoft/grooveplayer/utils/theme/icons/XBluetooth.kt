package com.aethelsoft.grooveplayer.utils.theme.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val XBluetooth: ImageVector
    get() {
        if (_XBluetooth != null) return _XBluetooth!!

        _XBluetooth = ImageVector.Builder(
            name = "bluetooth_category",
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
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(7f, 7f)
                lineToRelative(10f, 10f)
                lineToRelative(-5f, 5f)
                verticalLineTo(2f)
                lineToRelative(5f, 5f)
                lineTo(7f, 17f)
            }
        }.build()

        return _XBluetooth!!
    }

private var _XBluetooth: ImageVector? = null

