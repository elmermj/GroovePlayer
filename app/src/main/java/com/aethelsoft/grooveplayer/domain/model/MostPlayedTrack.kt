package com.aethelsoft.grooveplayer.domain.model

/**
 * A library track ranked by how many times it has been played locally.
 */
data class MostPlayedTrack(
    val song: Song,
    val playCount: Int,
)

fun playCountLabel(playCount: Int): String =
    if (playCount == 1) "1 play" else "$playCount plays"
