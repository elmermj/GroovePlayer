package com.aethelworks.grooveplayer.domain.auto

import com.aethelworks.grooveplayer.domain.library.PrivateLibrarySongs
import com.aethelworks.grooveplayer.domain.model.FavoriteAlbum
import com.aethelworks.grooveplayer.domain.model.FavoriteArtist
import com.aethelworks.grooveplayer.domain.model.MostPlayedTrack
import com.aethelworks.grooveplayer.domain.model.PlaylistWithTracks
import com.aethelworks.grooveplayer.domain.model.Song
import com.aethelworks.grooveplayer.domain.model.likedTrackLabel
import com.aethelworks.grooveplayer.domain.model.playCountLabel
import com.aethelworks.grooveplayer.domain.model.playlistCountLabel
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Android Auto browse tree for the private library.
 *
 * Callers pass songs, SCRUM-9 playlists, playback history, and SCRUM-68 likes.
 * This object does not read MediaStore or the device filesystem.
 * A play request is the song list the existing player should resolve, including
 * tracks whose local file is already gone, so local-first / Premium streaming
 * stays on the phone's playback path.
 */
object AndroidAutoBrowse {
    const val ROOT = "root"
    const val IMPORT_TITLE = "Import music on your phone"

    const val RECENT_TITLE = "Recently played"
    const val MOST_TITLE = "Most played"
    const val PLAYLISTS_TITLE = "Playlists"
    const val ARTISTS_TITLE = "Artists"
    const val ALBUMS_TITLE = "Albums"
    const val SONGS_TITLE = "Songs"
    const val FAVORITES_TITLE = "Favorites"
    const val TRACKS_TITLE = "Tracks"

    private const val RECENT = "recent"
    private const val MOST = "most"
    private const val PLAYLISTS = "playlists"
    private const val ARTISTS = "artists"
    private const val ALBUMS = "albums"
    private const val SONGS = "songs"
    private const val FAVORITES = "favorites"
    private const val FAVORITE_TRACKS = "favorites/tracks"
    private const val FAVORITE_ARTISTS = "favorites/artists"
    private const val FAVORITE_ALBUMS = "favorites/albums"
    private const val IMPORT = "import"
    private const val PLAYLIST_PREFIX = "playlist/"
    private const val ARTIST_PREFIX = "artist/"
    private const val ALBUM_PREFIX = "album/"
    private const val FAVORITE_ARTIST_PREFIX = "favorites/artist/"
    private const val FAVORITE_ALBUM_PREFIX = "favorites/album/"
    private const val SEARCH_PREFIX = "search/"
    private const val TRACK_PREFIX = "track/"

    fun isEmpty(snapshot: AutoLibrarySnapshot): Boolean {
        return snapshot.songs.isEmpty() &&
            snapshot.playlists.isEmpty() &&
            snapshot.recentlyPlayed.isEmpty() &&
            snapshot.mostPlayed.isEmpty() &&
            snapshot.favoriteTracks.isEmpty()
    }

    fun item(mediaId: String, snapshot: AutoLibrarySnapshot): AutoBrowseItem? {
        parseTrack(mediaId)?.let { return trackItem(it, snapshot) }
        return folder(mediaId, snapshot)
    }

