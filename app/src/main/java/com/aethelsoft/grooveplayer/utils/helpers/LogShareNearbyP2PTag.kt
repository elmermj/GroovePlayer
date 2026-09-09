package com.aethelsoft.grooveplayer.utils.helpers

import android.content.Context
import android.provider.Settings

/**
 * Returns the log tag for Share Nearby P2P feature: "LogShareNearbyP2P_&lt;device UUID&gt;"
 */
fun logShareNearbyP2PTag(context: Context): String {
    val deviceUuid = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ANDROID_ID
    ) ?: "unknown"
    return "LogShareNearbyP2P_$deviceUuid"
}
