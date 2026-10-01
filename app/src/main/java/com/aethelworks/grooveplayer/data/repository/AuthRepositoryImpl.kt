package com.aethelworks.grooveplayer.data.repository

import android.app.Activity
import android.util.Log
import com.aethelworks.grooveplayer.data.auth.GoogleIdTokenProvider
import com.aethelworks.grooveplayer.data.auth.SecureTokenStore
import com.aethelworks.grooveplayer.data.auth.ServerAccessGate
import com.aethelworks.grooveplayer.data.mapper.AuthMapper
import com.aethelworks.grooveplayer.data.remote.api.AuthApi
import com.aethelworks.grooveplayer.data.remote.dto.GoogleAuthRequestDto
import com.aethelworks.grooveplayer.data.remote.dto.LogoutRequestDto
import com.aethelworks.grooveplayer.data.remote.dto.RefreshRequestDto
import com.aethelworks.grooveplayer.domain.auth.AuthStatusException
import com.aethelworks.grooveplayer.domain.auth.BackendCallGuard
import com.aethelworks.grooveplayer.domain.auth.SessionLoadPurpose
import com.aethelworks.grooveplayer.domain.auth.StartupSessionRecovery
import com.aethelworks.grooveplayer.domain.model.AuthUser
import com.aethelworks.grooveplayer.domain.model.PrivilegeTier
import com.aethelworks.grooveplayer.domain.repository.AuthRepository
import com.aethelworks.grooveplayer.domain.repository.UserRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import retrofit2.HttpException
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val tokenStore: SecureTokenStore,
    private val googleIdTokenProvider: GoogleIdTokenProvider,
    private val userRepository: UserRepository,
    private val serverAccess: ServerAccessGate,
) : AuthRepository {

    private val mutex = Mutex()
    /** Bumped when the session is cleared so an in-flight startup refresh cannot restore it. */
    private val sessionGeneration = AtomicInteger(0)
    /** Bumped per restore so a timed-out startup fallback cannot clobber a newer `/v1/me`. */
    private val restoreAttempt = AtomicInteger(0)
    /** Highest attempt that has already applied a server `/v1/me` user. */
    private val publishedRemoteAttempt = AtomicInteger(0)
    private val _authUser = MutableStateFlow<AuthUser?>(null)
    private val _serverSyncError = MutableStateFlow<String?>(null)
    private val _accountRemoved = MutableStateFlow(false)
    /** Credential Manager clear must not join startup. Binder calls can stall. */
    private val credentialClearScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun observeAuthUser(): Flow<AuthUser?> = _authUser.asStateFlow()

    override fun observePrivilegeTier(): Flow<PrivilegeTier> =
        _authUser.map { it?.privilegeTier ?: PrivilegeTier.FREE }

    override fun currentPrivilegeTier(): PrivilegeTier =
        _authUser.value?.privilegeTier ?: PrivilegeTier.FREE

    override fun observeServerSyncError(): Flow<String?> = _serverSyncError.asStateFlow()

    override fun observeAccountRemoved(): Flow<Boolean> = _accountRemoved.asStateFlow()

    override fun acknowledgeAccountRemoved() {
        _accountRemoved.value = false
    }

    override suspend fun getAuthUser(): AuthUser? = _authUser.value

    override suspend fun isSignedIn(): Boolean = tokenStore.hasSession() && _authUser.value != null

    override suspend fun getAccessToken(): String? = tokenStore.getAccessToken()

    override suspend fun getRefreshToken(): String? = tokenStore.getRefreshToken()

    override suspend fun signInWithGoogle(activity: Activity): Result<AuthUser> = mutex.withLock {
        runCatching {
            val idToken = googleIdTokenProvider.requestIdToken(activity)
            val response = authApi.signInWithGoogle(
                GoogleAuthRequestDto(idToken = idToken, platform = "android")
            )
            val tokens = AuthMapper.toTokens(response)
            tokenStore.saveTokens(tokens.accessToken, tokens.refreshToken, tokens.tokenType)
            val user = AuthMapper.toDomainUser(response.user)
            persistLocalProfile(user)
            _authUser.value = user
            _serverSyncError.value = null
            publishServerAccess()
            user
        }.onFailure { e ->
            Log.e(TAG, "Google sign-in failed", e)
            _serverSyncError.value = humanizeNetworkError(e)
        }
    }

    override suspend fun signOut(): Result<Unit> = withContext(NonCancellable) {
        mutex.withLock {
            // POST /v1/auth/logout with Bearer access (interceptor) + refresh body.
            // all_devices:true revokes every refresh for this user.
            // Access JWT is NOT server-blacklisted — local tokens must still go.
            // API failure must not keep Premium, ads-off, or the Google session.
            try {
                postLogout()
            } catch (e: Exception) {
                Log.w(TAG, "Logout API failed (clearing local session anyway)", e)
            }
            clearLocalSession()
            Result.success(Unit)
        }
    }

    /**
     * Bearer comes from the OkHttp interceptor when an access token is present.
     * Refresh is sent in the body so the server can revoke it (and every device).
     */
    private suspend fun postLogout() {
        val access = tokenStore.getAccessToken()
        val refresh = tokenStore.getRefreshToken()
        if (access.isNullOrBlank() && refresh.isNullOrBlank()) return
        authApi.logout(
            LogoutRequestDto(
                refreshToken = refresh?.takeIf { it.isNotBlank() },
                allDevices = true,
            )
        )
    }

    /**
     * Drops access, refresh, Credential Manager session, Room profile, and the
     * in-memory /v1/me user. Each step is isolated so one failure cannot leave
     * privilege on Premium. Caller must hold [mutex].
     */
    private suspend fun clearLocalSession() {
        sessionGeneration.incrementAndGet()
        try {
            tokenStore.clear()
        } catch (e: Exception) {
            Log.w(TAG, "token clear failed", e)
        }
        try {
            googleIdTokenProvider.clearCredentialSession()
        } catch (e: Exception) {
            Log.w(TAG, "clearCredentialState failed", e)
        }
        try {
            userRepository.deleteUserProfile()
        } catch (e: Exception) {
            Log.w(TAG, "profile delete failed", e)
        }
        _authUser.value = null
        _serverSyncError.value = null
        publishServerAccess()
    }

    override suspend fun deleteAccount(): Result<Unit> = mutex.withLock {
        runCatching {
            // DELETE /v1/account (Benny): 200 {"deleted":true[, "already_gone":true]}.
            // Clear session only on 200. On 500/401 leave tokens so the user can retry.
            // Live on Fly (Benny). Clear session only on 200.
            val response = authApi.deleteAccount()
            when (response.code()) {
                200 -> {
                    val body = response.body()
                    if (body?.deleted != true) {
                        error("Delete account returned 200 without deleted=true")
                    }
                    // already_gone is still success — same local wipe as Sign-out.
                    withContext(NonCancellable) {
                        clearLocalSession()
                    }
                }
                401 -> error("Session expired. Sign in again, then retry delete.")
                500 -> error(
                    "Server couldn't finish deleting (cloud backup or database). " +
                        "Your session is still active — try again.",
                )
                else -> error(
                    "Delete account failed (${response.code()})" +
                        (response.errorBody()?.string()?.takeIf { it.isNotBlank() }?.let { ": $it" } ?: ""),
                )
            }
        }.onFailure { e ->
            Log.e(TAG, "deleteAccount failed", e)
        }
    }

    override suspend fun refreshSession(): Result<AuthUser> = mutex.withLock {
        if (!tokenStore.hasSession()) {
            return@withLock Result.failure(IllegalStateException("No refresh token"))
        }
        val tier = _authUser.value?.privilegeTier ?: PrivilegeTier.FREE
        if (!BackendCallGuard.allowsPaidApis(hasSession = true, tier = tier)) {
            return@withLock Result.failure(IllegalStateException("Server refresh is not used for Free."))
        }
        publishServerAccess()
        try {
            val refresh = tokenStore.getRefreshToken()
                ?: error("No refresh token")
            val response = authApi.refresh(
                RefreshRequestDto(refreshToken = refresh, platform = "android")
            )
            val tokens = AuthMapper.toTokens(response)
            tokenStore.saveTokens(tokens.accessToken, tokens.refreshToken, tokens.tokenType)
            val user = AuthMapper.toDomainUser(response.user)
            persistLocalProfile(user)
            _authUser.value = user
            _serverSyncError.value = null
            publishServerAccess()
            Result.success(user)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Refresh failed", e)
            when (StartupSessionRecovery.failureKind(asAuthStatus(e))) {
                StartupSessionRecovery.FailureKind.UNAUTHORIZED,
                StartupSessionRecovery.FailureKind.ACCOUNT_DELETED,
                -> dropRejectedSession()
                else -> _serverSyncError.value = humanizeNetworkError(e)
            }
            Result.failure(e)
        }
    }

    override suspend fun restoreSession(
        boundByStartupTimeout: Boolean,
        purpose: SessionLoadPurpose,
    ): Result<AuthUser?> {
        // Network runs outside the mutex. Holding it across /v1/me deadlocks
        // the first frame if the UI thread waits on the same lock.
        val generation = mutex.withLock {
            if (!tokenStore.hasSession()) {
                // No tokens: signed-out Free. Drop a leftover Room profile so a later
                // process start cannot rebuild Premium from cached /v1/me.
                _authUser.value = null
                _serverSyncError.value = null
                publishServerAccess()
                if (userRepository.getUserProfile() != null) {
                    userRepository.deleteUserProfile()
                }
                return@withLock null
            }
            publishServerAccess()
            sessionGeneration.get()
        }
        if (generation == null) return Result.success(null)

        val tier = _authUser.value?.privilegeTier ?: PrivilegeTier.FREE
        if (!BackendCallGuard.allowSessionLoad(purpose, hasSession = true, tier = tier)) {
            return Result.success(_authUser.value)
        }

        val attempt = restoreAttempt.incrementAndGet()
        val outcome = try {
            serverAccess.probe {
                val recover = suspend {
                    StartupSessionRecovery.restore(
                        accessToken = tokenStore.getAccessToken(),
                        refreshToken = { tokenStore.getRefreshToken() },
                        me = { token -> fetchMe(token) },
                        refresh = { token -> refreshForStartup(token, generation) },
                        localUser = { localAuthUser() },
                    )
                }
                val cap = if (boundByStartupTimeout) {
                    StartupSessionRecovery.NETWORK_TIMEOUT_MS
                } else {
                    StartupSessionRecovery.RETRY_TIMEOUT_MS
                }
                withTimeoutOrNull(cap) { recover() }
                    ?: StartupSessionRecovery.Outcome.LocalFallback(localAuthUser())
            }
        } catch (e: CancellationException) {
            throw e
        }

        return mutex.withLock {
            val newerRemotePublished = publishedRemoteAttempt.get() > attempt
            if (sessionGeneration.get() != generation || !tokenStore.hasSession()) {
                Result.success(_authUser.value)
            } else if (
                !StartupSessionRecovery.shouldPublish(
                    attempt,
                    restoreAttempt.get(),
                    outcome,
                    newerRemotePublished,
                )
            ) {
                // Stale fallback, or an older /v1/me after a newer one was published.
                Result.success(_authUser.value)
            } else {
                if (outcome is StartupSessionRecovery.Outcome.Remote) {
                    publishedRemoteAttempt.updateAndGet { published -> maxOf(published, attempt) }
                }
                applyStartupOutcome(outcome)
            }
        }
    }

    private suspend fun fetchMe(accessToken: String): AuthUser {
        try {
            return AuthMapper.toDomainUser(authApi.me("Bearer $accessToken"))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "/v1/me failed", e)
            throw asAuthStatus(e)
        }
    }

    private suspend fun refreshForStartup(
        refreshToken: String,
        generation: Int,
    ): StartupSessionRecovery.RefreshedSession {
        Log.i(TAG, "Refreshing access token after /v1/me failure")
        try {
            val response = authApi.refresh(
                RefreshRequestDto(refreshToken = refreshToken, platform = "android"),
            )
            val tokens = AuthMapper.toTokens(response)
            val user = AuthMapper.toDomainUser(response.user)
            val saved = mutex.withLock {
                if (sessionGeneration.get() != generation || !tokenStore.hasSession()) {
                    false
                } else {
                    tokenStore.saveTokens(tokens.accessToken, tokens.refreshToken, tokens.tokenType)
                    true
                }
            }
            if (!saved) throw CancellationException("session cleared during refresh")
            return StartupSessionRecovery.RefreshedSession(
                accessToken = tokens.accessToken,
                refreshToken = tokens.refreshToken,
                user = user,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Startup token refresh failed", e)
            throw asAuthStatus(e)
        }
    }

    private suspend fun applyStartupOutcome(
        outcome: StartupSessionRecovery.Outcome,
    ): Result<AuthUser?> {
        return when (outcome) {
            is StartupSessionRecovery.Outcome.Remote -> {
                persistLocalProfile(outcome.user)
                _authUser.value = outcome.user
                _serverSyncError.value = null
                publishServerAccess()
                Result.success(outcome.user)
            }
            is StartupSessionRecovery.Outcome.LocalFallback -> {
                val synced = _authUser.value
                // A /v1/me that already returned quota must not be replaced by a
                // slower startup fallback. That hid Cloud backup after Retry.
                if (_serverSyncError.value == null && synced?.storage != null) {
                    publishServerAccess()
                    return Result.success(synced)
                }
                _serverSyncError.value =
                    "Can't reach server. Check Wi‑Fi or API URL, then retry."
                val user = outcome.user ?: synced
                if (user != null) {
                    _authUser.value = user
                }
                publishServerAccess()
                Result.success(user)
            }
            StartupSessionRecovery.Outcome.RefreshRejected,
            StartupSessionRecovery.Outcome.AccountDeleted,
            -> {
                Log.w(TAG, "Session rejected — signing out locally")
                dropRejectedSession()
                Result.success(null)
            }
        }
    }

    /**
     * Refresh was rejected or the account is gone. Drop tokens and the cached
     * profile so Home stays usable. Credential Manager is cleared on a
     * background scope with a short timeout so a binder call cannot block startup.
     * The removed-account message is raised only when a session existed.
     */
    private suspend fun dropRejectedSession() {
        val hadSession = tokenStore.hasSession() || _authUser.value != null
        sessionGeneration.incrementAndGet()
        try {
            tokenStore.clear()
        } catch (e: Exception) {
            Log.w(TAG, "token clear failed", e)
        }
        try {
            userRepository.deleteUserProfile()
        } catch (e: Exception) {
            Log.w(TAG, "profile delete failed", e)
        }
        _authUser.value = null
        _serverSyncError.value = null
        publishServerAccess()
        if (hadSession) {
            _accountRemoved.value = true
            clearCredentialSessionInBackground()
        }
    }

    private fun clearCredentialSessionInBackground() {
        credentialClearScope.launch {
            try {
                withTimeout(CREDENTIAL_CLEAR_TIMEOUT_MS) {
                    googleIdTokenProvider.clearCredentialSession()
                }
            } catch (e: TimeoutCancellationException) {
                Log.w(TAG, "clearCredentialState timed out", e)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "clearCredentialState failed", e)
            }
        }
    }

    private suspend fun localAuthUser(): AuthUser? {
        val current = _authUser.value
        if (current != null && tokenStore.hasSession()) return current
        val local = userRepository.getUserProfile() ?: return null
        if (!tokenStore.hasSession()) return null
        return AuthUser(
            id = local.id,
            email = local.email.ifBlank { null },
            displayName = local.username,
            avatarUrl = local.profilePictureUrl,
            privilegeTier = local.privilegeTier,
            storage = null,
        )
    }

    private fun asAuthStatus(error: Throwable): AuthStatusException {
        if (error is AuthStatusException) return error
        val status = httpStatus(error)
        val body = (error as? HttpException)?.let { http ->
            try {
                http.response()?.errorBody()?.string()
            } catch (_: Exception) {
                null
            }
        }
        val code = if (StartupSessionRecovery.isAccountDeletedSignal(status, body)) {
            StartupSessionRecovery.ACCOUNT_DELETED
        } else {
            null
        }
        return AuthStatusException(status, error, code)
    }

    private fun publishServerAccess() {
        serverAccess.publish(
            hasSession = tokenStore.hasSession(),
            tier = _authUser.value?.privilegeTier ?: PrivilegeTier.FREE,
        )
    }

    private fun httpStatus(error: Throwable): Int? {
        var cur: Throwable? = error
        while (cur != null) {
            when (cur) {
                is HttpException -> return cur.code()
                is AuthStatusException -> if (cur.statusCode != null) return cur.statusCode
            }
            cur = cur.cause
        }
        return null
    }

    private suspend fun persistLocalProfile(user: AuthUser) {
        val existing = userRepository.getUserProfile()
        val now = System.currentTimeMillis()
        val profile = AuthMapper.toUserProfile(user, now = existing?.createdAt ?: now).copy(
            updatedAt = now,
            settingsReferences = existing?.settingsReferences.orEmpty(),
        )
        userRepository.saveUserProfile(profile)
    }


    override suspend fun applyRemoteUser(user: AuthUser) = mutex.withLock {
        // Backup/billing responses can arrive after Sign-out. Applying them would
        // put Premium back on screen with no tokens and ads still off.
        if (!tokenStore.hasSession()) {
            Log.w(TAG, "Ignoring remote user; local session already cleared")
            return@withLock
        }
        persistLocalProfile(user)
        _authUser.value = user
        _serverSyncError.value = null
        publishServerAccess()
    }

    companion object {
        private const val TAG = "AuthRepository"
        private const val CREDENTIAL_CLEAR_TIMEOUT_MS = 3_000L

        fun isUnreachable(e: Throwable): Boolean {
            // An HTTP status means the server answered. 401 used to be treated as
            // offline when it was wrapped in IOException, so startup never refreshed.
            if (httpStatusOf(e) != null) return false
            return StartupSessionRecovery.isConnectivityFailure(e)
        }

        private fun httpStatusOf(error: Throwable): Int? {
            var cur: Throwable? = error
            while (cur != null) {
                when (cur) {
                    is HttpException -> return cur.code()
                    is AuthStatusException -> if (cur.statusCode != null) return cur.statusCode
                }
                cur = cur.cause
            }
            return null
        }

        fun humanizeNetworkError(e: Throwable): String {
            return if (isUnreachable(e)) {
                "Can't reach server. Check Wi‑Fi or API URL, then retry."
            } else {
                e.message?.takeIf { it.isNotBlank() } ?: "Server error"
            }
        }
    }
}
