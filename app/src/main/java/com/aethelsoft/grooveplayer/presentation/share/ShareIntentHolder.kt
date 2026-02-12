package com.aethelsoft.grooveplayer.presentation.share

import com.aethelsoft.grooveplayer.domain.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds songs to share when navigating to the Share flow.
 * Caller sets songs before navigating to share screen.
 */
object ShareIntentHolder {
    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    fun setSongs(songs: List<Song>) {
        _songs.value = songs
    }

    fun clear() {
        _songs.value = emptyList()
    }
}
