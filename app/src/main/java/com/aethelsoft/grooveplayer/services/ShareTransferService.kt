package com.aethelsoft.grooveplayer.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.aethelsoft.grooveplayer.R
import com.aethelsoft.grooveplayer.data.share.ShareTransferState
import com.aethelsoft.grooveplayer.data.share.ShareTransferManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val CHANNEL_ID = "share_transfer"
private const val NOTIFICATION_ID = 2000

@AndroidEntryPoint
class ShareTransferService : Service() {

    @Inject
    lateinit var transferManager: ShareTransferManager

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        serviceScope.launch {
            transferManager.state.collectLatest { state ->
                val notification = buildNotification(state)
                startForeground(NOTIFICATION_ID, notification)
                if (state is ShareTransferState.Done || state is ShareTransferState.Error) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Share Transfer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setShowBadge(false)
            }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(state: ShareTransferState): Notification {
        val (title, text) = when (state) {
            is ShareTransferState.Connecting -> "Sharing" to "Connecting..."
            is ShareTransferState.Offering -> "Sharing" to "Waiting for receiver..."
            is ShareTransferState.WaitingApproval -> "Sharing" to "Waiting for approval..."
            is ShareTransferState.Transferring -> {
                val pct = if (state.totalBytes > 0) {
                    (100 * state.bytesTransferred / state.totalBytes).toInt()
                } else 0
                "Sharing" to "${state.currentItem.title} - $pct% (${state.itemIndex + 1}/${state.totalItems})"
            }
            is ShareTransferState.Done -> "Share complete" to "Transfer finished"
            is ShareTransferState.Error -> "Share failed" to state.message
            else -> "Sharing" to "In progress..."
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_play)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(state !is ShareTransferState.Done && state !is ShareTransferState.Error)
            .setProgress(
                if (state is ShareTransferState.Transferring && state.totalBytes > 0) 100 else 0,
                if (state is ShareTransferState.Transferring) (100 * state.bytesTransferred / state.totalBytes).toInt() else 0,
                false
            )
            .build()
    }

    companion object {
        fun start(context: Context, isSender: Boolean) {
            val intent = Intent(context, ShareTransferService::class.java).apply {
                putExtra("is_sender", isSender)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ShareTransferService::class.java))
        }
    }
}
