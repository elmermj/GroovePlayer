package com.aethelsoft.grooveplayer.data.backup

import com.aethelsoft.grooveplayer.domain.backup.ColdStartAction
import com.aethelsoft.grooveplayer.domain.backup.RestoreApplyRecovery
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cold-start decision for an interrupted restore. File rename recovery already ran
 * when Room opened. This only chooses Home versus the applying screen.
 * The unread `needs_restart_after_library_restore` flag is cleared and never swaps files.
 */
@Singleton
class LibraryRestoreGate @Inject constructor(
    private val session: LibraryRestoreSession,
) {
    val launchAction: ColdStartAction by lazy {
        session.clearLegacyRestartFlag()
        val action = RestoreApplyRecovery.coldStartAction(
            phase = session.phase(),
            verdict = session.peekStagingVerdict(),
        )
        if (action == ColdStartAction.DISCARD_AND_HOME) {
            session.discard()
        }
        action
    }
}
