package com.aethelsoft.grooveplayer.data.mapper

import com.aethelsoft.grooveplayer.data.local.mediastore.model.MediaStoreSongData
import com.aethelsoft.grooveplayer.domain.model.Album
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.domain.model.makeAlbumId
import com.aethelsoft.grooveplayer.utils.ArtistParser

/**
 * Mapper for converting between data layer Song representations and domain Song model.
 */
object SongMapper {
    
    /**
     * Converts MediaStore data model to domain model.
     */
    fun mediaStoreToDomain(data: MediaStoreSongData): Song {
        // Keep the original artist string for display and parsing,
        // but use the primary artist for album IDs/grouping.
        val primaryArtist = ArtistParser.getPrimaryArtist(data.artist)

        return Song(
            id = data.id,
            title = data.title,
            artist = data.artist,
            uri = data.uri,
            genre = data.genre,
            durationMs = data.durationMs,
            artworkUrl = data.artworkUrl,
            album = data.album?.let { albumName ->
                Album(
                    id = makeAlbumId(primaryArtist, albumName),
                    name = albumName,
                    artist = primaryArtist,
                    artworkUrl = data.artworkUrl,
                    songs = emptyList(),
                    year = null
                )
            }
        )
    }
    
    /**
     * Converts list of MediaStore data models to domain models.
     */
    fun mediaStoreToDomainList(dataList: List<MediaStoreSongData>): List<Song> {
        return dataList.map { mediaStoreToDomain(it) }
    }
}
