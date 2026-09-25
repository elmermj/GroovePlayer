package com.aethelsoft.grooveplayer.domain.backup

import com.aethelsoft.grooveplayer.domain.model.CloudLibrarySnapshot

/**
 * Whether to ask a signed-in Premium user to restore a cloud library backup.
 * "No" is remembered for this backup revision. A backup that just finished
 * restoring is remembered too, so the prompt stays quiet after the app restarts.
 * A different cloud backup, or a fresh sign-in that has not restored it, still asks.
 */
object LoginRestorePrompt {
    const val TITLE = "Do you want to restore your backup data?"
    const val KEEP_CURRENT = "No, I will use current data for now."
    const val RESTORE = "Yes."

    /**
     * Identity of a restorable cloud library snapshot.
     * Dry-run catalogs and snapshots with no id or hash are not offered.
     */
    fun revisionOf(snapshot: CloudLibrarySnapshot): String? {
        if (snapshot.dryRun) return null
        val id = snapshot.objectId?.trim().orEmpty()
        val hash = snapshot.contentHash?.trim().orEmpty()
        if (id.isEmpty() && hash.isEmpty()) return null
        val created = snapshot.createdAtIso?.trim().orEmpty()
        return "$id|$hash|$created|${snapshot.sizeBytes}"
    }

    fun memoryKey(userId: String, revision: String): String = "$userId|$revision"

    /** Memory key to persist when the prompt should show, or null to stay quiet. */
    fun shouldOffer(input: LoginRestorePromptInput): String? {
        if (!input.signedIn || !input.premium) return null
        if (input.onBackupOrRestoreScreen) return null
        if (input.backupUploadInProgress || input.restoreInProgress) return null
        val userId = input.userId.trim()
        if (userId.isEmpty()) return null
        val snapshot = input.snapshot ?: return null
        val revision = revisionOf(snapshot) ?: return null
        val key = memoryKey(userId, revision)
        if (key == input.persistedDeclinedKey ||
            key == input.sessionHandledKey ||
            key == input.persistedRestoredKey
        ) {
            return null
        }
        return key
    }
}

data class LoginRestorePromptInput(
    val signedIn: Boolean,
    val premium: Boolean,
    val userId: String,
    val snapshot: CloudLibrarySnapshot?,
    val backupUploadInProgress: Boolean,
    val restoreInProgress: Boolean,
    val onBackupOrRestoreScreen: Boolean,
    val persistedDeclinedKey: String?,
    val sessionHandledKey: String?,
    /** Backup revision applied by the last successful restore, if any. */
    val persistedRestoredKey: String? = null,
)
