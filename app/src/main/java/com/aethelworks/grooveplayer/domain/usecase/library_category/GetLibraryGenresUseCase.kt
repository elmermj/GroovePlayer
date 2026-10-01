package com.aethelworks.grooveplayer.domain.usecase.library_category

import com.aethelworks.grooveplayer.domain.library.LibraryGenreIndex
import com.aethelworks.grooveplayer.domain.model.LibraryGenre
import com.aethelworks.grooveplayer.domain.repository.MusicRepository
import com.aethelworks.grooveplayer.domain.repository.SongMetadataRepository
import javax.inject.Inject

class GetLibraryGenresUseCase @Inject constructor(
    private val musicRepository: MusicRepository,
    private val songMetadataRepository: SongMetadataRepository,
) {
    suspend operator fun invoke(): List<LibraryGenre> {
        val songs = musicRepository.getAllSongs()
        val edited = songMetadataRepository.getAllMetadata()
            .associate { it.songId to it.genres }
        return LibraryGenreIndex.build(songs, edited)
    }
}
