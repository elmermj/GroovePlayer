package com.aethelsoft.grooveplayer.utils.theme.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val XRepeatOne: ImageVector
    get() {
        if (_RepeatOne != null) return _RepeatOne!!

        _RepeatOne = ImageVector.Builder(
            name = "Repeat1",
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
                moveToRelative(17f, 2f)
                lineToRelative(4f, 4f)
                lineToRelative(-4f, 4f)
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3f, 11f)
                verticalLineToRelative(-1f)
                arcToRelative(4f, 4f, 0f, false, true, 4f, -4f)
                horizontalLineToRelative(14f)
                moveTo(7f, 22f)
                lineToRelative(-4f, -4f)
                lineToRelative(4f, -4f)
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(21f, 13f)
                verticalLineToRelative(1f)
                arcToRelative(4f, 4f, 0f, false, true, -4f, 4f)
                horizontalLineTo(3f)
                moveToRelative(8f, -8f)
                horizontalLineToRelative(1f)
                verticalLineToRelative(4f)
            }
        }.build()

        return _RepeatOne!!
    }

private var _RepeatOne: ImageVector? = null

