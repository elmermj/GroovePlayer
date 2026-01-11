package com.aethelsoft.grooveplayer.data.mapper

import com.aethelsoft.grooveplayer.data.local.mediastore.model.MediaStoreSongData
import com.aethelsoft.grooveplayer.domain.model.Song

/**
 * Mapper for converting between data layer Song representations and domain Song model.
 */
object SongMapper {
    
    /**
     * Converts MediaStore data model to domain model.
     */
    fun mediaStoreToDomain(data: MediaStoreSongData): Song {
        return Song(
            id = data.id,
            title = data.title,
            artist = data.artist,
            uri = data.uri,
            genre = data.genre,
            durationMs = data.durationMs,
            artworkUrl = data.artworkUrl,
            album = data.album
        )
    }
    
    /**
     * Converts list of MediaStore data models to domain models.
     */
    fun mediaStoreToDomainList(dataList: List<MediaStoreSongData>): List<Song> {
        return dataList.map { mediaStoreToDomain(it) }
    }
}
