package com.aethelsoft.grooveplayer.domain.auth

import com.aethelsoft.grooveplayer.domain.model.PrivilegeTier

/**
 * Why a `/v1/me` load is running.
 *
 * [STARTUP] is the one cold-start check and is allowed for every signed-in
 * session, including Free, so deletion and a tier upgrade can be seen.
 * [USER_RETRY] is the Profile Retry button after that check could not reach
 * the server. [FOLLOW_UP] is every later refetch (Backup open, Profile open)
 * and runs only for Basic or Premium.
 */
enum class SessionLoadPurpose {
    STARTUP,
    USER_RETRY,
    FOLLOW_UP,
}

/**
 * Which calls to grooveplayer-backend may leave the device.
 *
 * Signed-out users and signed-in Free users do not call the backend except:
 * sign-in, Play purchase verify/ack, the startup `/v1/me` check (and the
 * refresh it needs), a user-tapped Retry of that check, logout, and account
 * deletion. Playback lookup is Basic or Premium only. A skipped Free lookup
 * is unknown, so it does not purge a catalog row.
 */
object BackendCallGuard {

    fun allowsPaidApis(hasSession: Boolean, tier: PrivilegeTier): Boolean =
        hasSession && (tier == PrivilegeTier.BASIC || tier == PrivilegeTier.PREMIUM)

    fun allowSessionLoad(
        purpose: SessionLoadPurpose,
        hasSession: Boolean,
        tier: PrivilegeTier,
    ): Boolean {
        if (!hasSession) return false
        return when (purpose) {
            SessionLoadPurpose.STARTUP,
            SessionLoadPurpose.USER_RETRY,
            -> true
            SessionLoadPurpose.FOLLOW_UP -> allowsPaidApis(hasSession, tier)
        }
    }

    fun allow(
        path: String,
        method: String,
        hasSession: Boolean,
        tier: PrivilegeTier,
        sessionProbe: Boolean,
    ): Boolean {
        val normalized = path.substringBefore('?')
        if (normalized == "/v1/auth/google" && method.equals("POST", ignoreCase = true)) {
            return true
        }
        if (normalized == "/v1/billing/play/verify" || normalized == "/v1/billing/play/ack") {
            return true
        }
        if (normalized == "/healthz") return true
        if (sessionProbe && (normalized == "/v1/me" || normalized == "/v1/auth/refresh")) {
            return true
        }
        if (normalized == "/v1/auth/logout" || normalized == "/v1/account") {
            return hasSession
        }
        return allowsPaidApis(hasSession, tier)
    }
}
