package com.aethelsoft.grooveplayer.domain.playlist

import com.aethelsoft.grooveplayer.domain.model.M3uTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class M3uCodecTest {

    @Test
    fun `extended playlist round trips local paths titles and durations`() {
        val tracks = listOf(
            M3uTrack(
                location = "/storage/emulated/0/Music/Ada - One.mp3",
                title = "Ada - One",
                durationSeconds = 215,
            ),
            M3uTrack(
                location = "/storage/emulated/0/Music/Bea, Live.mp3",
                title = "Bea, Live",
                durationSeconds = -1,
            ),
        )

        val text = M3uCodec.serialize("Evening mix", tracks)
        val parsed = M3uCodec.parse(text)

        assertEquals("Evening mix", parsed.name)
        assertEquals(tracks, parsed.tracks)
        assertTrue(text.startsWith("#EXTM3U\n"))
    }

    @Test
    fun `simple m3u and comments are ignored`() {
        val text = """
            #EXTM3U
            # a comment
            #EXTVLCOPT:something

            /storage/emulated/0/Music/a.mp3

            #EXTINF:12,Only this one
            /storage/emulated/0/Music/b.mp3
        """.trimIndent()

        val parsed = M3uCodec.parse(text)

        assertEquals(
            listOf("/storage/emulated/0/Music/a.mp3", "/storage/emulated/0/Music/b.mp3"),
            parsed.tracks.map { it.location },
        )
        assertEquals(null, parsed.tracks[0].title)
        assertEquals(-1L, parsed.tracks[0].durationSeconds)
        assertEquals("Only this one", parsed.tracks[1].title)
        assertEquals(12L, parsed.tracks[1].durationSeconds)
    }

    @Test
    fun `crlf playlist name and quoted paths parse`() {
        val text = "#EXTM3U\r\n#PLAYLIST:Night\r\n#EXTINF:10,A - B\r\n\"/storage/emulated/0/Music/a.mp3\"\r\n"

        val parsed = M3uCodec.parse(text)

        assertEquals("Night", parsed.name)
        assertEquals("/storage/emulated/0/Music/a.mp3", parsed.tracks.single().location)
        assertEquals("A - B", parsed.tracks.single().title)
    }

    @Test
    fun `utf-8 bom is stripped when decoding bytes`() {
        val body = "#EXTM3U\n/storage/emulated/0/Music/a.mp3\n"
        val bytes = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + body.toByteArray()

        val parsed = M3uCodec.parse(M3uCodec.decode(bytes))

        assertEquals("/storage/emulated/0/Music/a.mp3", parsed.tracks.single().location)
    }
}
