package com.aethelsoft.grooveplayer.domain.usecase.playlist_category

import com.aethelsoft.grooveplayer.domain.model.M3uImportResult
import com.aethelsoft.grooveplayer.domain.playlist.M3uCodec
import com.aethelsoft.grooveplayer.domain.playlist.PlaylistLibraryPaths
import com.aethelsoft.grooveplayer.domain.playlist.PlaylistNames
import com.aethelsoft.grooveplayer.domain.repository.MusicRepository
import com.aethelsoft.grooveplayer.domain.repository.PlaylistRepository
import javax.inject.Inject

class ImportM3uPlaylistUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val musicRepository: MusicRepository,
) {
    suspend operator fun invoke(requestedName: String, m3uText: String): M3uImportResult {
        val document = M3uCodec.parse(m3uText)
        if (document.tracks.isEmpty()) {
            return M3uImportResult.Failure("This M3U file has no tracks")
        }
        val name = PlaylistNames.sanitize(requestedName).ifBlank {
            PlaylistNames.sanitize(document.name.orEmpty())
        }.ifBlank { "Imported playlist" }
        val nameError = PlaylistNames.validationError(name)
        if (nameError != null) return M3uImportResult.Failure(nameError)

        val match = PlaylistLibraryPaths.match(document.tracks, musicRepository.getAllSongs())
        if (match.matched.isEmpty()) {
            return M3uImportResult.Failure(
                "None of the ${document.tracks.size} tracks matched your library",
            )
        }
        val id = playlistRepository.create(name)
        playlistRepository.addSongs(id, match.matched)
        return M3uImportResult.Success(
            playlistId = id,
            playlistName = name,
            importedCount = match.matched.size,
            missingLocations = match.missingLocations,
        )
    }
}
