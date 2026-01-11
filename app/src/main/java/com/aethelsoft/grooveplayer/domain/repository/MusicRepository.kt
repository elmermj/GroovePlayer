package com.aethelsoft.grooveplayer.domain.repository

import com.aethelsoft.grooveplayer.domain.model.Song

interface MusicRepository {
    suspend fun getAllSongs(): List<Song>
    suspend fun getSongsByArtist(artist: String): List<Song>
    suspend fun getSongsByAlbum(album: String): List<Song>
}





