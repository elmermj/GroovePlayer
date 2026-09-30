package com.aethelsoft.grooveplayer.domain.plan

import com.aethelsoft.grooveplayer.domain.model.PlanSubscription
import com.aethelsoft.grooveplayer.domain.model.PrivilegeTier
import java.time.Instant
import java.time.ZoneId

/**
 * Startup plan notice for Basic and Premium.
 *
 * [EXPIRING_SOON] is an active plan that will not renew and ends within
 * [SOON_WINDOW_MS]. [IN_GRACE] is status grace. [EXPIRED] is status expired,
 * or a period that has already ended and is not auto-renewing.
 * When [PlanSubscription] is absent, [premiumPeriodEndEpochMs] and
 * [graceUntilEpochMs] from `/v1/me.storage` are used instead.
 */
object PlanExpiry {

    const val SOON_WINDOW_MS = 3L * 24 * 60 * 60 * 1000
    const val MANAGE = "Manage plan"
    const val DISMISS = "Dismiss"

    private const val DAY_MS = 24L * 60 * 60 * 1000

    enum class State {
        NONE,
        EXPIRING_SOON,
        IN_GRACE,
        EXPIRED,
    }

    data class Decision(
        val state: State,
        val plan: PrivilegeTier,
        val daysRemaining: Int,
    )

    data class Reminder(
        val plan: String,
        val state: String,
        val day: String,
    )

    fun evaluate(
        tier: PrivilegeTier,
        subscription: PlanSubscription?,
        premiumPeriodEndEpochMs: Long?,
        graceUntilEpochMs: Long?,
        nowEpochMs: Long,
    ): Decision {
        if (tier != PrivilegeTier.BASIC && tier != PrivilegeTier.PREMIUM) {
            return Decision(State.NONE, tier, 0)
        }
        val plan = noticePlan(tier, subscription)
        val state = if (subscription != null) {
            fromSubscription(subscription, nowEpochMs)
        } else {
            fromStorage(premiumPeriodEndEpochMs, graceUntilEpochMs, nowEpochMs)
        }
        val end = if (subscription != null) {
            subscription.currentPeriodEndEpochMs
        } else {
            premiumPeriodEndEpochMs
        }
        val days = if (state == State.EXPIRING_SOON && end != null) {
            daysUntil(end, nowEpochMs)
        } else {
            0
        }
        return Decision(state, plan, days)
    }

    fun message(state: State, plan: PrivilegeTier, days: Int): String {
        val name = when (plan) {
            PrivilegeTier.BASIC -> "Basic"
            PrivilegeTier.PREMIUM -> "Premium"
            PrivilegeTier.FREE -> return ""
        }
        return when (state) {
            State.EXPIRING_SOON -> {
                val unit = if (days == 1) "day" else "days"
                "Your $name plan expires in $days $unit."
            }
            State.IN_GRACE -> "Your $name plan is in a grace period."
            State.EXPIRED -> "Your $name plan has expired."
            State.NONE -> ""
        }
    }

    /**
     * One dialog per startup session, and the same plan + state at most once
     * per calendar day.
     */
    fun shouldShow(
        decision: Decision,
        today: String,
        last: Reminder?,
        alreadyShownThisSession: Boolean,
    ): Boolean {
        if (alreadyShownThisSession) return false
        if (decision.state == State.NONE) return false
        if (decision.plan != PrivilegeTier.BASIC && decision.plan != PrivilegeTier.PREMIUM) {
            return false
        }
        if (last != null &&
            last.plan == decision.plan.name &&
            last.state == decision.state.name &&
            last.day == today
        ) {
            return false
        }
        return true
    }

    fun dayKey(epochMs: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate().toString()

    fun daysUntil(endEpochMs: Long, nowEpochMs: Long): Int {
        val remaining = endEpochMs - nowEpochMs
        if (remaining <= 0L) return 0
        return ((remaining + DAY_MS - 1) / DAY_MS).toInt()
    }

    private fun noticePlan(tier: PrivilegeTier, subscription: PlanSubscription?): PrivilegeTier {
        val fromSubscription = subscription?.planKind
        return if (fromSubscription == PrivilegeTier.BASIC || fromSubscription == PrivilegeTier.PREMIUM) {
            fromSubscription
        } else {
            tier
        }
    }

    private fun fromSubscription(subscription: PlanSubscription, nowEpochMs: Long): State {
        val status = subscription.status.trim().lowercase()
        val end = subscription.currentPeriodEndEpochMs
        val renew = subscription.autoRenew
        if (status == "grace") return State.IN_GRACE
        if (status == "expired") return State.EXPIRED
        val periodPassed = end != null && end <= nowEpochMs
        if (periodPassed && !renew) return State.EXPIRED
        if (status == "active" && !renew && end != null && end > nowEpochMs && end - nowEpochMs <= SOON_WINDOW_MS) {
            return State.EXPIRING_SOON
        }
        val grace = subscription.graceUntilEpochMs
        if (grace != null && grace > nowEpochMs) return State.IN_GRACE
        return State.NONE
    }

    private fun fromStorage(
        premiumPeriodEndEpochMs: Long?,
        graceUntilEpochMs: Long?,
        nowEpochMs: Long,
    ): State {
        if (graceUntilEpochMs != null && graceUntilEpochMs > nowEpochMs) return State.IN_GRACE
        if (graceUntilEpochMs != null && graceUntilEpochMs <= nowEpochMs) return State.EXPIRED
        val end = premiumPeriodEndEpochMs ?: return State.NONE
        if (end <= nowEpochMs) return State.EXPIRED
        if (end - nowEpochMs <= SOON_WINDOW_MS) return State.EXPIRING_SOON
        return State.NONE
    }
}
