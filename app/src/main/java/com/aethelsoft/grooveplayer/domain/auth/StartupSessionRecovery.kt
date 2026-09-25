package com.aethelsoft.grooveplayer.domain.auth

import com.aethelsoft.grooveplayer.domain.model.AuthUser
import kotlinx.coroutines.CancellationException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Cold-start `/v1/me` recovery.
 *
 * A 401 means the access token was rejected. That is not "offline": refresh,
 * then retry `/v1/me`. If refresh fails, return the local profile so Home can
 * render. This function does not touch tokens or the UI.
 */
object StartupSessionRecovery {

    /** Bound for the startup network attempt. Past this, use the local profile. */
    const val NETWORK_TIMEOUT_MS = 12_000L

    enum class FailureKind {
        /** HTTP 401. Refresh, then retry. */
        UNAUTHORIZED,

        /** No HTTP response (DNS, connect, timeout). Do not refresh. */
        UNREACHABLE,

        /** Any other failure, including HTTP 5xx. Refresh once, then fall back. */
        OTHER,
    }

    data class RefreshedSession(
        val accessToken: String,
        val refreshToken: String,
        val user: AuthUser,
    )

    sealed class Outcome {
        data class Remote(val user: AuthUser) : Outcome()

        /** Tokens stay. [user] is the Room profile, or null when Room has none. */
        data class LocalFallback(val user: AuthUser?) : Outcome()

        /** Refresh token was rejected. Caller drops the local session. */
        data object RefreshRejected : Outcome()
    }

    fun failureKind(error: Throwable): FailureKind {
        val status = findStatus(error)
        if (status == 401) return FailureKind.UNAUTHORIZED
        if (status != null) return FailureKind.OTHER
        if (isConnectivityFailure(error)) return FailureKind.UNREACHABLE
        return FailureKind.OTHER
    }

    /**
     * True only when nothing answered. An HTTP status anywhere in the cause
     * chain means the server was reached — including a 401 wrapped in
     * [IOException], which must not skip refresh.
     */
    fun isConnectivityFailure(error: Throwable): Boolean {
        if (findStatus(error) != null) return false
        var cur: Throwable? = error
        while (cur != null) {
            when (cur) {
                is UnknownHostException,
                is ConnectException,
                is SocketTimeoutException,
                is IOException,
                -> return true
            }
            cur = cur.cause
        }
        return error.message?.contains("failed to connect", ignoreCase = true) == true
    }

    suspend fun restore(
        accessToken: String?,
        refreshToken: suspend () -> String?,
        me: suspend (accessToken: String) -> AuthUser,
        refresh: suspend (refreshToken: String) -> RefreshedSession,
        localUser: suspend () -> AuthUser?,
    ): Outcome {
        // Read again at refresh time so a 401 authenticator that already rotated
        // the refresh token is not followed by a second call with the stale one.
        if (refreshToken().isNullOrBlank()) {
            return Outcome.LocalFallback(null)
        }
        if (!accessToken.isNullOrBlank()) {
            try {
                return Outcome.Remote(me(accessToken))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (failureKind(e) == FailureKind.UNREACHABLE) {
                    return Outcome.LocalFallback(localUserOrNull(localUser))
                }
            }
        }
        val latestRefresh = refreshToken()?.takeIf { it.isNotBlank() }
            ?: return Outcome.RefreshRejected
        val refreshed = try {
            refresh(latestRefresh)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (failureKind(e) == FailureKind.UNAUTHORIZED) {
                return Outcome.RefreshRejected
            }
            return Outcome.LocalFallback(localUserOrNull(localUser))
        }
        val user = try {
            me(refreshed.accessToken)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Refresh already returned a user. A failed retry must not blank Home.
            refreshed.user
        }
        return Outcome.Remote(user)
    }

    private suspend fun localUserOrNull(localUser: suspend () -> AuthUser?): AuthUser? =
        try {
            localUser()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }

    private fun findStatus(error: Throwable): Int? {
        var cur: Throwable? = error
        while (cur != null) {
            if (cur is AuthStatusException && cur.statusCode != null) return cur.statusCode
            cur = cur.cause
        }
        return null
    }
}

/** HTTP status from an auth call, or null when the server never answered. */
class AuthStatusException(
    val statusCode: Int?,
    cause: Throwable? = null,
) : Exception(
    if (statusCode != null) "HTTP $statusCode" else cause?.message,
    cause,
)
