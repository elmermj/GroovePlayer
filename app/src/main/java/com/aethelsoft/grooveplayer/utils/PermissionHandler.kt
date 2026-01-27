package com.aethelsoft.grooveplayer.utils

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
fun rememberAudioPermissionState(): Pair<Boolean, () -> Unit> {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
    }

    val requestPermission: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launcher.launch(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            launcher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    return Pair(hasPermission, requestPermission)
}

/**
 * Runtime permission helper for RECORD_AUDIO, used by the audio visualizer.
 *
 * Even though the visualizer only reads the currently playing audio session,
 * Android treats it as microphone access and requires this permission on
 * Android 6.0+ devices.
 */
@Composable
fun rememberRecordAudioPermissionState(): Pair<Boolean, () -> Unit> {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
    }

    val requestPermission: () -> Unit = {
        launcher.launch(Manifest.permission.RECORD_AUDIO)
    }

    return Pair(hasPermission, requestPermission)
}

@Composable
fun rememberBluetoothPermissionState(): Pair<Boolean, () -> Unit> {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_SCAN
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH
                ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_ADMIN
                ) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions[Manifest.permission.BLUETOOTH_CONNECT] == true &&
            permissions[Manifest.permission.BLUETOOTH_SCAN] == true
        } else {
            permissions[Manifest.permission.BLUETOOTH] == true &&
            permissions[Manifest.permission.BLUETOOTH_ADMIN] == true
        }
    }

    val requestPermission: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            launcher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
                )
            )
        } else {
            launcher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN
                )
            )
        }
    }

    return Pair(hasPermission, requestPermission)
}

@Composable
fun rememberNotificationPermissionState(): Pair<Boolean, () -> Unit> {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Check both permission and notification manager state
                val hasRuntimePermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                
                // Also check if notifications are actually enabled in system settings
                val notificationManager = context.getSystemService(android.app.NotificationManager::class.java)
                val notificationsEnabled = notificationManager?.areNotificationsEnabled() ?: true
                
                hasRuntimePermission && notificationsEnabled
            } else {
                // Android 12 and below - notifications are always enabled
                true
            }
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Re-check both permission and notification manager state
            val notificationManager = context.getSystemService(android.app.NotificationManager::class.java)
            val notificationsEnabled = notificationManager?.areNotificationsEnabled() ?: true
            isGranted && notificationsEnabled
        } else {
            true
        }
    }

    val requestPermission: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        // Android 12 and below don't need permission request
    }

    return Pair(hasPermission, requestPermission)
}

/**
 * Utility function to check if notification permission is granted.
 * Can be called from anywhere in the app (not just from Composable functions).
 * 
 * @param context The application context
 * @return true if notifications are enabled, false otherwise
 */
fun checkNotificationPermission(context: android.content.Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val hasRuntimePermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        
        val notificationManager = context.getSystemService(android.app.NotificationManager::class.java)
        val notificationsEnabled = notificationManager?.areNotificationsEnabled() ?: true
        
        return hasRuntimePermission && notificationsEnabled
    }
    return true // Android 12 and below - notifications are always enabled
}