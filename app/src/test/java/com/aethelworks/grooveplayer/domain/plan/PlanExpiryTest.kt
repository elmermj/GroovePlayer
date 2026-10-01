package com.aethelworks.grooveplayer.domain.plan

import com.aethelworks.grooveplayer.domain.model.PlanSubscription
import com.aethelworks.grooveplayer.domain.model.PrivilegeTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset

class PlanExpiryTest {

    private val now = 1_700_000_000_000L
    private val day = 24L * 60 * 60 * 1000

    @Test
    fun activePlanThatWillNotRenewInsideThreeDaysIsExpiringSoon() {
        val decision = PlanExpiry.evaluate(
            tier = PrivilegeTier.PREMIUM,
            subscription = sub(
                plan = PrivilegeTier.PREMIUM,
                status = "active",
                end = now + 2 * day,
                autoRenew = false,
            ),
            premiumPeriodEndEpochMs = null,
            graceUntilEpochMs = null,
            nowEpochMs = now,
        )
        assertEquals(PlanExpiry.State.EXPIRING_SOON, decision.state)
        assertEquals(PrivilegeTier.PREMIUM, decision.plan)
        assertEquals(2, decision.daysRemaining)
        assertEquals("Your Premium plan expires in 2 days.", PlanExpiry.message(decision.state, decision.plan, decision.daysRemaining))
    }

    @Test
    fun oneDayUsesTheSingular() {
        assertEquals(
            "Your Basic plan expires in 1 day.",
            PlanExpiry.message(PlanExpiry.State.EXPIRING_SOON, PrivilegeTier.BASIC, 1),
        )
    }

    @Test
    fun autoRenewInsideTheWindowIsQuiet() {
        val decision = evaluate(
            sub(PrivilegeTier.PREMIUM, "active", now + 2 * day, autoRenew = true),
        )
        assertEquals(PlanExpiry.State.NONE, decision.state)
    }

    @Test
    fun activePlanEndingAfterThreeDaysIsQuiet() {
        val decision = evaluate(
            sub(PrivilegeTier.PREMIUM, "active", now + PlanExpiry.SOON_WINDOW_MS + 1, autoRenew = false),
        )
        assertEquals(PlanExpiry.State.NONE, decision.state)
    }

    @Test
    fun edgeOfTheThreeDayWindowIsExpiringSoon() {
        val decision = evaluate(
            sub(PrivilegeTier.BASIC, "active", now + PlanExpiry.SOON_WINDOW_MS, autoRenew = false),
        )
        assertEquals(PlanExpiry.State.EXPIRING_SOON, decision.state)
        assertEquals(3, decision.daysRemaining)
    }

    @Test
    fun graceStatusIsInGrace() {
        val decision = evaluate(
            sub(PrivilegeTier.PREMIUM, "grace", now - day, autoRenew = false, grace = now + day),
        )
        assertEquals(PlanExpiry.State.IN_GRACE, decision.state)
        assertEquals(
            "Your Premium plan is in a grace period.",
            PlanExpiry.message(decision.state, decision.plan, decision.daysRemaining),
        )
    }

    @Test
    fun expiredStatusIsExpired() {
        val decision = evaluate(
            sub(PrivilegeTier.BASIC, "expired", now + day, autoRenew = true),
        )
        assertEquals(PlanExpiry.State.EXPIRED, decision.state)
        assertEquals(PrivilegeTier.BASIC, decision.plan)
        assertEquals(
            "Your Basic plan has expired.",
            PlanExpiry.message(decision.state, decision.plan, 0),
        )
    }

    @Test
    fun periodEndPassedWithoutAutoRenewIsExpired() {
        val decision = evaluate(
            sub(PrivilegeTier.PREMIUM, "active", now, autoRenew = false),
        )
        assertEquals(PlanExpiry.State.EXPIRED, decision.state)
    }

    @Test
    fun periodEndPassedWhileAutoRenewingIsQuiet() {
        val decision = evaluate(
            sub(PrivilegeTier.PREMIUM, "active", now - 1, autoRenew = true),
        )
        assertEquals(PlanExpiry.State.NONE, decision.state)
    }

    @Test
    fun subscriptionPlanKindWinsOverTier() {
        val decision = evaluate(
            sub(PrivilegeTier.BASIC, "expired", now - day, autoRenew = false),
            tier = PrivilegeTier.PREMIUM,
        )
        assertEquals(PrivilegeTier.BASIC, decision.plan)
    }

