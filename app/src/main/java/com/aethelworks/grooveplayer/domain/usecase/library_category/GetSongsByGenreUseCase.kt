package com.aethelworks.grooveplayer.domain.usecase.library_category

import com.aethelworks.grooveplayer.domain.library.LibraryGenreIndex
import com.aethelworks.grooveplayer.domain.model.Song
import com.aethelworks.grooveplayer.domain.repository.MusicRepository
import com.aethelworks.grooveplayer.domain.repository.SongMetadataRepository
import javax.inject.Inject

class GetSongsByGenreUseCase @Inject constructor(
    private val musicRepository: MusicRepository,
    private val songMetadataRepository: SongMetadataRepository,
) {
    suspend operator fun invoke(genreName: String): List<Song> {
        val songs = musicRepository.getAllSongs()
        val edited = songMetadataRepository.getAllMetadata()
            .associate { it.songId to it.genres }
        return LibraryGenreIndex.songsIn(genreName, songs, edited)
    }
}
