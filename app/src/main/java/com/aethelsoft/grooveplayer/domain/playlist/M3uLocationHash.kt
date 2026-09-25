package com.aethelsoft.grooveplayer.domain.playlist

/** SHA-256 of an M3U location when that file can be read. Null leaves matching to path and name. */
fun interface M3uLocationHash {
    fun sha256OrNull(location: String): String?
}
