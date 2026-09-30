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

    /**
     * Bound for a Profile Retry. Longer than startup so a slow but working
     * IPv4 path can finish, short enough that "Retrying…" cannot sit for a minute.
     */
    const val RETRY_TIMEOUT_MS = 30_000L

    const val ACCOUNT_DELETED = "account_deleted"

    private val ACCOUNT_DELETED_BODY = Regex(
        """"(?:error|code)"\s*:\s*"account_deleted"""",
        RegexOption.IGNORE_CASE,
    )

    enum class FailureKind {
        /** HTTP 401. Refresh, then retry. */
        UNAUTHORIZED,

        /** No HTTP response (DNS, connect, timeout). Do not refresh. */
        UNREACHABLE,

        /**
         * HTTP 404, or a JSON `error` / `code` of [ACCOUNT_DELETED], from
         * `/v1/me` or refresh. The account row is gone. Do not keep the session.
         */
        ACCOUNT_DELETED,

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

        /**
         * `/v1/me` or refresh said the account is gone (HTTP 404 or
         * [ACCOUNT_DELETED]). Caller drops the local session.
         */
        data object AccountDeleted : Outcome()
    }

    /**
     * True for HTTP 404 or a JSON body whose `error` or `code` is
     * [ACCOUNT_DELETED]. Harmless when the server never sends that signal.
     */
    fun isAccountDeletedSignal(statusCode: Int?, body: String?): Boolean {
        if (statusCode == 404) return true
        if (body.isNullOrBlank()) return false
        return ACCOUNT_DELETED_BODY.containsMatchIn(body)
    }

    /**
     * Whether [attempt]'s [outcome] may change the signed-in user.
     *
     * A rejected refresh always applies, including from an older attempt: the
     * refresh token is dead and the session must be dropped. A stale fallback
     * is ignored. An older server user is ignored only after a newer attempt
     * has already published one, so a late response cannot overwrite tier or quota.
     *
     * [newerRemotePublished] is true when an attempt newer than [attempt] has
     * already applied [Outcome.Remote].
     */
    fun shouldPublish(
        attempt: Int,
        latestAttempt: Int,
        outcome: Outcome,
        newerRemotePublished: Boolean,
    ): Boolean {
        if (outcome is Outcome.RefreshRejected || outcome is Outcome.AccountDeleted) return true
        if (attempt >= latestAttempt) return true
        return when (outcome) {
            is Outcome.LocalFallback -> false
            is Outcome.Remote -> !newerRemotePublished
            Outcome.RefreshRejected, Outcome.AccountDeleted -> true
        }
    }

    fun failureKind(error: Throwable): FailureKind {
        if (findErrorCode(error) == ACCOUNT_DELETED || findStatus(error) == 404) {
            return FailureKind.ACCOUNT_DELETED
        }
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
                when (failureKind(e)) {
                    FailureKind.UNREACHABLE ->
                        return Outcome.LocalFallback(localUserOrNull(localUser))
                    FailureKind.ACCOUNT_DELETED -> return Outcome.AccountDeleted
                    FailureKind.UNAUTHORIZED, FailureKind.OTHER -> Unit
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
            return when (failureKind(e)) {
                FailureKind.UNAUTHORIZED -> Outcome.RefreshRejected
                FailureKind.ACCOUNT_DELETED -> Outcome.AccountDeleted
                FailureKind.UNREACHABLE, FailureKind.OTHER ->
                    Outcome.LocalFallback(localUserOrNull(localUser))
            }
        }
        val user = try {
            me(refreshed.accessToken)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (failureKind(e) == FailureKind.ACCOUNT_DELETED) return Outcome.AccountDeleted
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

    private fun findErrorCode(error: Throwable): String? {
        var cur: Throwable? = error
        while (cur != null) {
            if (cur is AuthStatusException && !cur.errorCode.isNullOrBlank()) return cur.errorCode
            cur = cur.cause
        }
        return null
    }
}

/** HTTP status from an auth call, or null when the server never answered. */
class AuthStatusException(
    val statusCode: Int?,
    cause: Throwable? = null,
    val errorCode: String? = null,
) : Exception(
    if (statusCode != null) "HTTP $statusCode" else cause?.message,
    cause,
)
