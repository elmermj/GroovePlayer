package com.aethelsoft.grooveplayer.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.aethelsoft.grooveplayer.MainActivity
import com.aethelsoft.grooveplayer.R
import com.aethelsoft.grooveplayer.data.transfer.NearbyTransferManager
import com.aethelsoft.grooveplayer.data.transfer.TransferNotificationBridge
import com.aethelsoft.grooveplayer.domain.repository.transfer.TransferRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service (dataSync type) for Nearby P2P file transfers.
 * Survives screen off and background restrictions.
 * Notification shows progress, ETA, and action buttons (Pause/Resume/Cancel).
 */
@AndroidEntryPoint
class NearbyTransferService : Service() {

    @Inject
    lateinit var notificationBridge: TransferNotificationBridge

    @Inject
    lateinit var transferRepository: TransferRepository

    @Inject
    lateinit var nearbyTransferManager: NearbyTransferManager

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _serviceState = MutableStateFlow<TransferServiceState>(TransferServiceState.Idle)
    val serviceState: StateFlow<TransferServiceState> = _serviceState.asStateFlow()

    private var lastNotificationUpdate = 0L
    private val NOTIFICATION_UPDATE_INTERVAL_MS = 500L

    inner class LocalBinder : Binder() {
        fun getService(): NearbyTransferService = this@NearbyTransferService
    }

    private val binder = LocalBinder()

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createChannel()
        serviceScope.launch {
            notificationBridge.state.collect { state ->
                _serviceState.value = state
                val now = System.currentTimeMillis()
                val isTerminal = state is TransferServiceState.Completed ||
                    state is TransferServiceState.Failed
                val shouldUpdate = isTerminal ||
                    state is TransferServiceState.Idle ||
                    (now - lastNotificationUpdate >= NOTIFICATION_UPDATE_INTERVAL_MS)
                if (shouldUpdate) {
                    lastNotificationUpdate = now
                    val notification = buildNotification(state)
                    val notificationManager =
                        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    if (isTerminal) {
                        // Leave completed/failed notification dismissible, then end FGS.
                        stopForeground(STOP_FOREGROUND_DETACH)
                        notificationManager.notify(NOTIFICATION_ID, notification)
                        delay(2_500)
                        // A new session may have started while we lingered; only shut
                        // down if the bridge still shows this terminal state.
                        if (notificationBridge.state.value == state) {
                            notificationManager.cancel(NOTIFICATION_ID)
                            stopSelf()
                        }
                    } else {
                        startForeground(NOTIFICATION_ID, notification)
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Contract: EVERY Context.startForegroundService() call must be answered with
        // startForeground(), no matter what state the bridge is in — otherwise the
        // system throws ForegroundServiceDidNotStartInTimeException and kills the app.
        // The state collector above never runs for a stale terminal state (StateFlow
        // doesn't re-emit an unchanged value), so this is the only reliable place.
        startForeground(NOTIFICATION_ID, buildNotification(notificationBridge.state.value))
        return START_NOT_STICKY
    }

    /** User swiped the app away: the transfer can't continue, so end it honestly. */
    override fun onTaskRemoved(rootIntent: Intent?) {
        serviceScope.launch {
            transferRepository.terminateActiveTransfers()
            nearbyTransferManager.disconnect()
            notificationBridge.reset()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    /** Legacy: use TransferNotificationBridge.updateState() instead */
    fun updateState(state: TransferServiceState) {
        notificationBridge.updateState(state)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Nearby Transfer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setShowBadge(false)
            }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(state: TransferServiceState): Notification {
        val (title, text, progress, max, indeterminate) = when (state) {
            is TransferServiceState.Idle -> NotificationData("Nearby Transfer", "Ready", 0, 100, true)
            is TransferServiceState.Connecting -> NotificationData("Connecting", state.deviceName, 0, 100, true)
            is TransferServiceState.Transferring -> {
                val pct = if (state.totalBytes > 0) {
                    ((100L * state.transferredBytes) / state.totalBytes)
                        .toInt()
                        .coerceIn(0, 100)
                } else 0
                val eta = if (state.bytesPerSec > 0 && state.totalBytes > state.transferredBytes) {
                    val remaining = (state.totalBytes - state.transferredBytes) / state.bytesPerSec
                    " • ETA: ${remaining}s"
                } else ""
                val label = state.currentFileName.ifBlank { state.deviceName }
                NotificationData("Transferring", "$label - $pct%$eta", pct, 100, false)
            }
            is TransferServiceState.Paused -> NotificationData("Paused", state.currentFileName, state.progressPercent, 100, false)
            is TransferServiceState.Completed -> NotificationData("Complete", "Transfer finished", 100, 100, false)
            is TransferServiceState.Failed -> NotificationData("Failed", state.message, 0, 100, false)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_play)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setOngoing(state !is TransferServiceState.Completed && state !is TransferServiceState.Failed)
            .setProgress(max, progress, indeterminate)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "nearby_transfer"
        private const val NOTIFICATION_ID = 3000

        fun start(context: Context) {
            val intent = Intent(context, NearbyTransferService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, NearbyTransferService::class.java))
        }
    }
}

private data class NotificationData(
    val title: String,
    val text: String,
    val progress: Int,
    val max: Int,
    val indeterminate: Boolean
)

/** State exposed by the service for UI binding */
sealed class TransferServiceState {
    data object Idle : TransferServiceState()
    data class Connecting(val deviceName: String) : TransferServiceState()
    data class Transferring(
        val deviceName: String,
        val currentFileName: String,
        val transferredBytes: Long,
        val totalBytes: Long,
        val bytesPerSec: Long,
    ) : TransferServiceState() {
        val progressPercent: Int
            get() = if (totalBytes > 0) (100 * transferredBytes / totalBytes).toInt() else 0
    }
    data class Paused(val currentFileName: String, val progressPercent: Int) : TransferServiceState()
    data object Completed : TransferServiceState()
    data class Failed(val message: String) : TransferServiceState()
}
