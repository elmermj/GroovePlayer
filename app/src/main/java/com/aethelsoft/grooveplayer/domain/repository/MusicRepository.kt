package com.aethelsoft.grooveplayer.domain.repository

import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.domain.model.StorageUsageData

interface MusicRepository {
    suspend fun getAllSongs(): List<Song>
    /** Loads a page of songs for All Songs screen. */
    suspend fun getSongsPage(offset: Int, limit: Int): List<Song>
    suspend fun getSongsByArtist(artist: String): List<Song>
    /** Loads a page of songs for Artist detail screen. */
    suspend fun getSongsByArtistPage(artist: String, offset: Int, limit: Int): List<Song>
    suspend fun getSongsByAlbum(album: String): List<Song>
    /** Loads a page of songs for Album detail screen. */
    suspend fun getSongsByAlbumPage(album: String, offset: Int, limit: Int): List<Song>
    suspend fun searchSongs(query: String): List<Song>
    /**
     * Returns distinct folder paths that contain music (from MediaStore DATA).
     * Used for excluded-folder suggestions.
     */
    suspend fun getMusicFolderPaths(): List<String>
    /**
     * Returns storage usage breakdown for music: included vs excluded folders.
     */
    suspend fun getStorageUsage(): StorageUsageData
}