    /**
     * Children of a browsable id. Null when [parentId] is not part of the tree.
     * An empty list means the node exists and has nothing under it.
     */
    fun children(parentId: String, snapshot: AutoLibrarySnapshot): List<AutoBrowseItem>? {
        val parent = item(parentId, snapshot) ?: return null
        if (!parent.browsable) return emptyList()
        return when (parentId) {
            ROOT -> if (isEmpty(snapshot)) listOf(importItem()) else rootFolders(snapshot)
            RECENT -> songs(RECENT, snapshot.recentlyPlayed)
            MOST -> snapshot.mostPlayed.mapIndexed { index, track ->
                songItem(
                    parentId = MOST,
                    index = index,
                    song = track.song,
                    subtitle = playCountLabel(track.playCount),
                )
            }
            PLAYLISTS -> snapshot.playlists.map { playlistItem(it) }
            ARTISTS -> artistGroups(catalogSongs(snapshot)).map { (name, songs) ->
                groupItem(artistId(name), name, songs.size, songs.firstArtwork())
            }
            ALBUMS -> albumGroups(catalogSongs(snapshot)).map { (name, songs) ->
                groupItem(albumId(name), name, songs.size, songs.firstArtwork())
            }
            SONGS -> songs(SONGS, catalogSongs(snapshot))
            FAVORITES -> listOf(
                groupItem(FAVORITE_TRACKS, TRACKS_TITLE, snapshot.favoriteTracks.size, snapshot.favoriteTracks.firstArtwork()),
                groupItem(
                    FAVORITE_ARTISTS,
                    ARTISTS_TITLE,
                    snapshot.favoriteArtists.size,
                    null,
                ),
                groupItem(
                    FAVORITE_ALBUMS,
                    ALBUMS_TITLE,
                    snapshot.favoriteAlbums.size,
                    snapshot.favoriteAlbums.firstNotNullOfOrNull { it.artworkUrl },
                ),
            )
            FAVORITE_TRACKS -> songs(FAVORITE_TRACKS, snapshot.favoriteTracks)
            FAVORITE_ARTISTS -> snapshot.favoriteArtists.map { artist ->
                val tracks = favoriteTracksForArtist(artist.artist, snapshot.favoriteTracks)
                groupItem(favoriteArtistId(artist.artist), artist.artist, tracks.size, tracks.firstArtwork())
            }
            FAVORITE_ALBUMS -> snapshot.favoriteAlbums.map { album ->
                val tracks = favoriteTracksForAlbum(album, snapshot.favoriteTracks)
                groupItem(
                    favoriteAlbumId(album.artist, album.album),
                    album.album,
                    tracks.size,
                    album.artworkUrl ?: tracks.firstArtwork(),
                )
            }
            IMPORT -> emptyList()
            else -> when {
                parentId.startsWith(PLAYLIST_PREFIX) -> {
                    val playlist = playlist(parentId, snapshot) ?: return emptyList()
                    songs(parentId, playlist.tracks.map { it.song })
                }
                parentId.startsWith(ARTIST_PREFIX) ->
                    songs(parentId, songsForArtist(decode(parentId.removePrefix(ARTIST_PREFIX)), catalogSongs(snapshot)))
                parentId.startsWith(ALBUM_PREFIX) ->
                    songs(parentId, songsForAlbum(decode(parentId.removePrefix(ALBUM_PREFIX)), catalogSongs(snapshot)))
                parentId.startsWith(FAVORITE_ARTIST_PREFIX) ->
                    songs(
                        parentId,
                        favoriteTracksForArtist(
                            decode(parentId.removePrefix(FAVORITE_ARTIST_PREFIX)),
                            snapshot.favoriteTracks,
                        ),
                    )
                parentId.startsWith(FAVORITE_ALBUM_PREFIX) -> {
                    val (artist, album) = parseFavoriteAlbum(parentId) ?: return emptyList()
                    songs(
                        parentId,
                        favoriteTracksForAlbum(
                            FavoriteAlbum(album = album, artist = artist, playCount = 0),
                            snapshot.favoriteTracks,
                        ),
                    )
                }
                parentId.startsWith(SEARCH_PREFIX) ->
                    search(decode(parentId.removePrefix(SEARCH_PREFIX)), snapshot)
                else -> emptyList()
            }
        }
    }

    fun playRequest(mediaId: String, snapshot: AutoLibrarySnapshot): AutoPlayRequest? {
        parseTrack(mediaId)?.let { ref ->
            val queue = songQueue(ref.parentId, snapshot) ?: return null
            val index = when {
                queue.getOrNull(ref.index)?.id == ref.songId -> ref.index
                else -> queue.indexOfFirst { it.id == ref.songId }
            }
            if (index !in queue.indices) return null
            return AutoPlayRequest(queue, index)
        }
        val queue = songQueue(mediaId, snapshot) ?: return null
        if (queue.isEmpty() || folder(mediaId, snapshot)?.playable != true) return null
        return AutoPlayRequest(queue, 0)
    }

    /** Most recent track, then most played, then the library. Null when there is nothing to resume. */
    fun resumeRequest(snapshot: AutoLibrarySnapshot): AutoPlayRequest? {
        if (snapshot.recentlyPlayed.isNotEmpty()) {
            return AutoPlayRequest(snapshot.recentlyPlayed, 0)
        }
        if (snapshot.mostPlayed.isNotEmpty()) {
            return AutoPlayRequest(snapshot.mostPlayed.map { it.song }, 0)
        }
        val songs = catalogSongs(snapshot)
        if (songs.isEmpty()) return null
        return AutoPlayRequest(songs, 0)
    }

