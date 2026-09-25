package com.aethelsoft.grooveplayer.domain.usecase.playlist_category

import com.aethelsoft.grooveplayer.domain.model.M3uImportResult
import com.aethelsoft.grooveplayer.domain.playlist.M3uCodec
import com.aethelsoft.grooveplayer.domain.playlist.M3uLocationHash
import com.aethelsoft.grooveplayer.domain.playlist.PlaylistLibrary
import com.aethelsoft.grooveplayer.domain.playlist.PlaylistM3uMatch
import com.aethelsoft.grooveplayer.domain.playlist.PlaylistNames
import com.aethelsoft.grooveplayer.domain.repository.PlaylistRepository
import javax.inject.Inject

class ImportM3uPlaylistUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val library: PlaylistLibrary,
    private val locationHash: M3uLocationHash,
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

        val copies = library.copies()
        val matched = mutableListOf<com.aethelsoft.grooveplayer.domain.model.Song>()
        val missing = mutableListOf<String>()
        for (track in document.tracks) {
            val hit = PlaylistM3uMatch.match(
                location = track.location,
                readableHash = locationHash.sha256OrNull(track.location),
                library = copies,
            )
            if (hit == null) missing += track.location else matched += hit.song
        }
        if (matched.isEmpty()) {
            return M3uImportResult.Success(
                playlistId = null,
                playlistName = name,
                importedCount = 0,
                missingLocations = missing,
            )
        }
        val id = playlistRepository.create(name)
        playlistRepository.addSongs(id, matched)
        return M3uImportResult.Success(
            playlistId = id,
            playlistName = name,
            importedCount = matched.size,
            missingLocations = missing,
        )
    }
}
