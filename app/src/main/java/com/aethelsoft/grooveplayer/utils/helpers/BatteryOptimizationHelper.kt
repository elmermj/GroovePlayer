package com.aethelsoft.grooveplayer.utils.helpers

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper to detect and request battery optimization whitelist.
 * Essential for Xiaomi, iQOO, and other OEMs with aggressive background kill.
 */
@Singleton
class BatteryOptimizationHelper @Inject constructor(
    private val context: Context,
) {

    /**
     * Check if the app is excluded from battery optimization.
     * Returns true if exempt (good for background transfers).
     */
    fun isIgnoringBatteryOptimizations(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * Open the system settings to request battery optimization whitelist.
     * User must manually enable "Don't optimize" for this app.
     */
    fun openBatteryOptimizationSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent().apply {
                action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(intent)
            } catch (_: Exception) {
                // Fallback: open general app info
                openAppInfoSettings()
            }
        }
    }

    /**
     * Open app info/settings screen where user can modify battery settings.
     */
    fun openAppInfoSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) { }
    }

    /**
     * Check if the device is Xiaomi/MIUI (aggressive battery optimization).
     */
    fun isXiaomiDevice(): Boolean {
        return Build.MANUFACTURER.equals("xiaomi", ignoreCase = true) ||
            Build.MANUFACTURER.equals("redmi", ignoreCase = true) ||
            Build.MANUFACTURER.equals("poco", ignoreCase = true)
    }

    /**
     * Check if the device is iQOO/Vivo (aggressive battery optimization).
     */
    fun isIqooDevice(): Boolean {
        return Build.MANUFACTURER.equals("iqoo", ignoreCase = true) ||
            Build.MANUFACTURER.equals("vivo", ignoreCase = true)
    }

    /**
     * Returns true if user should be prompted to whitelist the app for battery optimization.
     */
    fun shouldPromptForBatteryWhitelist(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return false
        if (isIgnoringBatteryOptimizations()) return false
        return isXiaomiDevice() || isIqooDevice() || Build.MANUFACTURER.equals("samsung", ignoreCase = true)
    }
}
