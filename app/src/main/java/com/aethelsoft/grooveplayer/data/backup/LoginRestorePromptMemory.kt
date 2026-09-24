package com.aethelsoft.grooveplayer.data.backup

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remembers "No, I will use current data for now" for one backup revision.
 * Survives process death so the prompt does not return on every cold start.
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

    companion object {
        const val PREFS = "groove_login_restore_prompt"
        const val KEY_DECLINED = "declined_revision"
    }
}
