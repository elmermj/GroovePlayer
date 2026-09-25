package com.aethelsoft.grooveplayer.domain.usecase.ads_category

import android.app.Activity
import com.aethelsoft.grooveplayer.domain.model.AuthUser
import com.aethelsoft.grooveplayer.domain.model.PrivilegeTier
import com.aethelsoft.grooveplayer.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Startup and banner ads stay off until a confirmed Free entitlement.
 * The post-restore restart leaves the in-memory user null, which auth maps to Free;
 * that gap must not show an interstitial.
 */
class AdGateTest {

    @Test
    fun unknownOrLoadingShowsNoAd() {
        assertFalse(AdGate.shouldShowAds(entitlementResolved = false, PrivilegeTier.FREE))
        assertFalse(AdGate.shouldShowAds(entitlementResolved = false, PrivilegeTier.BASIC))
        assertFalse(AdGate.shouldShowAds(entitlementResolved = false, PrivilegeTier.PREMIUM))
        assertFalse(AdGate.isEntitlementConfirmed(userPresent = false, hasSessionToken = true))
    }

    @Test
    fun premiumShowsNoAd() = runBlocking {
        assertFalse(AdGate.shouldShowAds(entitlementResolved = true, PrivilegeTier.PREMIUM))
        assertFalse(AdGate.shouldShowAds(entitlementResolved = true, PrivilegeTier.BASIC))

        val auth = FakeAuthRepository(initialTier = PrivilegeTier.PREMIUM, accessToken = "access")
        val readiness = AdEntitlementReadiness()
        PublishAdEntitlementUseCase(auth, readiness).invoke()
        val show = ShouldShowAdsUseCase(auth, readiness)

        assertTrue(readiness.isResolved())
        assertFalse(show.canShowNow())
        assertEquals(false, show.observeDecision().first())
        assertFalse(show().first())
    }

    @Test
    fun confirmedFreeAllowsAds() = runBlocking {
        assertTrue(AdGate.shouldShowAds(entitlementResolved = true, PrivilegeTier.FREE))
        assertTrue(AdGate.isEntitlementConfirmed(userPresent = true, hasSessionToken = false))
        assertTrue(AdGate.isEntitlementConfirmed(userPresent = false, hasSessionToken = false))

        val signedInFree = ShouldShowAdsUseCase(
            FakeAuthRepository(initialTier = PrivilegeTier.FREE),
            AdEntitlementReadiness().also { it.markResolved() },
        )
        assertTrue(signedInFree.canShowNow())
        assertEquals(true, signedInFree.observeDecision().first())

        val signedOut = FakeAuthRepository(initialTier = null)
        val signedOutReady = AdEntitlementReadiness()
        PublishAdEntitlementUseCase(signedOut, signedOutReady).invoke()
        val signedOutAds = ShouldShowAdsUseCase(signedOut, signedOutReady)
        assertTrue(signedOutReady.isResolved())
        assertTrue(signedOutAds.canShowNow())
        assertEquals(true, signedOutAds.observeDecision().first())
    }

    @Test
    fun loadingLooksLikeFreeUntilSessionRestoreFinishes() = runBlocking {
        val auth = FakeAuthRepository(initialTier = null, accessToken = "access")
        val readiness = AdEntitlementReadiness()
        val show = ShouldShowAdsUseCase(auth, readiness)

        assertEquals(PrivilegeTier.FREE, auth.currentPrivilegeTier())
        assertNull(show.observeDecision().first())
        assertFalse(show.canShowNow())
        assertFalse(show().first())
    }

    @Test
    fun sessionTokenWithoutUserStaysClosedUntilPremiumLoads() = runBlocking {
        val auth = FakeAuthRepository(initialTier = null, accessToken = "access")
        val readiness = AdEntitlementReadiness()
        val show = ShouldShowAdsUseCase(auth, readiness)
        val job = launch { PublishAdEntitlementUseCase(auth, readiness).invoke() }
        try {
            yield()
            assertFalse(readiness.isResolved())
            assertFalse(show.canShowNow())
            auth.setTier(PrivilegeTier.PREMIUM)
        } finally {
            job.join()
        }
        assertTrue(readiness.isResolved())
        assertFalse(show.canShowNow())
        assertEquals(false, show.observeDecision().first())
    }

    @Test
    fun unreadableSessionIsNotConfirmedFree() = runBlocking {
        val auth = FakeAuthRepository(initialTier = null, throwOnToken = true)
        val readiness = AdEntitlementReadiness()
        val job = launch { PublishAdEntitlementUseCase(auth, readiness).invoke() }
        try {
            yield()
            assertFalse(readiness.isResolved())
            assertFalse(ShouldShowAdsUseCase(auth, readiness).canShowNow())
        } finally {
            job.cancel()
        }
    }
}

private class FakeAuthRepository(
    initialTier: PrivilegeTier?,
    private val accessToken: String? = null,
    private val refreshToken: String? = null,
    private val throwOnToken: Boolean = false,
) : AuthRepository {
    private val user = MutableStateFlow(initialTier?.let(::authUser))

    fun setTier(tier: PrivilegeTier) {
        user.value = authUser(tier)
    }

    override fun observeAuthUser(): Flow<AuthUser?> = user.asStateFlow()

    override fun observePrivilegeTier(): Flow<PrivilegeTier> =
        user.map { it?.privilegeTier ?: PrivilegeTier.FREE }

    override fun currentPrivilegeTier(): PrivilegeTier =
        user.value?.privilegeTier ?: PrivilegeTier.FREE

    override fun observeServerSyncError(): Flow<String?> = MutableStateFlow(null)

    override suspend fun getAuthUser(): AuthUser? = user.value

    override suspend fun isSignedIn(): Boolean = false

    override suspend fun signInWithGoogle(activity: Activity): Result<AuthUser> = error("unused")

    override suspend fun signOut(): Result<Unit> = error("unused")

    override suspend fun deleteAccount(): Result<Unit> = error("unused")

    override suspend fun refreshSession(): Result<AuthUser> = error("unused")

    override suspend fun restoreSession(boundByStartupTimeout: Boolean): Result<AuthUser?> =
        Result.success(user.value)

    override suspend fun getAccessToken(): String? = token(accessToken)

    override suspend fun getRefreshToken(): String? = token(refreshToken)

    override suspend fun applyRemoteUser(user: AuthUser) {
        this.user.value = user
    }

    private fun token(value: String?): String? {
        if (throwOnToken) error("token store unavailable")
        return value
    }

    private fun authUser(tier: PrivilegeTier) = AuthUser(
        id = "user",
        email = null,
        displayName = null,
        avatarUrl = null,
        privilegeTier = tier,
    )
}
