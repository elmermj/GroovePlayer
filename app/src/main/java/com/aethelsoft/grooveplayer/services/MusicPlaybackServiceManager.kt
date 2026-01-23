package com.aethelsoft.grooveplayer.services

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import com.aethelsoft.grooveplayer.utils.checkNotificationPermission
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager class to control the MusicPlaybackService.
 * This class provides a simple interface to start and stop the foreground service.
 */
@Singleton
class MusicPlaybackServiceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private var isServiceRunning = false
    
    /**
     * Starts the music playback foreground service.
     * Call this when playback begins.
     */
    fun startService() {
        if (!isServiceRunning) {
            // Check notification permission and log warning if disabled
            val notificationsEnabled = checkNotificationPermission(context)
            if (!notificationsEnabled) {
                android.util.Log.w(
                    "MusicPlaybackServiceManager",
                    "⚠️ Notifications are disabled! " +
                    "Please enable in Settings → Apps → GroovePlayer → Notifications. " +
                    "The notification may not appear."
                )
            }
            
            val intent = Intent(context, MusicPlaybackService::class.java)
            
            android.util.Log.d("MusicPlaybackServiceManager", "Starting MusicPlaybackService")
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, intent)
            } else {
                context.startService(intent)
            }
            
            isServiceRunning = true
        } else {
            android.util.Log.d("MusicPlaybackServiceManager", "Service already running, skipping start")
        }
    }
    
    /**
     * Stops the music playback foreground service.
     * Call this when the app is closing or when there's no active playback session.
     */
    fun stopService() {
        if (isServiceRunning) {
            val intent = Intent(context, MusicPlaybackService::class.java)
            context.stopService(intent)
            isServiceRunning = false
        }
    }
    
    /**
     * Checks if the service is currently running.
     */
    fun isRunning(): Boolean = isServiceRunning
}
