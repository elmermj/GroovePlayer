package com.aethelworks.grooveplayer.domain.auth

import com.aethelworks.grooveplayer.domain.model.PrivilegeTier
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackendCallGuardTest {

    @Test
    fun signedOutUserCannotCallTheBackendExceptSignIn() {
        assertTrue(allow("POST", "/v1/auth/google", signedIn = false, PrivilegeTier.FREE))
        assertFalse(allow("GET", "/v1/me", signedIn = false, PrivilegeTier.FREE))
        assertFalse(allow("POST", "/v1/auth/refresh", signedIn = false, PrivilegeTier.FREE))
        assertFalse(allow("POST", "/v1/auth/logout", signedIn = false, PrivilegeTier.FREE))
        assertFalse(allow("DELETE", "/v1/account", signedIn = false, PrivilegeTier.FREE))
        assertFalse(allow("GET", "/v1/backup/objects", signedIn = false, PrivilegeTier.FREE))
        assertFalse(allow("GET", "/v1/playback/objects", signedIn = false, PrivilegeTier.FREE))
        assertTrue(allow("POST", "/v1/billing/play/verify", signedIn = false, PrivilegeTier.FREE))
        assertTrue(allow("POST", "/v1/billing/play/ack", signedIn = false, PrivilegeTier.FREE))
    }

    @Test
    fun freeSessionIsLimitedToStartupCheckBillingAndAccountActions() {
        assertTrue(allow("GET", "/v1/me", signedIn = true, PrivilegeTier.FREE, probe = true))
        assertTrue(allow("POST", "/v1/auth/refresh", signedIn = true, PrivilegeTier.FREE, probe = true))
        assertFalse(allow("GET", "/v1/me", signedIn = true, PrivilegeTier.FREE, probe = false))
        assertFalse(allow("POST", "/v1/auth/refresh", signedIn = true, PrivilegeTier.FREE))
        assertFalse(allow("GET", "/v1/backup/library", signedIn = true, PrivilegeTier.FREE))
        assertFalse(allow("POST", "/v1/backup/lease", signedIn = true, PrivilegeTier.FREE))
        assertFalse(allow("POST", "/v1/billing/addons/cancel", signedIn = true, PrivilegeTier.FREE))
        assertTrue(allow("POST", "/v1/billing/play/verify", signedIn = true, PrivilegeTier.FREE))
        assertTrue(allow("POST", "/v1/billing/play/ack", signedIn = true, PrivilegeTier.FREE))
        assertTrue(allow("POST", "/v1/auth/logout", signedIn = true, PrivilegeTier.FREE))
        assertTrue(allow("DELETE", "/v1/account", signedIn = true, PrivilegeTier.FREE))
        assertFalse(allow("GET", "/v1/playback/objects", signedIn = true, PrivilegeTier.FREE))
        assertFalse(allow("POST", "/v1/playback/stream-url", signedIn = true, PrivilegeTier.FREE))
    }

    @Test
    fun basicAndPremiumMayCallPaidApis() {
        listOf(PrivilegeTier.BASIC, PrivilegeTier.PREMIUM).forEach { tier ->
            assertTrue(allow("GET", "/v1/me", signedIn = true, tier))
            assertTrue(allow("GET", "/v1/backup/objects", signedIn = true, tier))
            assertTrue(allow("POST", "/v1/backup/upload-url", signedIn = true, tier))
            assertTrue(allow("POST", "/v1/billing/addons/cancel", signedIn = true, tier))
            assertTrue(allow("GET", "/v1/playback/objects", signedIn = true, tier))
            assertTrue(allow("POST", "/v1/playback/stream-url", signedIn = true, tier))
            assertTrue(BackendCallGuard.allowsPaidApis(hasSession = true, tier = tier))
        }
        assertFalse(BackendCallGuard.allowsPaidApis(hasSession = true, tier = PrivilegeTier.FREE))
        assertFalse(BackendCallGuard.allowsPaidApis(hasSession = false, tier = PrivilegeTier.PREMIUM))
    }

    @Test
    fun sessionLoadFollowsPurpose() {
        assertTrue(
            BackendCallGuard.allowSessionLoad(
                SessionLoadPurpose.STARTUP,
                hasSession = true,
                tier = PrivilegeTier.FREE,
            ),
        )
        assertTrue(
            BackendCallGuard.allowSessionLoad(
                SessionLoadPurpose.USER_RETRY,
                hasSession = true,
                tier = PrivilegeTier.FREE,
            ),
        )
        assertFalse(
            BackendCallGuard.allowSessionLoad(
                SessionLoadPurpose.FOLLOW_UP,
                hasSession = true,
                tier = PrivilegeTier.FREE,
            ),
        )
        assertTrue(
            BackendCallGuard.allowSessionLoad(
                SessionLoadPurpose.FOLLOW_UP,
                hasSession = true,
                tier = PrivilegeTier.BASIC,
            ),
        )
        assertTrue(
            BackendCallGuard.allowSessionLoad(
                SessionLoadPurpose.FOLLOW_UP,
                hasSession = true,
                tier = PrivilegeTier.PREMIUM,
            ),
        )
        assertFalse(
            BackendCallGuard.allowSessionLoad(
                SessionLoadPurpose.STARTUP,
                hasSession = false,
                tier = PrivilegeTier.PREMIUM,
            ),
        )
    }

    private fun allow(
        method: String,
        path: String,
        signedIn: Boolean,
        tier: PrivilegeTier,
        probe: Boolean = false,
    ): Boolean = BackendCallGuard.allow(
        path = path,
        method = method,
        hasSession = signedIn,
        tier = tier,
        sessionProbe = probe,
    )
}
