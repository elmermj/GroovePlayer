package com.aethelsoft.grooveplayer.domain.backup

/**
 * File-level state of an atomic Room database swap.
 *
 * The live file is never overwritten in place. [ARMED] keeps the previous bytes aside
 * while the live file is still the original. [LIVE_PARKED] means the live name has been
 * moved aside (or that step was attempted). [COMMITTED] means the incoming snapshot
 * occupies the live name and its WAL sidecars were removed.
 */
enum class DbSwapStep {
    IDLE,
    ARMED,
    LIVE_PARKED,
    COMMITTED,
}

data class DbSwapSnapshot(
    val step: DbSwapStep,
    val liveExists: Boolean,
    val liveHeaderOk: Boolean,
    val asideExists: Boolean,
    val asideHeaderOk: Boolean,
    val incomingExists: Boolean,
    val incomingHeaderOk: Boolean,
    val previousExists: Boolean,
    val previousHeaderOk: Boolean,
)

enum class DbSwapAction {
    /** Open whatever is at the live path. */
    NONE,
    /** Finish `incoming` → live. The previous generation is already aside. */
    PROMOTE_INCOMING,
    /** Put the parked live file back at the live path. */
    RESTORE_ASIDE,
    /** Copy the pre-swap generation back over the live path. */
    RESTORE_PREVIOUS,
}

object RoomDbSwap {
    fun parseStep(raw: String?): DbSwapStep {
        if (raw.isNullOrBlank()) return DbSwapStep.IDLE
        return runCatching { DbSwapStep.valueOf(raw.trim()) }.getOrDefault(DbSwapStep.IDLE)
    }

    fun nextAction(snapshot: DbSwapSnapshot): DbSwapAction = when (snapshot.step) {
        DbSwapStep.IDLE -> DbSwapAction.NONE
        DbSwapStep.ARMED -> when {
            snapshot.liveExists && snapshot.liveHeaderOk -> DbSwapAction.NONE
            !snapshot.liveExists && snapshot.asideExists && snapshot.asideHeaderOk ->
                DbSwapAction.RESTORE_ASIDE
            snapshot.previousExists && snapshot.previousHeaderOk -> DbSwapAction.RESTORE_PREVIOUS
            else -> DbSwapAction.NONE
        }
        DbSwapStep.LIVE_PARKED -> when {
            !snapshot.liveExists && snapshot.incomingExists && snapshot.incomingHeaderOk ->
                DbSwapAction.PROMOTE_INCOMING
            !snapshot.liveExists && snapshot.asideExists && snapshot.asideHeaderOk ->
                DbSwapAction.RESTORE_ASIDE
            snapshot.liveExists && snapshot.liveHeaderOk -> DbSwapAction.NONE
            snapshot.previousExists && snapshot.previousHeaderOk -> DbSwapAction.RESTORE_PREVIOUS
            else -> DbSwapAction.RESTORE_PREVIOUS
        }
        DbSwapStep.COMMITTED -> when {
            snapshot.liveExists && snapshot.liveHeaderOk -> DbSwapAction.NONE
            snapshot.previousExists && snapshot.previousHeaderOk -> DbSwapAction.RESTORE_PREVIOUS
            snapshot.asideExists && snapshot.asideHeaderOk -> DbSwapAction.RESTORE_ASIDE
            else -> DbSwapAction.NONE
        }
    }
}
