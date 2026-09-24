package com.aethelsoft.grooveplayer.utils.theme.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Material outlined cloud, 24dp. Used by the premium availability badge. */
val XCloudOutlined: ImageVector
    get() {
        if (_xCloudOutlined != null) return _xCloudOutlined!!
        _xCloudOutlined = ImageVector.Builder(
            name = "XCloudOutlined",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.EvenOdd,
            ) {
                moveTo(19.35f, 10.04f)
                curveTo(18.67f, 6.59f, 15.64f, 4f, 12f, 4f)
                curveTo(9.11f, 4f, 6.6f, 5.64f, 5.35f, 8.04f)
                curveTo(2.34f, 8.36f, 0f, 10.91f, 0f, 14f)
                curveTo(0f, 17.31f, 2.69f, 20f, 6f, 20f)
                horizontalLineTo(19f)
                curveTo(21.76f, 20f, 24f, 17.76f, 24f, 15f)
                curveTo(24f, 12.36f, 21.95f, 10.22f, 19.35f, 10.04f)
                close()
                moveTo(19f, 18f)
                horizontalLineTo(6f)
                curveTo(3.79f, 18f, 2f, 16.21f, 2f, 14f)
                curveTo(2f, 11.79f, 3.79f, 10f, 6f, 10f)
                horizontalLineTo(6.71f)
                curveTo(7.37f, 7.69f, 9.48f, 6f, 12f, 6f)
                curveTo(15.04f, 6f, 17.5f, 8.46f, 17.5f, 11.5f)
                verticalLineTo(12f)
                horizontalLineTo(19f)
                curveTo(20.66f, 12f, 22f, 13.34f, 22f, 15f)
                curveTo(22f, 16.66f, 20.66f, 18f, 19f, 18f)
                close()
            }
        }.build()
        return _xCloudOutlined!!
    }

private var _xCloudOutlined: ImageVector? = null
