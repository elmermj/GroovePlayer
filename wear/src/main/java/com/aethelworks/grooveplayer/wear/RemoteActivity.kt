package com.aethelworks.grooveplayer.wear

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.core.view.WindowCompat
import androidx.wear.compose.material3.MaterialTheme
import com.aethelworks.grooveplayer.wear.data.WatchSession
import com.aethelworks.grooveplayer.wear.ui.RemoteScreen

class RemoteActivity : ComponentActivity() {
    private val model: RemoteViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val state by model.uiState.collectAsState()
            val jpeg by model.artwork.collectAsState()
            val artwork = remember(jpeg) { jpeg?.let(::jpegPainter) }
            MaterialTheme {
                RemoteScreen(
                    state = state,
                    artwork = artwork,
                    onPlayPause = model::playPause,
                    onNext = model::next,
                    onPrevious = model::previous,
                    onSeek = model::seekTo,
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        WatchSession.onForeground(this)
    }
}

private fun jpegPainter(bytes: ByteArray): Painter? {
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
    return BitmapPainter(bitmap.asImageBitmap())
}
