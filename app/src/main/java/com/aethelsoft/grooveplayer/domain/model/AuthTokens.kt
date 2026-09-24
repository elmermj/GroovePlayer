package com.aethelsoft.grooveplayer.domain.model

data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresInSeconds: Long = 0L,
)
