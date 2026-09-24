package com.aethelsoft.grooveplayer.utils.theme.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Drag handle (two columns of three dots), Lucide "grip-vertical". */
val XGripVertical: ImageVector
    get() {
        if (_XGripVertical != null) return _XGripVertical!!

        _XGripVertical = ImageVector.Builder(
            name = "grip_vertical",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            listOf(9f, 15f).forEach { x ->
                listOf(5f, 12f, 19f).forEach { y ->
                    path(fill = SolidColor(Color.Black)) {
                        moveTo(x - 1.5f, y)
                        arcTo(1.5f, 1.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = x + 1.5f, y1 = y)
                        arcTo(1.5f, 1.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = x - 1.5f, y1 = y)
                        close()
                    }
                }
            }
        }.build()

        return _XGripVertical!!
    }

private var _XGripVertical: ImageVector? = null
