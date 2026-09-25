package com.aethelsoft.grooveplayer.data.library

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** One-time upgrade prompt. Dismissing it is local to this install and is not a pending delete. */
@Singleton
class LibraryUpgradeStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isDismissed(): Boolean = prefs.getBoolean(KEY_DISMISSED, false)

    fun dismiss() {
        prefs.edit().putBoolean(KEY_DISMISSED, true).apply()
    }

    private companion object {
        const val PREFS = "library_upgrade"
        const val KEY_DISMISSED = "import_prompt_dismissed"
    }
}
