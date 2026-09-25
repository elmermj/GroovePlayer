package com.aethelsoft.grooveplayer.data.auth

import android.util.Log
import com.aethelsoft.grooveplayer.data.remote.api.AuthApi
import com.aethelsoft.grooveplayer.data.remote.dto.RefreshRequestDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * On HTTP 401, refresh the access token once and retry the original call.
 *
 * Uses a Retrofit client that does not attach this authenticator, and does not
 * take [com.aethelsoft.grooveplayer.data.repository.AuthRepositoryImpl]'s mutex.
 * Startup `/v1/me` holds that mutex's caller only around bookkeeping; blocking
 * here on it would deadlock the first frame.
 */
class TokenRefreshAuthenticator(
    private val tokenStore: SecureTokenStore,
    private val refreshApi: AuthApi,
) : Authenticator {

    private val lock = ReentrantLock()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2) return null
        val path = response.request.url.encodedPath
        if (path == "/v1/auth/refresh" || path == "/v1/auth/google" || path == "/healthz") {
            return null
        }
        val failedToken = bearer(response.request)
        return lock.withLock {
            val current = tokenStore.getAccessToken()
            if (!current.isNullOrBlank() && current != failedToken) {
                return@withLock withBearer(response.request, current)
            }
            val refresh = tokenStore.getRefreshToken()?.takeIf { it.isNotBlank() } ?: return@withLock null
            if (!tokenStore.hasSession()) return@withLock null
            val refreshed = try {
                runBlocking(Dispatchers.IO) {
                    refreshApi.refresh(
                        RefreshRequestDto(refreshToken = refresh, platform = "android"),
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "HTTP 401 — access refresh failed", e)
                null
            } ?: return@withLock null
            if (!tokenStore.hasSession()) return@withLock null
            tokenStore.saveTokens(
                refreshed.accessToken,
                refreshed.refreshToken,
                refreshed.tokenType,
            )
            Log.i(TAG, "HTTP 401 — refreshed access token and retrying ${response.request.method} $path")
            withBearer(response.request, refreshed.accessToken)
        }
    }

    private fun bearer(request: Request): String? =
        request.header("Authorization")
            ?.removePrefix("Bearer ")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

    private fun withBearer(request: Request, accessToken: String): Request =
        request.newBuilder()
            .header("Authorization", "Bearer $accessToken")
            .build()

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    private companion object {
        const val TAG = "TokenRefresh"
    }
}
