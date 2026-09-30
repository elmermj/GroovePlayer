package com.aethelsoft.grooveplayer.presentation.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aethelsoft.grooveplayer.data.plan.PlanExpiryMemory
import com.aethelsoft.grooveplayer.domain.model.AuthUser
import com.aethelsoft.grooveplayer.domain.model.PrivilegeTier
import com.aethelsoft.grooveplayer.domain.plan.PlanExpiry
import com.aethelsoft.grooveplayer.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlanExpiryDialogModel(
    val state: PlanExpiry.State,
    val plan: PrivilegeTier,
    val message: String,
    val day: String,
)

@HiltViewModel
class PlanExpiryViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val memory: PlanExpiryMemory,
) : ViewModel() {

    private val _notice = MutableStateFlow<PlanExpiryDialogModel?>(null)
    val notice: StateFlow<PlanExpiryDialogModel?> = _notice.asStateFlow()

    private var shownThisSession = false
    private var holding: PlanExpiryDialogModel? = null

    init {
        viewModelScope.launch {
            authRepository.observeAuthUser().collect { user ->
                publish(user)
            }
        }
    }

    /** Persist the day + state once the dialog is on screen. */
    fun confirmShown() {
        val model = holding ?: return
        memory.mark(model.plan.name, model.state.name, model.day)
    }

    fun dismiss() {
        shownThisSession = true
        holding = null
        _notice.value = null
    }

    private fun publish(user: AuthUser?) {
        if (user == null ||
            (user.privilegeTier != PrivilegeTier.BASIC && user.privilegeTier != PrivilegeTier.PREMIUM)
        ) {
            holding = null
            _notice.value = null
            return
        }
        if (holding != null) {
            _notice.value = holding
            return
        }
        val model = build(user) ?: return
        holding = model
        _notice.value = model
    }

    private fun build(user: AuthUser): PlanExpiryDialogModel? {
        if (shownThisSession) return null
        val now = System.currentTimeMillis()
        val storage = user.storage?.takeUnless { it.isOptimisticStub }
        val decision = PlanExpiry.evaluate(
            tier = user.privilegeTier,
            subscription = user.subscription,
            premiumPeriodEndEpochMs = storage?.premiumPeriodEndEpochMs,
            graceUntilEpochMs = storage?.graceUntilEpochMs,
            nowEpochMs = now,
        )
        val today = PlanExpiry.dayKey(now)
        if (!PlanExpiry.shouldShow(decision, today, memory.last(), shownThisSession)) return null
        return PlanExpiryDialogModel(
            state = decision.state,
            plan = decision.plan,
            message = PlanExpiry.message(decision.state, decision.plan, decision.daysRemaining),
            day = today,
        )
    }
}
