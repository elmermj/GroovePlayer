package com.aethelsoft.grooveplayer.domain.model

private const val ALBUM_ID_SEPARATOR = "::__::"

fun makeAlbumId(artist: String, name: String): String =
    "$artist$ALBUM_ID_SEPARATOR$name"

fun parseAlbumId(id: String): Pair<String, String> {
    val parts = id.split(ALBUM_ID_SEPARATOR, limit = 2)
    return if (parts.size == 2) {
        parts[0] to parts[1]
    } else {
        "" to id
    }
}

