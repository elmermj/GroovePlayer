package com.aethelsoft.grooveplayer.domain.model

/**
 * Connection details exchanged via NFC or displayed for manual connection.
 */
data class ShareSessionInfo(
    val host: String,
    val port: Int,
    val sessionToken: String,
    val deviceName: String
)
