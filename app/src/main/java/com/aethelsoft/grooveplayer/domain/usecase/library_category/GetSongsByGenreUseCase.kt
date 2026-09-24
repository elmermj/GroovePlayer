package com.aethelsoft.grooveplayer.domain.usecase.library_category

import com.aethelsoft.grooveplayer.domain.library.LibraryGenreIndex
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.domain.repository.MusicRepository
import com.aethelsoft.grooveplayer.domain.repository.SongMetadataRepository
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
