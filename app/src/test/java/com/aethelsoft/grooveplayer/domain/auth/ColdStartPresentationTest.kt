package com.aethelsoft.grooveplayer.domain.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ColdStartPresentationTest {

    @Test
    fun splashHoldsOnlyUntilTheFirstFrame() {
        assertTrue(
            ColdStartPresentation.keepOnScreen(
                frameReady = false,
                elapsedMs = 0,
                blocker = ColdStartPresentation.StartupBlocker.NONE,
            ),
        )
        assertFalse(
            ColdStartPresentation.keepOnScreen(
                frameReady = true,
                elapsedMs = 0,
                blocker = ColdStartPresentation.StartupBlocker.NONE,
            ),
        )
    }

    @Test
    fun splashReleasesOnTimeoutEvenIfTheFirstFrameNeverArrives() {
        assertFalse(
            ColdStartPresentation.keepOnScreen(
                frameReady = false,
                elapsedMs = ColdStartPresentation.SPLASH_MAX_MS,
                blocker = ColdStartPresentation.StartupBlocker.NONE,
            ),
        )
    }

    @Test
    fun networkInFlightDoesNotHoldTheSplash() {
        val blocker = ColdStartPresentation.blockerFor(
            failure = null,
            timedOut = false,
            networkInFlight = true,
        )
        assertEquals(ColdStartPresentation.StartupBlocker.NETWORK, blocker)
        assertFalse(
            ColdStartPresentation.keepOnScreen(
                frameReady = false,
                elapsedMs = 0,
                blocker = blocker,
            ),
        )
    }

    @Test
    fun unauthorizedServerOfflineAndTimeoutReleaseTheSplash() {
        val cases = listOf(
            ColdStartPresentation.blockerFor(
                failure = StartupSessionRecovery.FailureKind.UNAUTHORIZED,
                timedOut = false,
                networkInFlight = true,
            ),
            ColdStartPresentation.blockerFor(
                failure = StartupSessionRecovery.FailureKind.OTHER,
                timedOut = false,
                networkInFlight = true,
            ),
            ColdStartPresentation.blockerFor(
                failure = StartupSessionRecovery.FailureKind.UNREACHABLE,
                timedOut = false,
                networkInFlight = true,
            ),
            ColdStartPresentation.blockerFor(
                failure = null,
                timedOut = true,
                networkInFlight = true,
            ),
        )
        assertEquals(
            listOf(
                ColdStartPresentation.StartupBlocker.UNAUTHORIZED,
                ColdStartPresentation.StartupBlocker.SERVER_ERROR,
                ColdStartPresentation.StartupBlocker.OFFLINE,
                ColdStartPresentation.StartupBlocker.TIMED_OUT,
            ),
            cases,
        )
        cases.forEach { blocker ->
            assertFalse(
                ColdStartPresentation.keepOnScreen(
                    frameReady = false,
                    elapsedMs = 0,
                    blocker = blocker,
                ),
            )
        }
    }

    @Test
    fun coldStartEntranceDoesNotSlideTheFirstScreenOff() {
        assertFalse(ColdStartPresentation.playEntrance(wasColdStart = true))
        assertTrue(ColdStartPresentation.playEntrance(wasColdStart = false))
    }

    @Test
    fun blankStartDestinationFallsBackToHome() {
        assertEquals("home", ColdStartPresentation.startDestination(null, "home"))
        assertEquals("home", ColdStartPresentation.startDestination("  ", "home"))
        assertEquals(
            "restore_apply?start=true",
            ColdStartPresentation.startDestination("restore_apply?start=true", "home"),
        )
    }
}
