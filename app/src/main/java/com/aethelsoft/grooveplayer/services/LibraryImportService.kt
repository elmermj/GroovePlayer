package com.aethelsoft.grooveplayer.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.aethelsoft.grooveplayer.R
import com.aethelsoft.grooveplayer.data.library.LibraryImportController
import com.aethelsoft.grooveplayer.data.library.LibraryImportUiState
import com.aethelsoft.grooveplayer.domain.library.ImportPromptCopy
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LibraryImportService : Service() {

    @Inject lateinit var controller: LibraryImportController

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createChannel()
        val notification = notification("Importing", "Starting import")
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            },
        )
        if (intent?.action == ACTION_CANCEL) {
            controller.requestCancel()
            return START_NOT_STICKY
        }
        val uri = intent?.data
        if (uri == null) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        scope.launch {
            val updates = launch {
                controller.state.collect { state ->
                    if (state is LibraryImportUiState.Running) {
                        val manager = getSystemService(NotificationManager::class.java)
                        manager.notify(
                            NOTIFICATION_ID,
                            notification(
                                ImportPromptCopy.progress(state.completed, state.total),
                                state.fileName,
                            ),
                        )
                    }
                }
            }
            try {
                controller.run(uri)
            } finally {
                updates.cancel()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(CHANNEL_ID, "Library import", NotificationManager.IMPORTANCE_LOW)
        manager.createNotificationChannel(channel)
    }

    private fun notification(title: String, text: String): Notification {
        val cancel = PendingIntent.getService(
            this,
            1,
            Intent(this, LibraryImportService::class.java).setAction(ACTION_CANCEL),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_transparent)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .addAction(0, "Cancel", cancel)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "library_import"
        private const val NOTIFICATION_ID = 84
        private const val ACTION_CANCEL = "com.aethelsoft.grooveplayer.IMPORT_CANCEL"

        fun start(context: Context, treeUri: android.net.Uri) {
            val intent = Intent(context, LibraryImportService::class.java).setData(treeUri)
            androidx.core.content.ContextCompat.startForegroundService(context, intent)
        }
    }
}
