package com.aethelworks.grooveplayer.wear.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchLinkTest {
    @Test
    fun reachablePhoneNodeConnectsAndADropDisconnects() {
        val first = reduceWatchLink(WatchLink(), setOf("phone-1"))
        assertTrue(first.link.connected)
        assertTrue(first.reconnected)

        val still = reduceWatchLink(first.link, setOf("phone-1"))
        assertTrue(still.link.connected)
        assertFalse(still.reconnected)

        val extra = reduceWatchLink(first.link, setOf("phone-1", "phone-2"))
        assertFalse(extra.reconnected)

        val gone = reduceWatchLink(first.link, emptySet())
        assertFalse(gone.link.connected)
        assertFalse(gone.reconnected)
    }

    @Test
    fun emptyToEmptyIsNotAReconnect() {
        val change = reduceWatchLink(WatchLink(), emptySet())
        assertFalse(change.link.connected)
        assertFalse(change.reconnected)
    }

    @Test
    fun requestStateOnLaunchAndReconnectOnly() {
        assertTrue(shouldRequestState(launch = true, reconnected = false))
        assertTrue(shouldRequestState(launch = false, reconnected = true))
        assertFalse(shouldRequestState(launch = false, reconnected = false))
    }
}
