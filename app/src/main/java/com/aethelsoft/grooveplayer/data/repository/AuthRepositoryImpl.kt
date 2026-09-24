package com.aethelsoft.grooveplayer.data.repository

import android.app.Activity
import android.util.Log
import com.aethelsoft.grooveplayer.data.auth.GoogleIdTokenProvider
import com.aethelsoft.grooveplayer.data.auth.SecureTokenStore
import com.aethelsoft.grooveplayer.data.mapper.AuthMapper
import com.aethelsoft.grooveplayer.data.remote.api.AuthApi
import com.aethelsoft.grooveplayer.data.remote.dto.GoogleAuthRequestDto
import com.aethelsoft.grooveplayer.data.remote.dto.LogoutRequestDto
import com.aethelsoft.grooveplayer.data.remote.dto.RefreshRequestDto
import com.aethelsoft.grooveplayer.domain.model.AuthUser
import com.aethelsoft.grooveplayer.domain.model.PrivilegeTier
import com.aethelsoft.grooveplayer.domain.repository.AuthRepository
import com.aethelsoft.grooveplayer.domain.repository.UserRepository
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val tokenStore: SecureTokenStore,
    private val googleIdTokenProvider: GoogleIdTokenProvider,
    private val userRepository: UserRepository,
) : AuthRepository {

    private val mutex = Mutex()
    private val _authUser = MutableStateFlow<AuthUser?>(null)
    private val _serverSyncError = MutableStateFlow<String?>(null)

    override fun observeAuthUser(): Flow<AuthUser?> = _authUser.asStateFlow()

    override fun observePrivilegeTier(): Flow<PrivilegeTier> =
        _authUser.map { it?.privilegeTier ?: PrivilegeTier.FREE }

    override fun observeServerSyncError(): Flow<String?> = _serverSyncError.asStateFlow()

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
        runCatching {
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
            user
        }.onFailure { e ->
            Log.e(TAG, "Refresh failed", e)
            _serverSyncError.value = humanizeNetworkError(e)
            if (e is HttpException && e.code() == 401) {
                tokenStore.clear()
                _authUser.value = null
            }
        }
    }

    override suspend fun restoreSession(): Result<AuthUser?> = mutex.withLock {
        runCatching {
            if (!tokenStore.hasSession()) {
                // No tokens: signed-out Free. Drop a leftover Room profile so a later
                // process start cannot rebuild Premium from cached /v1/me.
                _authUser.value = null
                _serverSyncError.value = null
                if (userRepository.getUserProfile() != null) {
                    userRepository.deleteUserProfile()
                }
                return@runCatching null
            }
            val access = tokenStore.getAccessToken()
            if (!access.isNullOrBlank()) {
                try {
                    val me = authApi.me("Bearer $access")
                    val user = AuthMapper.toDomainUser(me)
                    persistLocalProfile(user)
                    _authUser.value = user
                    _serverSyncError.value = null
                    return@runCatching user
                } catch (e: Exception) {
                    Log.w(TAG, "/v1/me failed, trying refresh", e)
                    if (isUnreachable(e)) {
                        _serverSyncError.value = humanizeNetworkError(e)
                        throw e
                    }
                }
            }
            val refresh = tokenStore.getRefreshToken() ?: return@runCatching null
            val response = authApi.refresh(
                RefreshRequestDto(refreshToken = refresh, platform = "android")
            )
            val tokens = AuthMapper.toTokens(response)
            tokenStore.saveTokens(tokens.accessToken, tokens.refreshToken, tokens.tokenType)
            val user = AuthMapper.toDomainUser(response.user)
            persistLocalProfile(user)
            _authUser.value = user
            _serverSyncError.value = null
            user
        }.recoverCatching { e ->
            Log.e(TAG, "restoreSession failed", e)
            _serverSyncError.value = humanizeNetworkError(e)
            // Offline only: seed identity from Room when we have tokens.
            // Do NOT invent storage here — Room has no used/quota. Prefer the
            // in-memory user (may still be stale) over a storage-less rewrite
            // that would flash empty usage then stick Premium from disk.
            val current = _authUser.value
            if (current != null && tokenStore.hasSession()) {
                return@recoverCatching current
            }
            val local = userRepository.getUserProfile()
            if (local != null && tokenStore.hasSession()) {
                val user = AuthUser(
                    id = local.id,
                    email = local.email.ifBlank { null },
                    displayName = local.username,
                    avatarUrl = local.profilePictureUrl,
                    privilegeTier = local.privilegeTier,
                    storage = null,
                )
                _authUser.value = user
                user
            } else {
                null
            }
        }
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
    }

    companion object {
        private const val TAG = "AuthRepository"

        fun isUnreachable(e: Throwable): Boolean {
            var cur: Throwable? = e
            while (cur != null) {
                when (cur) {
                    is UnknownHostException,
                    is ConnectException,
                    is SocketTimeoutException,
                    is IOException -> {
                        val msg = cur.message.orEmpty()
                        if (cur is IOException && "cleartext" in msg.lowercase()) return true
                        if (cur is IOException || cur is UnknownHostException ||
                            cur is ConnectException || cur is SocketTimeoutException
                        ) {
                            return true
                        }
                    }
                }
                cur = cur.cause
            }
            return e.message?.contains("failed to connect", ignoreCase = true) == true
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
