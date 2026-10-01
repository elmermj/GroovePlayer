package com.aethelworks.grooveplayer.wear.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.ui.tooling.preview.WearPreviewLargeRound
import androidx.wear.compose.ui.tooling.preview.WearPreviewSmallRound
import androidx.wear.compose.ui.tooling.preview.WearPreviewSquare
import androidx.wear.tooling.preview.devices.WearDevices
import com.aethelworks.grooveplayer.wear.logic.WatchSnapshot
import com.aethelworks.grooveplayer.wear.logic.remoteUi

private const val SAMPLE_UPDATED_AT = 1_700_000_000_000L

private fun playingSnapshot() = WatchSnapshot(
    title = "Midnight Radio",
    artist = "The Northern Line",
    isPlaying = true,
    positionMs = 83_000L,
    durationMs = 214_000L,
    updatedAtMs = SAMPLE_UPDATED_AT,
)

private fun pausedSnapshot() = playingSnapshot().copy(
    isPlaying = false,
    positionMs = 42_000L,
)

private val sampleArtwork = object : Painter() {
    override val intrinsicSize: Size = Size.Unspecified

    override fun DrawScope.onDraw() {
        drawRect(
            brush = Brush.linearGradient(
                listOf(
                    Color(0xFF5B2A86),
                    Color(0xFFE07A3D),
                    Color(0xFF1B3A4B),
                ),
            ),
        )
    }
}

@Composable
private fun PreviewRemote(
    snapshot: WatchSnapshot?,
    connected: Boolean,
    artwork: Painter?,
) {
    MaterialTheme {
        RemoteScreen(
            state = remoteUi(snapshot, connected, snapshot?.updatedAtMs ?: 0L),
            artwork = artwork,
            onPlayPause = {},
            onNext = {},
            onPrevious = {},
            onSeek = {},
        )
    }
}

@WearPreviewSmallRound
@Composable
private fun SmallRoundPlaying() = PreviewRemote(playingSnapshot(), connected = true, artwork = sampleArtwork)

@WearPreviewLargeRound
@Composable
private fun LargeRoundPlaying() = PreviewRemote(playingSnapshot(), connected = true, artwork = sampleArtwork)

@WearPreviewSquare
@Composable
private fun SquarePlaying() = PreviewRemote(playingSnapshot(), connected = true, artwork = sampleArtwork)

@WearPreviewSmallRound
@Composable
private fun SmallRoundPaused() = PreviewRemote(pausedSnapshot(), connected = true, artwork = sampleArtwork)

@WearPreviewLargeRound
@Composable
private fun LargeRoundPaused() = PreviewRemote(pausedSnapshot(), connected = true, artwork = sampleArtwork)

@WearPreviewSquare
@Composable
private fun SquarePaused() = PreviewRemote(pausedSnapshot(), connected = true, artwork = sampleArtwork)

@WearPreviewSmallRound
@Composable
private fun SmallRoundDisconnected() = PreviewRemote(playingSnapshot(), connected = false, artwork = null)

@WearPreviewLargeRound
@Composable
private fun LargeRoundDisconnected() = PreviewRemote(playingSnapshot(), connected = false, artwork = null)

@WearPreviewSquare
@Composable
private fun SquareDisconnected() = PreviewRemote(playingSnapshot(), connected = false, artwork = null)

@Preview(
    device = WearDevices.RECT,
    showSystemUi = true,
    backgroundColor = 0xFF000000,
    showBackground = true,
    group = "Devices - Rect",
    name = "Rect playing",
)
@Composable
private fun RectPlaying() = PreviewRemote(playingSnapshot(), connected = true, artwork = sampleArtwork)
