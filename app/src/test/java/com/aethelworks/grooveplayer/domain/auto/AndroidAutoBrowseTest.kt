package com.aethelworks.grooveplayer.domain.auto

import com.aethelworks.grooveplayer.domain.model.Album
import com.aethelworks.grooveplayer.domain.model.FavoriteAlbum
import com.aethelworks.grooveplayer.domain.model.FavoriteArtist
import com.aethelworks.grooveplayer.domain.model.MostPlayedTrack
import com.aethelworks.grooveplayer.domain.model.Playlist
import com.aethelworks.grooveplayer.domain.model.PlaylistTrack
import com.aethelworks.grooveplayer.domain.model.PlaylistWithTracks
import com.aethelworks.grooveplayer.domain.model.Song
import com.aethelworks.grooveplayer.domain.model.makeAlbumId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidAutoBrowseTest {

    @Test
    fun emptyLibraryIsOneImportItem() {
        val roots = AndroidAutoBrowse.children(AndroidAutoBrowse.ROOT, emptySnapshot())!!
        assertEquals(listOf(AndroidAutoBrowse.IMPORT_TITLE), roots.map { it.title })
        val prompt = roots.single()
        assertTrue(prompt.browsable)
        assertFalse(prompt.playable)
        assertTrue(AndroidAutoBrowse.children(prompt.id, emptySnapshot())!!.isEmpty())
        assertNull(AndroidAutoBrowse.playRequest(prompt.id, emptySnapshot()))
        assertNull(AndroidAutoBrowse.resumeRequest(emptySnapshot()))
    }

    @Test
    fun rootsComeFromThePrivateLibraryPlaylistsAndLikes() {
        val roots = AndroidAutoBrowse.children(AndroidAutoBrowse.ROOT, sample())!!
        assertEquals(
            listOf(
                "Recently played",
                "Most played",
                "Playlists",
                "Artists",
                "Albums",
                "Songs",
                "Favorites",
            ),
            roots.map { it.title },
        )
        assertTrue(roots.all { it.browsable })
        assertEquals("2 playlists", roots.first { it.title == "Playlists" }.subtitle)
        assertEquals("2 liked tracks", roots.first { it.title == "Favorites" }.subtitle)
    }

    @Test
    fun songsArtistsAndAlbumsUseTheLibraryAndKeepAMissingFileFromAPlaylist() {
        val snapshot = sample()
        val songs = titles(AndroidAutoBrowse.ROOT, "Songs", snapshot)
        assertEquals(listOf("Cloud Only", "First", "Second"), songs)

        val artists = titles(AndroidAutoBrowse.ROOT, "Artists", snapshot)
        assertEquals(listOf("A / Slash", "Ada"), artists)
        val slash = open(AndroidAutoBrowse.ROOT, "Artists", snapshot)
            .let { artistsNode ->
                AndroidAutoBrowse.children(artistsNode.id, snapshot)!!
                    .first { it.title == "A / Slash" }
            }
        assertEquals(listOf("Second"), titles(slash.id, snapshot))

        val albums = titles(AndroidAutoBrowse.ROOT, "Albums", snapshot)
        assertEquals(listOf("Away", "Home"), albums)
        assertEquals(listOf("Cloud Only", "First"), titles(open(AndroidAutoBrowse.ROOT, "Albums", snapshot).let {
            AndroidAutoBrowse.children(it.id, snapshot)!!.first { item -> item.title == "Home" }.id
        }, snapshot))
    }

    @Test
    fun playlistKeepsOrderAndPlaysTheMissingFileThroughTheSameSong() {
        val snapshot = sample()
        val road = AndroidAutoBrowse.children(open(AndroidAutoBrowse.ROOT, "Playlists", snapshot).id, snapshot)!!
            .first { it.title == "Road" }
        val tracks = AndroidAutoBrowse.children(road.id, snapshot)!!
        assertEquals(listOf("Second", "Cloud Only"), tracks.map { it.title })
        assertTrue(tracks.all { it.playable && !it.browsable })

        val request = AndroidAutoBrowse.playRequest(tracks[1].id, snapshot)!!
        assertEquals(1, request.startIndex)
        assertEquals(listOf("Second", "Cloud Only"), request.songs.map { it.title })
        assertEquals("cloud-1", request.songs[1].id)
        assertEquals("", request.songs[1].uri)

        val whole = AndroidAutoBrowse.playRequest(road.id, snapshot)!!
        assertEquals(0, whole.startIndex)
        assertEquals("Second", whole.songs[0].title)
    }

    @Test
    fun fileBackedSongWinsWhenTheSameIdAlsoHasAMissingCopy() {
        val local = song("1", "Local", uri = "file:///data/groove-library/1.mp3")
        val missing = song("1", "Missing", uri = "")
        val snapshot = emptySnapshot().copy(
            songs = listOf(local),
            playlists = listOf(playlist(3, "Mix", listOf(missing))),
        )
        val request = AndroidAutoBrowse.playRequest(
            AndroidAutoBrowse.children(
                AndroidAutoBrowse.children(AndroidAutoBrowse.ROOT, snapshot)!!
                    .first { it.title == "Songs" }.id,
                snapshot,
            )!!.single().id,
            snapshot,
        )!!
        assertEquals("file:///data/groove-library/1.mp3", request.songs.single().uri)
        assertEquals("Local", request.songs.single().title)
    }

    @Test
    fun recentMostPlayedAndFavoritesKeepTheirOrder() {
        val snapshot = sample()
        assertEquals(listOf("Second", "First"), titles(AndroidAutoBrowse.ROOT, "Recently played", snapshot))
        val most = AndroidAutoBrowse.children(open(AndroidAutoBrowse.ROOT, "Most played", snapshot).id, snapshot)!!
        assertEquals(listOf("First", "Second"), most.map { it.title })
        assertEquals("4 plays", most[0].subtitle)

        val favorites = AndroidAutoBrowse.children(open(AndroidAutoBrowse.ROOT, "Favorites", snapshot).id, snapshot)!!
        assertEquals(listOf("Tracks", "Artists", "Albums"), favorites.map { it.title })
        assertEquals(listOf("Second", "First"), titles(favorites.first { it.title == "Tracks" }.id, snapshot))
        val artists = AndroidAutoBrowse.children(favorites.first { it.title == "Artists" }.id, snapshot)!!
        assertEquals(listOf("Ada", "A / Slash"), artists.map { it.title })
        assertEquals(
            listOf("First"),
            titles(artists.first { it.title == "Ada" }.id, snapshot),
        )
    }

    @Test
    fun searchMatchesTitleArtistAndAlbumAndPlaysThatResultList() {
        val snapshot = sample()
        val hits = AndroidAutoBrowse.search("home", snapshot)
        assertEquals(listOf("Cloud Only", "First"), hits.map { it.title })
        val request = AndroidAutoBrowse.playRequest(hits[1].id, snapshot)!!
        assertEquals(1, request.startIndex)
        assertEquals(listOf("Cloud Only", "First"), request.songs.map { it.title })
        assertTrue(AndroidAutoBrowse.search("   ", snapshot).isEmpty())
    }

    @Test
    fun resumeUsesRecentlyPlayedThenTheRestOfTheLibrary() {
        val snapshot = sample()
        val recent = AndroidAutoBrowse.resumeRequest(snapshot)!!
        assertEquals("Second", recent.songs.first().title)
        assertEquals(0, recent.startIndex)

        val withoutRecent = snapshot.copy(recentlyPlayed = emptyList(), mostPlayed = emptyList())
        assertEquals(
            listOf("Cloud Only", "First", "Second"),
            AndroidAutoBrowse.resumeRequest(withoutRecent)!!.songs.map { it.title },
        )
    }

    @Test
    fun pagingStaysInsideTheRequestedPage() {
        val items = (0 until 5).toList()
        assertEquals(listOf(0, 1), AndroidAutoBrowse.page(items, 0, 2))
        assertEquals(listOf(4), AndroidAutoBrowse.page(items, 2, 2))
        assertTrue(AndroidAutoBrowse.page(items, 3, 2).isEmpty())
        assertEquals(items, AndroidAutoBrowse.page(items, 0, Int.MAX_VALUE))
        assertTrue(AndroidAutoBrowse.page(items, -1, 2).isEmpty())
        assertNull(AndroidAutoBrowse.children("not-a-node", sample()))
    }

    private fun titles(parentId: String, title: String, snapshot: AutoLibrarySnapshot): List<String> {
        val node = if (parentId == AndroidAutoBrowse.ROOT) {
            open(parentId, title, snapshot)
        } else {
            AndroidAutoBrowse.children(parentId, snapshot)!!.first { it.title == title }
        }
        return titles(node.id, snapshot)
    }

    private fun titles(parentId: String, snapshot: AutoLibrarySnapshot): List<String> {
        return AndroidAutoBrowse.children(parentId, snapshot)!!.map { it.title }
    }

    private fun open(parentId: String, title: String, snapshot: AutoLibrarySnapshot): AutoBrowseItem {
        return AndroidAutoBrowse.children(parentId, snapshot)!!.first { it.title == title }
    }

    private fun sample(): AutoLibrarySnapshot {
        val first = song("1", "First", artist = "Ada", album = "Home")
        val second = song("2", "Second", artist = "A / Slash", album = "Away")
        val missing = song("cloud-1", "Cloud Only", artist = "Ada", album = "Home", uri = "")
        return AutoLibrarySnapshot(
            songs = listOf(second, first),
            playlists = listOf(
                playlist(1, "Road", listOf(second, missing)),
                playlist(2, "Empty", emptyList()),
            ),
            recentlyPlayed = listOf(second, first),
            mostPlayed = listOf(
                MostPlayedTrack(first, 4),
                MostPlayedTrack(second, 1),
            ),
            favoriteTracks = listOf(second, first),
            favoriteArtists = listOf(
                FavoriteArtist("Ada", 1),
                FavoriteArtist("A / Slash", 1),
            ),
            favoriteAlbums = listOf(
                FavoriteAlbum("Home", "Ada", 1),
                FavoriteAlbum("Away", "A / Slash", 1),
            ),
        )
    }

    private fun emptySnapshot() = AutoLibrarySnapshot(
        songs = emptyList(),
        playlists = emptyList(),
        recentlyPlayed = emptyList(),
        mostPlayed = emptyList(),
        favoriteTracks = emptyList(),
    )

    private fun playlist(id: Long, name: String, tracks: List<Song>): PlaylistWithTracks {
        return PlaylistWithTracks(
            playlist = Playlist(
                id = id,
                name = name,
                createdAt = 0L,
                updatedAt = 0L,
                trackCount = tracks.size,
            ),
            tracks = tracks.mapIndexed { index, song ->
                PlaylistTrack(
                    entryId = index.toLong(),
                    position = index,
                    song = song,
                    available = song.uri.isNotBlank(),
                )
            },
        )
    }

    private fun song(
        id: String,
        title: String,
        artist: String = "Ada",
        album: String = "Home",
        uri: String = "file:///data/groove-library/$id.mp3",
    ): Song {
        return Song(
            id = id,
            title = title,
            artist = artist,
            uri = uri,
            genre = "",
            durationMs = 1_000L,
            album = Album(
                id = makeAlbumId(artist, album),
                name = album,
                artist = artist,
                artworkUrl = null,
                songs = emptyList(),
            ),
            filePath = if (uri.isBlank()) "/data/groove-library/$id.mp3" else uri.removePrefix("file://"),
        )
    }
}
