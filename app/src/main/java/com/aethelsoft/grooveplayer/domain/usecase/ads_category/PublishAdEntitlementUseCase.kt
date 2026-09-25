package com.aethelsoft.grooveplayer.domain.usecase.ads_category

import com.aethelsoft.grooveplayer.domain.repository.AuthRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Opens [AdEntitlementReadiness] after the startup [AuthRepository.restoreSession]
 * call. Does not refresh tokens or change backup/restore.
 *
 * Stays closed when a session token exists but no user was loaded (`/v1/me` still
 * in flight, or it failed with nothing cached). A later user publication opens it.
 */
class PublishAdEntitlementUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val readiness: AdEntitlementReadiness,
) {
    suspend operator fun invoke() {
        if (!isConfirmedNow()) {
            authRepository.observeAuthUser().first { it != null }
        }
        readiness.markResolved()
    }

    private suspend fun isConfirmedNow(): Boolean =
        AdGate.isEntitlementConfirmed(
            userPresent = authRepository.getAuthUser() != null,
            hasSessionToken = hasSessionToken(),
        )

    /** Token-store failures stay closed: an unreadable session is not confirmed Free. */
    private suspend fun hasSessionToken(): Boolean = try {
        !authRepository.getAccessToken().isNullOrBlank() ||
            !authRepository.getRefreshToken().isNullOrBlank()
    } catch (_: Exception) {
        true
    }
}
