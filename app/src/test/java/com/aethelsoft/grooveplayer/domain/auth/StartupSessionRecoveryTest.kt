package com.aethelsoft.grooveplayer.domain.auth

import com.aethelsoft.grooveplayer.domain.model.AuthUser
import com.aethelsoft.grooveplayer.domain.model.PrivilegeTier
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class StartupSessionRecoveryTest {

    @Test
    fun unauthorizedMeRefreshesThenRetriesWithNewAccessToken() = runBlocking {
        val meTokens = mutableListOf<String>()
        var refreshCount = 0
        val outcome = StartupSessionRecovery.restore(
            accessToken = "expired-access",
            refreshToken = { "refresh-1" },
            me = { token ->
                meTokens += token
                if (token == "expired-access") {
                    // 401 wrapped in IOException used to be treated as offline and skipped refresh.
                    throw IOException("stream reset", AuthStatusException(401))
                }
                user("remote")
            },
            refresh = {
                refreshCount += 1
                StartupSessionRecovery.RefreshedSession(
                    accessToken = "new-access",
                    refreshToken = "refresh-2",
                    user = user("from-refresh"),
                )
            },
            localUser = { user("local") },
        )

        assertEquals(listOf("expired-access", "new-access"), meTokens)
        assertEquals(1, refreshCount)
        assertTrue(outcome is StartupSessionRecovery.Outcome.Remote)
        assertEquals("remote", (outcome as StartupSessionRecovery.Outcome.Remote).user.id)
    }

    @Test
    fun refreshFailureFallsBackToLocalProfile() = runBlocking {
        var meCount = 0
        var refreshCount = 0
        val outcome = StartupSessionRecovery.restore(
            accessToken = "expired-access",
            refreshToken = { "refresh-1" },
            me = {
                meCount += 1
                throw AuthStatusException(401)
            },
            refresh = {
                refreshCount += 1
                throw IOException("failed to connect")
            },
            localUser = { user("local") },
        )

        assertEquals(1, meCount)
        assertEquals(1, refreshCount)
        assertTrue(outcome is StartupSessionRecovery.Outcome.LocalFallback)
        val fallback = outcome as StartupSessionRecovery.Outcome.LocalFallback
        assertEquals("local", fallback.user?.id)
        assertEquals(PrivilegeTier.PREMIUM, fallback.user?.privilegeTier)
    }

    @Test
    fun refreshUnauthorizedSignsOutInsteadOfHanging() = runBlocking {
        val outcome = StartupSessionRecovery.restore(
            accessToken = "expired-access",
            refreshToken = { "refresh-1" },
            me = { throw AuthStatusException(401) },
            refresh = { throw AuthStatusException(401) },
            localUser = { user("local") },
        )

        assertTrue(outcome is StartupSessionRecovery.Outcome.RefreshRejected)
    }

    @Test
    fun staleFallbackIsDroppedButRejectedRefreshAlwaysApplies() {
        val local = StartupSessionRecovery.Outcome.LocalFallback(user("local"))
        val remote = StartupSessionRecovery.Outcome.Remote(user("remote"))
        val rejected = StartupSessionRecovery.Outcome.RefreshRejected
        assertTrue(StartupSessionRecovery.shouldPublish(2, 2, local, newerRemotePublished = false))
        assertTrue(StartupSessionRecovery.shouldPublish(2, 2, remote, newerRemotePublished = false))
        // Older /v1/me may fill in until a newer attempt has published one.
        assertTrue(StartupSessionRecovery.shouldPublish(1, 2, remote, newerRemotePublished = false))
        assertFalse(StartupSessionRecovery.shouldPublish(1, 2, remote, newerRemotePublished = true))
        assertFalse(StartupSessionRecovery.shouldPublish(1, 2, local, newerRemotePublished = false))
        assertFalse(StartupSessionRecovery.shouldPublish(1, 2, local, newerRemotePublished = true))
        // A dead refresh token signs the user out even if this attempt is older.
        assertTrue(StartupSessionRecovery.shouldPublish(1, 2, rejected, newerRemotePublished = false))
        assertTrue(StartupSessionRecovery.shouldPublish(1, 2, rejected, newerRemotePublished = true))
    }

    private fun user(id: String) = AuthUser(
        id = id,
        email = "$id@example.com",
        displayName = id,
        avatarUrl = null,
        privilegeTier = PrivilegeTier.PREMIUM,
    )
}
