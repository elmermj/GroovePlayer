package com.aethelsoft.grooveplayer.domain.library

import com.aethelsoft.grooveplayer.domain.model.LibraryGenre
import com.aethelsoft.grooveplayer.domain.model.Song

/**
 * Groups local songs by genre tags from MediaStore and edited metadata.
 * A missing map entry means the song was never edited. An empty list means the
 * user cleared genre tags, so the MediaStore genre is not used.
 */
object LibraryGenreIndex {

    fun namesFor(
        song: Song,
        editedGenres: List<String>? = null,
    ): List<String> {
        val raw = when {
            editedGenres != null -> editedGenres
            song.genres.any { it.name.isNotBlank() } -> song.genres.map { it.name }
            else -> listOf(song.genre)
        }
        return raw
            .flatMap { splitGenreField(it) }
            .distinctBy { it.lowercase() }
    }

    /**
     * Same resolution browse uses: a map entry replaces MediaStore tags, including
     * an empty list when the user cleared genres.
     */
    fun namesFor(
        song: Song,
        editedGenresBySongId: Map<String, List<String>>,
    ): List<String> = namesFor(song, editedGenres(song.id, editedGenresBySongId))

    fun build(
        songs: List<Song>,
        editedGenresBySongId: Map<String, List<String>> = emptyMap(),
    ): List<LibraryGenre> {
        val groups = linkedMapOf<String, GenreAccumulator>()
        for (song in songs) {
            for (name in namesFor(song, editedGenresBySongId)) {
                val key = name.lowercase()
                val acc = groups.getOrPut(key) { GenreAccumulator(name) }
                acc.songs.add(song)
            }
        }
        return groups.values
            .map { acc ->
                val ordered = sortSongs(acc.songs.distinctBy { it.id })
                LibraryGenre(
                    name = acc.name,
                    trackCount = ordered.size,
                    artworkUrl = ordered.firstNotNullOfOrNull { song ->
                        song.artworkUrl?.takeIf { it.isNotBlank() }
                    },
                )
            }
            .sortedWith(compareBy<LibraryGenre, String>(String.CASE_INSENSITIVE_ORDER) { it.name })
    }

    fun songsIn(
        genreName: String,
        songs: List<Song>,
        editedGenresBySongId: Map<String, List<String>> = emptyMap(),
    ): List<Song> {
        val key = genreName.trim().lowercase()
        if (key.isEmpty()) return emptyList()
        return sortSongs(
            songs.filter { song ->
                namesFor(song, editedGenresBySongId).any { it.lowercase() == key }
            }.distinctBy { it.id }
        )
    }

    private fun editedGenres(
        songId: String,
        editedGenresBySongId: Map<String, List<String>>,
    ): List<String>? =
        if (editedGenresBySongId.containsKey(songId)) {
            editedGenresBySongId.getValue(songId)
        } else {
            null
        }

    private fun splitGenreField(value: String): List<String> =
        value.split(',', ';')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    private fun sortSongs(songs: List<Song>): List<Song> =
        songs.sortedWith(
            compareBy<Song, String>(String.CASE_INSENSITIVE_ORDER) { it.title }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.artist }
        )

    private class GenreAccumulator(val name: String) {
        val songs = mutableListOf<Song>()
    }
}
