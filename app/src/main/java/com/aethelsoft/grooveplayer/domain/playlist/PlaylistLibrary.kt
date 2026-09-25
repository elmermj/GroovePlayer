package com.aethelsoft.grooveplayer.domain.playlist

/** Private-library copies the M3U matcher can see. Implemented on top of SCRUM-84. */
fun interface PlaylistLibrary {
    suspend fun copies(): List<PlaylistLibrarySong>
}