    fun search(query: String, snapshot: AutoLibrarySnapshot): List<AutoBrowseItem> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()
        val needle = trimmed.lowercase()
        val parent = searchId(trimmed)
        return catalogSongs(snapshot)
            .filter { song ->
                song.title.lowercase().contains(needle) ||
                    song.artist.lowercase().contains(needle) ||
                    (song.album?.name?.lowercase()?.contains(needle) == true)
            }
            .mapIndexed { index, song -> songItem(parent, index, song, song.artist) }
    }

    fun <T> page(items: List<T>, page: Int, pageSize: Int): List<T> {
        if (page < 0 || pageSize < 1 || items.isEmpty()) return emptyList()
        val from = page.toLong() * pageSize.toLong()
        if (from < 0L || from >= items.size) return emptyList()
        val start = from.toInt()
        val end = minOf(items.size.toLong(), from + pageSize.toLong()).toInt()
        return items.subList(start, end)
    }

    private fun rootFolders(snapshot: AutoLibrarySnapshot): List<AutoBrowseItem> {
        return listOf(RECENT, MOST, PLAYLISTS, ARTISTS, ALBUMS, SONGS, FAVORITES)
            .mapNotNull { folder(it, snapshot) }
    }

    private fun folder(id: String, snapshot: AutoLibrarySnapshot): AutoBrowseItem? {
        return when (id) {
            ROOT -> AutoBrowseItem(
                id = ROOT,
                title = "Groove Player",
                subtitle = if (isEmpty(snapshot)) IMPORT_TITLE else null,
                browsable = true,
                playable = false,
            )
            RECENT -> queueFolder(RECENT, RECENT_TITLE, snapshot.recentlyPlayed)
            MOST -> queueFolder(MOST, MOST_TITLE, snapshot.mostPlayed.map { it.song })
            PLAYLISTS -> AutoBrowseItem(
                id = PLAYLISTS,
                title = PLAYLISTS_TITLE,
                subtitle = playlistCountLabel(snapshot.playlists.size),
                browsable = true,
                playable = false,
            )
            ARTISTS -> countFolder(ARTISTS, ARTISTS_TITLE, artistGroups(catalogSongs(snapshot)).size, "artist", "artists")
            ALBUMS -> countFolder(ALBUMS, ALBUMS_TITLE, albumGroups(catalogSongs(snapshot)).size, "album", "albums")
            SONGS -> queueFolder(SONGS, SONGS_TITLE, catalogSongs(snapshot))
            FAVORITES -> AutoBrowseItem(
                id = FAVORITES,
                title = FAVORITES_TITLE,
                subtitle = likedTrackLabel(snapshot.favoriteTracks.size),
                browsable = true,
                playable = false,
            )
            FAVORITE_TRACKS -> queueFolder(FAVORITE_TRACKS, TRACKS_TITLE, snapshot.favoriteTracks)
            FAVORITE_ARTISTS -> countFolder(
                FAVORITE_ARTISTS,
                ARTISTS_TITLE,
                snapshot.favoriteArtists.size,
                "artist",
                "artists",
            )
            FAVORITE_ALBUMS -> countFolder(
                FAVORITE_ALBUMS,
                ALBUMS_TITLE,
                snapshot.favoriteAlbums.size,
                "album",
                "albums",
            )
            IMPORT -> if (isEmpty(snapshot)) importItem() else null
            else -> when {
                id.startsWith(PLAYLIST_PREFIX) -> playlist(id, snapshot)?.let { playlistItem(it) }
                id.startsWith(ARTIST_PREFIX) -> {
                    val name = decode(id.removePrefix(ARTIST_PREFIX))
                    val songs = songsForArtist(name, catalogSongs(snapshot))
                    groupItem(id, name, songs.size, songs.firstArtwork())
                }
                id.startsWith(ALBUM_PREFIX) -> {
                    val name = decode(id.removePrefix(ALBUM_PREFIX))
                    val songs = songsForAlbum(name, catalogSongs(snapshot))
                    groupItem(id, name, songs.size, songs.firstArtwork())
                }
                id.startsWith(FAVORITE_ARTIST_PREFIX) -> {
                    val name = decode(id.removePrefix(FAVORITE_ARTIST_PREFIX))
                    val songs = favoriteTracksForArtist(name, snapshot.favoriteTracks)
                    groupItem(id, name, songs.size, songs.firstArtwork())
                }
                id.startsWith(FAVORITE_ALBUM_PREFIX) -> {
                    val parsed = parseFavoriteAlbum(id) ?: return null
                    val songs = favoriteTracksForAlbum(
                        FavoriteAlbum(album = parsed.second, artist = parsed.first, playCount = 0),
                        snapshot.favoriteTracks,
                    )
                    groupItem(id, parsed.second, songs.size, songs.firstArtwork())
                }
                id.startsWith(SEARCH_PREFIX) -> AutoBrowseItem(
                    id = id,
                    title = "Search",
                    subtitle = decode(id.removePrefix(SEARCH_PREFIX)),
                    browsable = true,
                    playable = search(decode(id.removePrefix(SEARCH_PREFIX)), snapshot).isNotEmpty(),
                )
                else -> null
            }
        }
    }

    private fun importItem(): AutoBrowseItem {
        return AutoBrowseItem(
            id = IMPORT,
            title = IMPORT_TITLE,
            subtitle = "Open GroovePlayer and import a folder",
            browsable = true,
            playable = false,
        )
    }

    private fun queueFolder(id: String, title: String, songs: List<Song>): AutoBrowseItem {
        return AutoBrowseItem(
            id = id,
            title = title,
            subtitle = songsLabel(songs.size),
            browsable = true,
            playable = songs.isNotEmpty(),
        )
    }

    private fun countFolder(id: String, title: String, count: Int, singular: String, plural: String): AutoBrowseItem {
        return AutoBrowseItem(
            id = id,
            title = title,
            subtitle = if (count == 1) "1 $singular" else "$count $plural",
            browsable = true,
            playable = false,
        )
    }

    private fun groupItem(id: String, title: String, count: Int, artworkUrl: String?): AutoBrowseItem {
        return AutoBrowseItem(
            id = id,
            title = title,
            subtitle = songsLabel(count),
            browsable = true,
            playable = count > 0,
            artworkUrl = artworkUrl,
        )
    }

    private fun playlistItem(playlist: PlaylistWithTracks): AutoBrowseItem {
        return AutoBrowseItem(
            id = playlistId(playlist.playlist.id),
            title = playlist.playlist.name,
            subtitle = songsLabel(playlist.tracks.size),
            browsable = true,
            playable = playlist.tracks.isNotEmpty(),
            artworkUrl = playlist.playlist.artworkUrls.firstOrNull(),
        )
    }

    private fun songs(parentId: String, songs: List<Song>): List<AutoBrowseItem> {
        return songs.mapIndexed { index, song -> songItem(parentId, index, song, song.artist) }
    }

    private fun songItem(parentId: String, index: Int, song: Song, subtitle: String): AutoBrowseItem {
        return AutoBrowseItem(
            id = trackId(parentId, index, song.id),
            title = song.title.ifBlank { "Unknown" },
            subtitle = subtitle.ifBlank { PrivateLibrarySongs.ARTIST },
            browsable = false,
            playable = true,
            artist = song.artist.ifBlank { PrivateLibrarySongs.ARTIST },
            album = song.album?.name?.takeIf { it.isNotBlank() },
            artworkUrl = song.artworkUrl,
            durationMs = song.durationMs,
        )
    }

    private fun trackItem(ref: TrackRef, snapshot: AutoLibrarySnapshot): AutoBrowseItem? {
        val queue = songQueue(ref.parentId, snapshot) ?: return null
        val song = queue.getOrNull(ref.index)?.takeIf { it.id == ref.songId }
            ?: queue.firstOrNull { it.id == ref.songId }
            ?: return null
        val index = queue.indexOfFirst { it.id == song.id }
        val subtitle = if (ref.parentId == MOST) {
            snapshot.mostPlayed.getOrNull(index)?.let { playCountLabel(it.playCount) } ?: song.artist
        } else {
            song.artist
        }
        return songItem(ref.parentId, index, song, subtitle)
    }

    private fun songQueue(parentId: String, snapshot: AutoLibrarySnapshot): List<Song>? {
        return when (parentId) {
            RECENT -> snapshot.recentlyPlayed
            MOST -> snapshot.mostPlayed.map { it.song }
            SONGS -> catalogSongs(snapshot)
            FAVORITE_TRACKS -> snapshot.favoriteTracks
            else -> when {
                parentId.startsWith(PLAYLIST_PREFIX) ->
                    playlist(parentId, snapshot)?.tracks?.map { it.song }
                parentId.startsWith(ARTIST_PREFIX) ->
                    songsForArtist(decode(parentId.removePrefix(ARTIST_PREFIX)), catalogSongs(snapshot))
                parentId.startsWith(ALBUM_PREFIX) ->
                    songsForAlbum(decode(parentId.removePrefix(ALBUM_PREFIX)), catalogSongs(snapshot))
                parentId.startsWith(FAVORITE_ARTIST_PREFIX) ->
                    favoriteTracksForArtist(
                        decode(parentId.removePrefix(FAVORITE_ARTIST_PREFIX)),
                        snapshot.favoriteTracks,
                    )
                parentId.startsWith(FAVORITE_ALBUM_PREFIX) -> {
                    val parsed = parseFavoriteAlbum(parentId) ?: return emptyList()
                    favoriteTracksForAlbum(
                        FavoriteAlbum(album = parsed.second, artist = parsed.first, playCount = 0),
                        snapshot.favoriteTracks,
                    )
                }
                parentId.startsWith(SEARCH_PREFIX) ->
                    matchingSongs(decode(parentId.removePrefix(SEARCH_PREFIX)), snapshot)
                else -> null
            }
        }
    }

    /**
     * On-disk private-library songs first, then playlist, like, and history rows
     * whose file is already missing. The file-backed row wins when both exist.
     */
    private fun catalogSongs(snapshot: AutoLibrarySnapshot): List<Song> {
        val byId = LinkedHashMap<String, Song>()
        for (song in snapshot.songs) {
            if (song.id.isNotBlank()) byId[song.id] = song
        }
        fun add(song: Song) {
            if (song.id.isBlank()) return
            byId.putIfAbsent(song.id, song)
        }
        snapshot.playlists.flatMap { it.tracks }.forEach { add(it.song) }
        snapshot.favoriteTracks.forEach(::add)
        snapshot.recentlyPlayed.forEach(::add)
        snapshot.mostPlayed.forEach { add(it.song) }
        return byId.values.sortedWith(compareBy({ it.title.lowercase() }, { it.id }))
    }

    private fun artistGroups(songs: List<Song>): List<Pair<String, List<Song>>> {
        val groups = linkedMapOf<String, Pair<String, MutableList<Song>>>()
        for (song in songs) {
            val name = song.artist.ifBlank { PrivateLibrarySongs.ARTIST }
            val bucket = groups.getOrPut(name.lowercase()) { name to mutableListOf() }
            bucket.second.add(song)
        }
        return groups.values
            .map { (name, tracks) -> name to tracks.toList() }
            .sortedBy { it.first.lowercase() }
    }

    private fun albumGroups(songs: List<Song>): List<Pair<String, List<Song>>> {
        val groups = linkedMapOf<String, Pair<String, MutableList<Song>>>()
        for (song in songs) {
            val name = song.album?.name?.takeIf { it.isNotBlank() } ?: "Unknown Album"
            val bucket = groups.getOrPut(name.lowercase()) { name to mutableListOf() }
            bucket.second.add(song)
        }
        return groups.values
            .map { (name, tracks) -> name to tracks.toList() }
            .sortedBy { it.first.lowercase() }
    }

    private fun songsForArtist(name: String, songs: List<Song>): List<Song> {
        return songs.filter { it.artist.ifBlank { PrivateLibrarySongs.ARTIST }.equals(name, ignoreCase = true) }
    }

    private fun songsForAlbum(name: String, songs: List<Song>): List<Song> {
        return songs.filter { song ->
            val album = song.album?.name?.takeIf { it.isNotBlank() } ?: "Unknown Album"
            album.equals(name, ignoreCase = true)
        }
    }

    private fun favoriteTracksForArtist(name: String, tracks: List<Song>): List<Song> {
        return tracks.filter {
            it.artist.ifBlank { PrivateLibrarySongs.ARTIST }.equals(name, ignoreCase = true)
        }
    }

    private fun favoriteTracksForAlbum(album: FavoriteAlbum, tracks: List<Song>): List<Song> {
        return tracks.filter { song ->
            val albumName = song.album?.name?.takeIf { it.isNotBlank() } ?: "Unknown Album"
            val artistName = song.artist.ifBlank { PrivateLibrarySongs.ARTIST }
            albumName.equals(album.album, ignoreCase = true) &&
                artistName.equals(album.artist, ignoreCase = true)
        }
    }

    private fun matchingSongs(query: String, snapshot: AutoLibrarySnapshot): List<Song> {
        val needle = query.trim().lowercase()
        if (needle.isEmpty()) return emptyList()
        return catalogSongs(snapshot).filter { song ->
            song.title.lowercase().contains(needle) ||
                song.artist.lowercase().contains(needle) ||
                (song.album?.name?.lowercase()?.contains(needle) == true)
        }
    }

    private fun playlist(id: String, snapshot: AutoLibrarySnapshot): PlaylistWithTracks? {
        val playlistId = id.removePrefix(PLAYLIST_PREFIX).toLongOrNull() ?: return null
        return snapshot.playlists.firstOrNull { it.playlist.id == playlistId }
    }

    private fun parseFavoriteAlbum(id: String): Pair<String, String>? {
        if (!id.startsWith(FAVORITE_ALBUM_PREFIX)) return null
        val rest = id.removePrefix(FAVORITE_ALBUM_PREFIX)
        val artist = rest.substringBefore('/', missingDelimiterValue = "")
        val album = rest.substringAfter('/', missingDelimiterValue = "")
        if (artist.isEmpty() || album.isEmpty()) return null
        return decode(artist) to decode(album)
    }

    private fun parseTrack(id: String): TrackRef? {
        if (!id.startsWith(TRACK_PREFIX)) return null
        val rest = id.removePrefix(TRACK_PREFIX)
        val parent = rest.substringBefore('/', missingDelimiterValue = "")
        val afterParent = rest.substringAfter('/', missingDelimiterValue = "")
        val indexText = afterParent.substringBefore('/', missingDelimiterValue = "")
        val song = afterParent.substringAfter('/', missingDelimiterValue = "")
        val index = indexText.toIntOrNull()
        if (parent.isEmpty() || index == null || index < 0 || song.isEmpty()) return null
        return TrackRef(decode(parent), index, decode(song))
    }

    private fun playlistId(id: Long) = "$PLAYLIST_PREFIX$id"
    private fun artistId(name: String) = "$ARTIST_PREFIX${encode(name)}"
    private fun albumId(name: String) = "$ALBUM_PREFIX${encode(name)}"
    private fun favoriteArtistId(name: String) = "$FAVORITE_ARTIST_PREFIX${encode(name)}"
    private fun favoriteAlbumId(artist: String, album: String) =
        "$FAVORITE_ALBUM_PREFIX${encode(artist)}/${encode(album)}"
    private fun searchId(query: String) = "$SEARCH_PREFIX${encode(query)}"
    private fun trackId(parentId: String, index: Int, songId: String) =
        "$TRACK_PREFIX${encode(parentId)}/$index/${encode(songId)}"

    @Suppress("DEPRECATION")
    private fun encode(value: String): String =
        URLEncoder.encode(value, "UTF-8").replace("+", "%20")

    @Suppress("DEPRECATION")
    private fun decode(value: String): String = URLDecoder.decode(value, "UTF-8")

    private fun songsLabel(count: Int): String = if (count == 1) "1 song" else "$count songs"

    private fun List<Song>.firstArtwork(): String? =
        firstNotNullOfOrNull { it.artworkUrl?.takeIf(String::isNotBlank) }

    private data class TrackRef(val parentId: String, val index: Int, val songId: String)
}

data class AutoLibrarySnapshot(
    val songs: List<Song>,
    val playlists: List<PlaylistWithTracks>,
    val recentlyPlayed: List<Song>,
    val mostPlayed: List<MostPlayedTrack>,
    val favoriteTracks: List<Song>,
    val favoriteArtists: List<FavoriteArtist> = emptyList(),
    val favoriteAlbums: List<FavoriteAlbum> = emptyList(),
)

data class AutoBrowseItem(
    val id: String,
    val title: String,
    val subtitle: String?,
    val browsable: Boolean,
    val playable: Boolean,
    val artist: String? = null,
    val album: String? = null,
    val artworkUrl: String? = null,
    val durationMs: Long = 0L,
)

data class AutoPlayRequest(
    val songs: List<Song>,
    val startIndex: Int,
)
