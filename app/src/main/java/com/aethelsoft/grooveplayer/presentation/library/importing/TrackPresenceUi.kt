package com.aethelsoft.grooveplayer.presentation.library.importing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.aethelsoft.grooveplayer.data.library.TrackPresenceRepository
import com.aethelsoft.grooveplayer.domain.library.TrackPresence
import com.aethelsoft.grooveplayer.domain.model.Song
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@Composable
fun rememberTrackPresence(song: Song): TrackPresence {
    val context = LocalContext.current
    val repository = remember(context) {
        runCatching {
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                TrackPresenceEntryPoint::class.java,
            ).trackPresenceRepository()
        }.getOrNull()
    }
    var presence by remember(song.id, song.filePath) {
        mutableStateOf(TrackPresence(playable = true, canRestore = false))
    }
    LaunchedEffect(repository, song.id, song.uri, song.filePath, song.fileSizeBytes) {
        val resolved = repository?.presence(song)
        if (resolved != null) presence = resolved
    }
    return presence
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface TrackPresenceEntryPoint {
    fun trackPresenceRepository(): TrackPresenceRepository
}
