package com.aethelsoft.grooveplayer.domain.repository

import android.app.Activity
import com.aethelsoft.grooveplayer.domain.model.AuthUser
import com.aethelsoft.grooveplayer.domain.model.PrivilegeTier
import kotlinx.coroutines.flow.Flow

/**
 * Google Sign-In + session against grooveplayer-backend.
 */
interface AuthRepository {
    fun observeAuthUser(): Flow<AuthUser?>
    fun observePrivilegeTier(): Flow<PrivilegeTier>
    /** In-memory tier. Free when signed out. Same source as [observePrivilegeTier]. */
    fun currentPrivilegeTier(): PrivilegeTier
    /** Non-null when last /v1/me (or auth) call failed to reach the server. */
    fun observeServerSyncError(): Flow<String?>
    suspend fun getAuthUser(): AuthUser?
    suspend fun isSignedIn(): Boolean
    suspend fun signInWithGoogle(activity: Activity): Result<AuthUser>
    suspend fun signOut(): Result<Unit>
    /** Server hard-delete then local sign-out. Play Store requirement for Sign-In apps. */
    suspend fun deleteAccount(): Result<Unit>
    suspend fun refreshSession(): Result<AuthUser>
    /**
     * Load `/v1/me` (tier, quota, storage). [boundByStartupTimeout] uses the
     * 12s startup cap. Profile Retry passes false and is capped at
     * [com.aethelsoft.grooveplayer.domain.auth.StartupSessionRecovery.RETRY_TIMEOUT_MS].
     */
    suspend fun restoreSession(boundByStartupTimeout: Boolean = true): Result<AuthUser?>
    suspend fun getAccessToken(): String?
    suspend fun getRefreshToken(): String?

    /** Apply /v1/me-shaped user after billing verify (or forced me refresh). */
    suspend fun applyRemoteUser(user: AuthUser)
}
