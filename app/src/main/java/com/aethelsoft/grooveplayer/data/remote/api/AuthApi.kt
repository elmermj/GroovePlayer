package com.aethelsoft.grooveplayer.data.remote.api

import com.aethelsoft.grooveplayer.data.remote.dto.GoogleAuthRequestDto
import com.aethelsoft.grooveplayer.data.remote.dto.LogoutRequestDto
import com.aethelsoft.grooveplayer.data.remote.dto.PublicUserDto
import com.aethelsoft.grooveplayer.data.remote.dto.RefreshRequestDto
import com.aethelsoft.grooveplayer.data.remote.dto.DeleteAccountResponseDto
import com.aethelsoft.grooveplayer.data.remote.dto.TokenResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Matches grooveplayer-backend handlers exactly.
 */
interface AuthApi {
    @POST("/v1/auth/google")
    suspend fun signInWithGoogle(@Body body: GoogleAuthRequestDto): TokenResponseDto

    @POST("/v1/auth/refresh")
    suspend fun refresh(@Body body: RefreshRequestDto): TokenResponseDto

    @POST("/v1/auth/logout")
    suspend fun logout(@Body body: LogoutRequestDto): okhttp3.ResponseBody

    @GET("/v1/me")
    suspend fun me(@Header("Authorization") authorization: String): PublicUserDto

    /**
     * Hard-delete the signed-in account (user + tokens + R2 backups).
     * Auth via OkHttp Bearer interceptor. Body: none.
     *
     * Success: 200 {"deleted":true} or {"deleted":true,"already_gone":true}
     * Errors: 401 (bad token), 500 (R2/DB mid-flight — do NOT clear session).
     *
     * Live on https://grooveplayer-backend.fly.dev (Benny deploy).
     */
    @DELETE("/v1/account")
    suspend fun deleteAccount(): Response<DeleteAccountResponseDto>
}
