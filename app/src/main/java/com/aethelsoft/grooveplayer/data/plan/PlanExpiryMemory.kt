package com.aethelsoft.grooveplayer.data.plan

import android.content.Context
import com.aethelsoft.grooveplayer.domain.plan.PlanExpiry
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Last plan-expiry dialog: plan, state, and local calendar day. */
@Singleton
class PlanExpiryMemory @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun last(): PlanExpiry.Reminder? {
        val plan = prefs.getString(KEY_PLAN, null)?.takeIf { it.isNotBlank() } ?: return null
        val state = prefs.getString(KEY_STATE, null)?.takeIf { it.isNotBlank() } ?: return null
        val day = prefs.getString(KEY_DAY, null)?.takeIf { it.isNotBlank() } ?: return null
        return PlanExpiry.Reminder(plan = plan, state = state, day = day)
    }

    fun mark(plan: String, state: String, day: String) {
        prefs.edit()
            .putString(KEY_PLAN, plan)
            .putString(KEY_STATE, state)
            .putString(KEY_DAY, day)
            .commit()
    }

    companion object {
        const val PREFS = "groove_plan_expiry"
        private const val KEY_PLAN = "plan"
        private const val KEY_STATE = "state"
        private const val KEY_DAY = "day"
    }
}
