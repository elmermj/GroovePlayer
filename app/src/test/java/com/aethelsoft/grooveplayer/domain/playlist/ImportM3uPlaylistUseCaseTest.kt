package com.aethelsoft.grooveplayer.domain.playlist

import com.aethelsoft.grooveplayer.domain.model.M3uImportResult
import com.aethelsoft.grooveplayer.domain.model.Playlist
import com.aethelsoft.grooveplayer.domain.model.PlaylistTrack
import com.aethelsoft.grooveplayer.domain.model.PlaylistWithTracks
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.domain.model.StorageUsageData
import com.aethelsoft.grooveplayer.domain.repository.MusicRepository
import com.aethelsoft.grooveplayer.domain.repository.PlaylistRepository
import com.aethelsoft.grooveplayer.domain.usecase.playlist_category.ExportM3uPlaylistUseCase
import com.aethelsoft.grooveplayer.domain.usecase.playlist_category.ImportM3uPlaylistUseCase
import com.aethelsoft.grooveplayer.domain.usecase.playlist_category.ReorderPlaylistUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportM3uPlaylistUseCaseTest {

    @Test
    fun `import keeps library order and export writes the same paths`() = runBlocking {
        val first = librarySong("1", "/storage/emulated/0/Music/one.mp3")
        val second = librarySong("2", "/storage/emulated/0/Music/two.mp3")
        val playlists = FakePlaylistRepository()
        val music = FakeMusicRepository(listOf(first, second))
        val importer = ImportM3uPlaylistUseCase(playlists, music)
        val exporter = ExportM3uPlaylistUseCase(playlists)
        val source = """
            #EXTM3U
            #PLAYLIST:Ignored when a file name is given
            #EXTINF:10,Missing
            /storage/emulated/0/Music/missing.mp3
            file:///storage/emulated/0/Music/two.mp3
            /storage/emulated/0/Music/one.mp3
        """.trimIndent()

        val result = importer("Road trip.m3u", source)

        val success = result as M3uImportResult.Success
        assertEquals("Road trip.m3u", success.playlistName)
        assertEquals(2, success.importedCount)
        assertEquals(listOf("/storage/emulated/0/Music/missing.mp3"), success.missingLocations)

        val exported = exporter(success.playlistId)!!
        val parsed = M3uCodec.parse(exported)
        assertEquals("Road trip.m3u", parsed.name)
        assertEquals(
            listOf(
                "/storage/emulated/0/Music/two.mp3",
                "/storage/emulated/0/Music/one.mp3",
            ),
            parsed.tracks.map { it.location },
        )
        val rematch = PlaylistLibraryPaths.match(parsed.tracks, listOf(first, second))
        assertEquals(listOf("2", "1"), rematch.matched.map { it.id })
        assertTrue(rematch.missingLocations.isEmpty())
    }

    @Test
    fun `import fails when nothing in the file is in the library`() = runBlocking {
        val playlists = FakePlaylistRepository()
        val result = ImportM3uPlaylistUseCase(
            playlists,
            FakeMusicRepository(emptyList()),
        ).invoke("Mix", "/storage/emulated/0/Music/missing.mp3\n")

        assertTrue(result is M3uImportResult.Failure)
        assertTrue(playlists.stored.isEmpty())
    }

    @Test
    fun `reorder moves a track once`() = runBlocking {
        val playlists = FakePlaylistRepository()
        val id = playlists.create("Mix")
        playlists.addSongs(
            id,
            listOf(
                librarySong("a", "/a.mp3"),
                librarySong("b", "/b.mp3"),
                librarySong("c", "/c.mp3"),
            ),
        )

        ReorderPlaylistUseCase(playlists).invoke(id, 0, 2)

        assertEquals(
            listOf("b", "c", "a"),
            playlists.getPlaylist(id)!!.tracks.map { it.song.id },
        )
    }

    private fun librarySong(id: String, path: String) = Song(
        id = id,
        title = id,
        artist = "Ada",
        uri = "content://media/external/audio/media/$id",
        genre = "",
        durationMs = 1_000,
        filePath = path,
    )
}

private class FakeMusicRepository(
    private val songs: List<Song>,
) : MusicRepository {
    override suspend fun getAllSongs(): List<Song> = songs
    override suspend fun getSongsPage(offset: Int, limit: Int): List<Song> = songs
    override suspend fun getSongsByArtist(artist: String): List<Song> = emptyList()
    override suspend fun getSongsByArtistPage(artist: String, offset: Int, limit: Int): List<Song> = emptyList()
    override suspend fun getSongsByAlbum(album: String): List<Song> = emptyList()
    override suspend fun getSongsByAlbumPage(album: String, offset: Int, limit: Int): List<Song> = emptyList()
    override suspend fun searchSongs(query: String): List<Song> = emptyList()
    override suspend fun getMusicFolderPaths(): List<String> = emptyList()
    override suspend fun getStorageUsage(): StorageUsageData = error("unused")
    override val catalogGeneration = MutableStateFlow(0L)
    override fun bumpCatalogGeneration() = Unit
}

private class FakePlaylistRepository : PlaylistRepository {
    val stored = mutableListOf<StoredPlaylist>()
    private var nextPlaylistId = 1L
    private var nextEntryId = 1L

    override fun observePlaylists(): Flow<List<Playlist>> =
        flowOf(stored.map { it.summary })

    override fun observePlaylist(id: Long): Flow<PlaylistWithTracks?> =
        flowOf(getSnapshot(id))

    override suspend fun getPlaylist(id: Long): PlaylistWithTracks? = getSnapshot(id)

    override suspend fun create(name: String): Long {
        val id = nextPlaylistId++
        stored += StoredPlaylist(
            summary = Playlist(id = id, name = name, createdAt = 0, updatedAt = 0, trackCount = 0),
            tracks = mutableListOf(),
        )
        return id
    }

    override suspend fun rename(id: Long, name: String) {
        val row = stored.first { it.summary.id == id }
        row.summary = row.summary.copy(name = name)
    }

    override suspend fun delete(id: Long) {
        stored.removeAll { it.summary.id == id }
    }

    override suspend fun addSongs(playlistId: Long, songs: List<Song>) {
        val row = stored.first { it.summary.id == playlistId }
        songs.forEach { song ->
            row.tracks += PlaylistTrack(
                entryId = nextEntryId++,
                position = row.tracks.size,
                song = song,
            )
        }
        row.summary = row.summary.copy(trackCount = row.tracks.size)
    }

    override suspend fun removeTrack(playlistId: Long, entryId: Long) {
        val row = stored.first { it.summary.id == playlistId }
        row.tracks.removeAll { it.entryId == entryId }
    }

    override suspend fun reorder(playlistId: Long, orderedEntryIds: List<Long>) {
        val row = stored.first { it.summary.id == playlistId }
        val byId = row.tracks.associateBy { it.entryId }
        row.tracks.clear()
        orderedEntryIds.forEachIndexed { index, entryId ->
            val track = byId.getValue(entryId)
            row.tracks += track.copy(position = index)
        }
    }

    private fun getSnapshot(id: Long): PlaylistWithTracks? {
        val row = stored.firstOrNull { it.summary.id == id } ?: return null
        return PlaylistWithTracks(row.summary, row.tracks.toList())
    }
}

private class StoredPlaylist(
    var summary: Playlist,
    val tracks: MutableList<PlaylistTrack>,
)
