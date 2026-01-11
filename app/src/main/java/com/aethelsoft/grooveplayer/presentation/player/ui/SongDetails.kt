package com.aethelsoft.grooveplayer.presentation.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.aethelsoft.grooveplayer.domain.model.Song

@Composable
fun SongDetails(
    song: Song?,
) {

    val scrimBrush = createBackgroundScrimBrush()
    Column(
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .wrapContentSize()
                .background(
                    brush = scrimBrush
                )
        ){
            Text(
                text = song?.title ?: "",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Text(
            text = song?.artist ?: "",
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun createBackgroundScrimBrush(): Brush {

    return Brush.sweepGradient(
        colors = listOf(
            Color.Black.copy(alpha = 0.8f),
            Color.Black.copy(alpha = 0.8f),
            Color.Black.copy(alpha = 0.8f),
            Color.Black.copy(alpha = 0.8f),
            Color.Black.copy(alpha = 0.8f),
            Color.Transparent
        )
    )
}