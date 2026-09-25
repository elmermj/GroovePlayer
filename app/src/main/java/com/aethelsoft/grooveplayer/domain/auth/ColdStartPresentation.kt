package com.aethelsoft.grooveplayer.domain.auth

/**
 * Decides when the cold-start window may show app content.
 *
 * The system splash sits on a black window background until the first frame.
 * Nothing here may wait on `/v1/me`, token refresh, or any other network call.
 * Offline, 401, 5xx, and timeout all release the splash. A blank navigation
 * route falls back to [home] so the start destination cannot stay unset.
 */
object ColdStartPresentation {

    /**
     * Upper bound for holding the splash while the first frame has not drawn.
     * Independent of [StartupSessionRecovery.NETWORK_TIMEOUT_MS]: a slow or
     * failed auth call must not keep the window black.
     */
    const val SPLASH_MAX_MS = 1_500L

    enum class StartupBlocker {
        /** Local startup only. Splash may stay until the first frame or [SPLASH_MAX_MS]. */
        NONE,
        /** `/v1/me` or refresh still running. Does not hold the splash. */
        NETWORK,
        UNAUTHORIZED,
        SERVER_ERROR,
        OFFLINE,
        TIMED_OUT,
    }

    fun blockerFor(
        failure: StartupSessionRecovery.FailureKind?,
        timedOut: Boolean,
        networkInFlight: Boolean,
    ): StartupBlocker {
        if (timedOut) return StartupBlocker.TIMED_OUT
        return when (failure) {
            null -> if (networkInFlight) StartupBlocker.NETWORK else StartupBlocker.NONE
            StartupSessionRecovery.FailureKind.UNAUTHORIZED -> StartupBlocker.UNAUTHORIZED
            StartupSessionRecovery.FailureKind.UNREACHABLE -> StartupBlocker.OFFLINE
            StartupSessionRecovery.FailureKind.OTHER -> StartupBlocker.SERVER_ERROR
        }
    }

    /**
     * True while the splash should cover the window.
     * [StartupBlocker.NETWORK] and every failure return false: the first frame
     * is not gated on the server.
     */
    fun keepOnScreen(
        frameReady: Boolean,
        elapsedMs: Long,
        blocker: StartupBlocker,
    ): Boolean {
        if (frameReady) return false
        if (elapsedMs >= SPLASH_MAX_MS) return false
        return blocker == StartupBlocker.NONE
    }

    /** False for the cold-start entrance so the first screen is not slid off-screen. */
    fun playEntrance(wasColdStart: Boolean): Boolean = !wasColdStart

    fun startDestination(candidate: String?, home: String): String {
        val route = candidate?.trim().orEmpty()
        return if (route.isEmpty()) home else route
    }
}
