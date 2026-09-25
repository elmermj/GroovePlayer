package com.aethelsoft.grooveplayer.data.backup

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remembers "No, I will use current data for now" for one backup revision,
 * and the revision that a successful restore just applied.
 * Both survive process death so the prompt does not return for that backup.
 */
@Singleton
class LoginRestorePromptMemory @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun declinedKey(): String? = prefs.getString(KEY_DECLINED, null)?.takeIf { it.isNotBlank() }

    fun decline(memoryKey: String) {
        prefs.edit().putString(KEY_DECLINED, memoryKey).commit()
    }

    fun clearDecline() {
        prefs.edit().remove(KEY_DECLINED).commit()
    }

    /** Revision key of the backup the last successful restore applied. */
    fun restoredKey(): String? = prefs.getString(KEY_RESTORED, null)?.takeIf { it.isNotBlank() }

    fun markRestored(memoryKey: String) {
        prefs.edit().putString(KEY_RESTORED, memoryKey).commit()
    }

    companion object {
        const val PREFS = "groove_login_restore_prompt"
        const val KEY_DECLINED = "declined_revision"
        const val KEY_RESTORED = "restored_revision"
    }
}
