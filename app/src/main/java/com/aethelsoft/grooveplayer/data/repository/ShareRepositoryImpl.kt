package com.aethelsoft.grooveplayer.data.repository

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.aethelsoft.grooveplayer.data.share.NsdShareDiscovery
import com.aethelsoft.grooveplayer.data.share.ShareProtocol
import com.aethelsoft.grooveplayer.data.share.ShareTransferManager
import com.aethelsoft.grooveplayer.data.share.ShareTransferState
import com.aethelsoft.grooveplayer.data.share.ShareTransport
import com.aethelsoft.grooveplayer.domain.model.ShareableItem
import com.aethelsoft.grooveplayer.domain.model.ShareSessionInfo
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.domain.repository.ShareRepository
import com.aethelsoft.grooveplayer.services.ShareTransferService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.net.ServerSocket
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShareRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val transport: ShareTransport,
    private val transferManager: ShareTransferManager,
    private val nsdDiscovery: NsdShareDiscovery
) : ShareRepository {

    override val transferState: StateFlow<ShareTransferState> =
        transferManager.state

    private var serverSocket: ServerSocket? = null
    private var cancelled = false

    // Pending receiver connection - held between connectAndReceiveOffer and approveAndReceive
    private var pendingSocket: Socket? = null
    private var pendingIns: DataInputStream? = null
    private var pendingOut: DataOutputStream? = null
    private var pendingItems: List<ShareableItem>? = null

    override suspend fun startSender(items: List<Song>, sessionInfo: ShareSessionInfo) =
        withContext(Dispatchers.IO) {
            cancelled = false
            transferManager.setConnecting()
            ShareTransferService.start(context, isSender = true)

            val shareables = items.map { songToShareable(it) }
            serverSocket = ShareProtocol.createServerSocket(0)
            val port = serverSocket!!.localPort
            nsdDiscovery.register(port, sessionInfo.sessionToken, sessionInfo.deviceName)
            transferManager.setOffering(shareables)
            transferManager.setWaitingApproval()

            try {
                val client = serverSocket!!.accept()
                val ins = DataInputStream(client.getInputStream())
                val out = DataOutputStream(client.getOutputStream())

                transport.sendLine(out, ShareProtocol.encodeOffer(shareables))
                val line = transport.receiveLine(ins) ?: run {
                    transport.sendLine(out, ShareProtocol.encodeReject())
                    return@withContext
                }
                val (type, _) = ShareProtocol.decodeMessage(line) ?: run {
                    transport.sendLine(out, ShareProtocol.encodeReject())
                    return@withContext
                }
                when (type) {
                    ShareProtocol.MSG_REJECT -> {
                        transferManager.setError("Receiver declined")
                        return@withContext
                    }
                    ShareProtocol.MSG_APPROVE -> {
                        val approvedIds = ShareProtocol.decodeApprove(line)
                        val toSend = shareables.filter { it.id in approvedIds }
                        if (toSend.isEmpty()) {
                            transport.sendLine(out, ShareProtocol.encodeError("No items approved"))
                            return@withContext
                        }
                        sendFiles(context, transport, out, toSend, items, transferManager)
                    }
                    else -> {
                        transport.sendLine(out, ShareProtocol.encodeError("Unexpected message"))
                    }
                }
                transport.sendLine(out, ShareProtocol.encodeDone())
                transferManager.setDone()
            } catch (e: Exception) {
                if (!cancelled) transferManager.setError(e.message ?: "Transfer failed")
            } finally {
                nsdDiscovery.unregister()
                serverSocket?.close()
                serverSocket = null
                ShareTransferService.stop(context)
            }
        }

    override suspend fun connectAndReceiveOffer(sessionInfo: ShareSessionInfo): List<ShareableItem>? =
        withContext(Dispatchers.IO) {
            cancelled = false
            clearPendingReceiver()
            transferManager.setConnecting()
            try {
                val socket = transport.connect(sessionInfo.host, sessionInfo.port)
                val ins = DataInputStream(socket.getInputStream())
                val out = DataOutputStream(socket.getOutputStream())

                val line = transport.receiveLine(ins) ?: return@withContext null
                val (type, _) = ShareProtocol.decodeMessage(line) ?: return@withContext null
                if (type != ShareProtocol.MSG_OFFER) return@withContext null

                val items = ShareProtocol.decodeOffer(line)
                pendingSocket = socket
                pendingIns = ins
                pendingOut = out
                pendingItems = items
                transferManager.setOffering(items)
                items
            } catch (e: Exception) {
                transferManager.setError(e.message ?: "Connection failed")
                null
            }
        }

    override suspend fun approveAndReceive(approvedIds: List<String>) = withContext(Dispatchers.IO) {
        val socket = pendingSocket ?: run {
            transferManager.setError("Connection lost")
            return@withContext
        }
        val ins = pendingIns!!
        val out = pendingOut!!
        val items = pendingItems!!.filter { it.id in approvedIds }
        if (items.isEmpty()) {
            transport.sendLine(out, ShareProtocol.encodeReject())
            clearPendingReceiver()
            transferManager.setIdle()
            return@withContext
        }

        ShareTransferService.start(context, isSender = false)
        transport.sendLine(out, ShareProtocol.encodeApprove(approvedIds))

        val dir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
            ?: context.filesDir
        items.forEachIndexed { index, item ->
            if (cancelled) return@forEachIndexed
            val line = transport.receiveLine(ins) ?: return@forEachIndexed
            val (type, obj) = ShareProtocol.decodeMessage(line) ?: return@forEachIndexed
            if (type != ShareProtocol.MSG_FILE_START) return@forEachIndexed
            val id = obj.optString("id")
            val sizeBytes = obj.optLong("sizeBytes", 0)
            val safeTitle = item.title.replace(Regex("[\\\\/:*?\"<>|]"), "_")
            val file = File(dir, "${safeTitle}_${item.id.take(8)}.mp3")
            val uri = Uri.fromFile(file)
            transferManager.setTransferring(item, 0, sizeBytes, index, items.size)
            transport.receiveFile(ins, uri, sizeBytes) { sent, total ->
                transferManager.setTransferring(item, sent, total, index, items.size)
            }
        }
        transport.receiveLine(ins) // MSG_DONE
        clearPendingReceiver()
        socket.close()
        transferManager.setDone()
        ShareTransferService.stop(context)
    }

    override suspend fun rejectOffer() = withContext(Dispatchers.IO) {
        pendingOut?.let { transport.sendLine(it, ShareProtocol.encodeReject()) }
        clearPendingReceiver()
        transferManager.setIdle()
    }

    private fun clearPendingReceiver() {
        pendingSocket?.close()
        pendingSocket = null
        pendingIns = null
        pendingOut = null
        pendingItems = null
    }

    override fun cancelTransfer() {
        cancelled = true
        serverSocket?.close()
        serverSocket = null
        ShareTransferService.stop(context)
        transferManager.setIdle()
    }

    private fun songToShareable(song: Song): ShareableItem {
        val size = try {
            context.contentResolver.openFileDescriptor(Uri.parse(song.uri), "r")?.use { pfd ->
                pfd.statSize
            } ?: 0L
        } catch (_: Exception) {
            0L
        }
        return ShareableItem(
            id = song.id,
            title = song.title,
            artist = song.artist,
            album = song.album?.name,
            sizeBytes = size,
            mimeType = "audio/mpeg"
        )
    }

    private suspend fun sendFiles(
        ctx: Context,
        transport: ShareTransport,
        out: DataOutputStream,
        items: List<ShareableItem>,
        songs: List<Song>,
        manager: ShareTransferManager
    ) {
        val songMap = songs.associateBy { it.id }
        items.forEachIndexed { index, item ->
            if (cancelled) return
            val song = songMap[item.id] ?: return@forEachIndexed
            manager.setTransferring(item, 0, item.sizeBytes, index, items.size)
            transport.sendLine(out, ShareProtocol.encodeFileStart(item.id, item.sizeBytes))
            transport.sendFile(out, Uri.parse(song.uri), item) { sent, total ->
                manager.setTransferring(item, sent, total, index, items.size)
            }
        }
    }
}