    @Test
    fun freeTierIsNeverNotified() {
        val decision = PlanExpiry.evaluate(
            tier = PrivilegeTier.FREE,
            subscription = sub(PrivilegeTier.PREMIUM, "expired", now - day, autoRenew = false),
            premiumPeriodEndEpochMs = now - day,
            graceUntilEpochMs = now + day,
            nowEpochMs = now,
        )
        assertEquals(PlanExpiry.State.NONE, decision.state)
    }

    @Test
    fun missingSubscriptionFallsBackToStorageGraceThenPeriodEnd() {
        val grace = PlanExpiry.evaluate(
            tier = PrivilegeTier.PREMIUM,
            subscription = null,
            premiumPeriodEndEpochMs = now - day,
            graceUntilEpochMs = now + day,
            nowEpochMs = now,
        )
        assertEquals(PlanExpiry.State.IN_GRACE, grace.state)

        val soon = PlanExpiry.evaluate(
            tier = PrivilegeTier.BASIC,
            subscription = null,
            premiumPeriodEndEpochMs = now + day,
            graceUntilEpochMs = null,
            nowEpochMs = now,
        )
        assertEquals(PlanExpiry.State.EXPIRING_SOON, soon.state)
        assertEquals(1, soon.daysRemaining)

        val expired = PlanExpiry.evaluate(
            tier = PrivilegeTier.PREMIUM,
            subscription = null,
            premiumPeriodEndEpochMs = now - 1,
            graceUntilEpochMs = null,
            nowEpochMs = now,
        )
        assertEquals(PlanExpiry.State.EXPIRED, expired.state)

        val closedGrace = PlanExpiry.evaluate(
            tier = PrivilegeTier.PREMIUM,
            subscription = null,
            premiumPeriodEndEpochMs = now + day,
            graceUntilEpochMs = now,
            nowEpochMs = now,
        )
        assertEquals(PlanExpiry.State.EXPIRED, closedGrace.state)

        val none = PlanExpiry.evaluate(
            tier = PrivilegeTier.PREMIUM,
            subscription = null,
            premiumPeriodEndEpochMs = null,
            graceUntilEpochMs = null,
            nowEpochMs = now,
        )
        assertEquals(PlanExpiry.State.NONE, none.state)
    }

    @Test
    fun sameStateIsNotShownAgainTheSameDay() {
        val decision = evaluate(
            sub(PrivilegeTier.PREMIUM, "expired", now - day, autoRenew = false),
        )
        val today = PlanExpiry.dayKey(now, ZoneOffset.UTC)
        val last = PlanExpiry.Reminder(
            plan = PrivilegeTier.PREMIUM.name,
            state = PlanExpiry.State.EXPIRED.name,
            day = today,
        )
        assertFalse(PlanExpiry.shouldShow(decision, today, last, alreadyShownThisSession = false))
        assertTrue(
            PlanExpiry.shouldShow(
                decision,
                today,
                last.copy(state = PlanExpiry.State.EXPIRING_SOON.name),
                alreadyShownThisSession = false,
            ),
        )
        assertTrue(
            PlanExpiry.shouldShow(
                decision,
                today,
                last.copy(plan = PrivilegeTier.BASIC.name),
                alreadyShownThisSession = false,
            ),
        )
        assertFalse(PlanExpiry.shouldShow(decision, today, null, alreadyShownThisSession = true))
        assertTrue(PlanExpiry.shouldShow(decision, today, null, alreadyShownThisSession = false))
    }

    private fun evaluate(
        subscription: PlanSubscription,
        tier: PrivilegeTier = PrivilegeTier.PREMIUM,
    ) = PlanExpiry.evaluate(
        tier = tier,
        subscription = subscription,
        premiumPeriodEndEpochMs = null,
        graceUntilEpochMs = null,
        nowEpochMs = now,
    )

    private fun sub(
        plan: PrivilegeTier,
        status: String,
        end: Long,
        autoRenew: Boolean,
        grace: Long? = null,
    ) = PlanSubscription(
        planKind = plan,
        status = status,
        currentPeriodEndEpochMs = end,
        autoRenew = autoRenew,
        graceUntilEpochMs = grace,
    )
}
