package com.aethelsoft.grooveplayer.data.backup

import android.content.Context
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stable per-install id sent as `device_id` on the backup lease.
 * Survives process death. A reinstall mints a new id.
 */
@Singleton
class InstallDeviceId @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Synchronized
    fun get(): String {
        val existing = prefs.getString(KEY_DEVICE_ID, null)?.takeIf { it.isNotBlank() }
        if (existing != null) return existing
        val created = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_DEVICE_ID, created).commit()
        return prefs.getString(KEY_DEVICE_ID, null)?.takeIf { it.isNotBlank() } ?: created
    }

    fun deviceName(): String {
        val raw = listOf(Build.MANUFACTURER, Build.MODEL)
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.equals("unknown", ignoreCase = true) }
            .distinct()
            .joinToString(" ")
        return raw.take(MAX_NAME).ifBlank { "Android" }
    }

    companion object {
        const val PREFS = "groove_install"
        const val KEY_DEVICE_ID = "device_id"
        private const val MAX_NAME = 80
    }
}
