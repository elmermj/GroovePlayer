package com.aethelsoft.grooveplayer.domain.backup

import com.aethelsoft.grooveplayer.domain.model.CloudLibrarySnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LoginRestorePromptTest {

    @Test
    fun lockedOptionCopy() {
        assertEquals("Do you want to restore your backup data?", LoginRestorePrompt.TITLE)
        assertEquals("No, I will use current data for now.", LoginRestorePrompt.KEEP_CURRENT)
        assertEquals("Yes.", LoginRestorePrompt.RESTORE)
    }

    @Test
    fun offersWhenSignedInPremiumAndARestorableBackupExists() {
        val key = LoginRestorePrompt.shouldOffer(ready())
        assertEquals(
            "user-1|obj-1|hash-a|2026-09-01T00:00:00Z|128",
            key,
        )
    }

    @Test
    fun skipsWhenSignedOutNotPremiumOrBackupMissing() {
        assertNull(LoginRestorePrompt.shouldOffer(ready(signedIn = false)))
        assertNull(LoginRestorePrompt.shouldOffer(ready(premium = false)))
        assertNull(LoginRestorePrompt.shouldOffer(ready(userId = " ")))
        assertNull(LoginRestorePrompt.shouldOffer(ready(snapshot = null)))
        assertNull(LoginRestorePrompt.shouldOffer(ready(snapshot = sample(dryRun = true))))
        assertNull(
            LoginRestorePrompt.shouldOffer(
                ready(snapshot = sample(objectId = " ", contentHash = null)),
            ),
        )
    }

    @Test
    fun skipsWhileBackupOrRestoreIsRunningOrScreenIsOpen() {
        assertNull(LoginRestorePrompt.shouldOffer(ready(backupUploadInProgress = true)))
        assertNull(LoginRestorePrompt.shouldOffer(ready(restoreInProgress = true)))
        assertNull(LoginRestorePrompt.shouldOffer(ready(onBackupOrRestoreScreen = true)))
    }

    @Test
    fun remembersNoUntilTheBackupRevisionChanges() {
        val declined = LoginRestorePrompt.shouldOffer(ready())
        assertNull(LoginRestorePrompt.shouldOffer(ready(persistedDeclinedKey = declined)))
        assertNull(LoginRestorePrompt.shouldOffer(ready(sessionHandledKey = declined)))

        val changed = LoginRestorePrompt.shouldOffer(
            ready(
                snapshot = sample(contentHash = "hash-b"),
                persistedDeclinedKey = declined,
            ),
        )
        assertEquals(
            "user-1|obj-1|hash-b|2026-09-01T00:00:00Z|128",
            changed,
        )
    }

    @Test
    fun skipsTheBackupThatWasJustRestored() {
        val restored = LoginRestorePrompt.shouldOffer(ready())
        assertNull(LoginRestorePrompt.shouldOffer(ready(persistedRestoredKey = restored)))
    }

    @Test
    fun offersADifferentBackupAfterRestoreAndAFreshSignIn() {
        val restored = LoginRestorePrompt.shouldOffer(ready())
        assertNull(LoginRestorePrompt.shouldOffer(ready(persistedRestoredKey = restored)))
        val changed = LoginRestorePrompt.shouldOffer(
            ready(
                snapshot = sample(contentHash = "hash-b"),
                persistedRestoredKey = restored,
            ),
        )
        assertEquals(
            "user-1|obj-1|hash-b|2026-09-01T00:00:00Z|128",
            changed,
        )
        assertEquals(
            "user-1|obj-1|hash-a|2026-09-01T00:00:00Z|128",
            LoginRestorePrompt.shouldOffer(ready(persistedRestoredKey = null)),
        )
    }

    private fun ready(
        signedIn: Boolean = true,
        premium: Boolean = true,
        userId: String = "user-1",
        snapshot: CloudLibrarySnapshot? = sample(),
        backupUploadInProgress: Boolean = false,
        restoreInProgress: Boolean = false,
        onBackupOrRestoreScreen: Boolean = false,
        persistedDeclinedKey: String? = null,
        sessionHandledKey: String? = null,
        persistedRestoredKey: String? = null,
    ) = LoginRestorePromptInput(
        signedIn = signedIn,
        premium = premium,
        userId = userId,
        snapshot = snapshot,
        backupUploadInProgress = backupUploadInProgress,
        restoreInProgress = restoreInProgress,
        onBackupOrRestoreScreen = onBackupOrRestoreScreen,
        persistedDeclinedKey = persistedDeclinedKey,
        sessionHandledKey = sessionHandledKey,
        persistedRestoredKey = persistedRestoredKey,
    )

    private fun sample(
        objectId: String? = "obj-1",
        contentHash: String? = "hash-a",
        dryRun: Boolean = false,
    ) = CloudLibrarySnapshot(
        objectId = objectId,
        contentHash = contentHash,
        sizeBytes = 128,
        r2Key = "backups/user-1/library/room.db.gz",
        logicalPath = "library/room.db.gz",
        schemaVersion = 16,
        appVersion = "1.0",
        createdAtIso = "2026-09-01T00:00:00Z",
        dryRun = dryRun,
    )
}
