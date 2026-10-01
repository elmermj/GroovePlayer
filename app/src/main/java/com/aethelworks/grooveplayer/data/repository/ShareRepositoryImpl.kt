package com.aethelworks.grooveplayer.data.repository

import android.content.Context
import android.net.Uri
import com.aethelworks.grooveplayer.data.backup.GrooveDownloadsLocator
import com.aethelworks.grooveplayer.data.library.SongIdentityStore
import com.aethelworks.grooveplayer.data.share.NsdShareDiscovery
import com.aethelworks.grooveplayer.data.share.ShareProtocol
import com.aethelworks.grooveplayer.data.share.ShareTransferManager
import com.aethelworks.grooveplayer.data.share.ShareTransferState
import com.aethelworks.grooveplayer.data.share.ShareTransport
import com.aethelworks.grooveplayer.domain.backup.ContentHash
import com.aethelworks.grooveplayer.domain.library.PrivateLibrarySongs
import com.aethelworks.grooveplayer.domain.library.SongHashRemap
import com.aethelworks.grooveplayer.domain.model.ShareableItem
import com.aethelworks.grooveplayer.domain.model.ShareSessionInfo
import com.aethelworks.grooveplayer.domain.model.Song
import com.aethelworks.grooveplayer.domain.repository.MusicRepository
import com.aethelworks.grooveplayer.domain.repository.ShareRepository
import com.aethelworks.grooveplayer.domain.transfer.ReceiveFailure
import com.aethelworks.grooveplayer.domain.transfer.ReceivedTransferIntake
import com.aethelworks.grooveplayer.domain.transfer.userMessage
import com.aethelworks.grooveplayer.services.ShareTransferService
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
    private val nsdDiscovery: NsdShareDiscovery,
    private val grooveDownloads: GrooveDownloadsLocator,
    private val songIdentity: SongIdentityStore,
    private val musicRepository: MusicRepository,
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

        val libraryRoot = grooveDownloads.directory()
        libraryRoot.mkdirs()
        val incoming = File(libraryRoot, PrivateLibrarySongs.INCOMING_DIR)
        incoming.mkdirs()
        val known = SongHashRemap.privateHashes(songIdentity.rows()).toMutableSet()
        var failure: String? = null
        var stored = 0
        val pendingCatalog = mutableListOf<Triple<String, String, String>>()
        try {
            for ((index, item) in items.withIndex()) {
                if (cancelled) {
                    failure = "Cancelled"
                    break
                }
                val line = transport.receiveLine(ins) ?: run {
                    failure = ReceiveFailure.SIZE_MISMATCH.userMessage()
                    break
                }
                val (type, obj) = ShareProtocol.decodeMessage(line) ?: run {
                    failure = ReceiveFailure.UNREADABLE.userMessage()
                    break
                }
                if (type != ShareProtocol.MSG_FILE_START) {
                    failure = ReceiveFailure.UNREADABLE.userMessage()
                    break
                }
                val sizeBytes = obj.optLong("sizeBytes", -1L)
                val checksum = obj.optString("checksum").ifBlank { item.checksum }
                val sentName = obj.optString("fileName").ifBlank { item.fileName }
                if (sizeBytes != item.sizeBytes || sizeBytes <= 0L || sizeBytes > ReceivedTransferIntake.MAX_FILE_BYTES) {
                    failure = if (sizeBytes > ReceivedTransferIntake.MAX_FILE_BYTES) {
                        ReceiveFailure.OVER_LIMIT.userMessage()
                    } else {
                        ReceiveFailure.SIZE_MISMATCH.userMessage()
                    }
                    break
                }
                val displayName = ReceivedTransferIntake.displayName(sentName, item.title, item.mimeType)
                val staged = File(incoming, "$displayName.partial")
                transferManager.setTransferring(item, 0, sizeBytes, index, items.size)
                val written = transport.receiveFile(ins, staged, sizeBytes) { sent, total ->
                    transferManager.setTransferring(item, sent, total, index, items.size)
                }
                if (written != sizeBytes) {
                    if (staged.exists()) staged.delete()
                    failure = if (written < 0L) {
                        ReceiveFailure.OVER_LIMIT.userMessage()
                    } else {
                        ReceiveFailure.SIZE_MISMATCH.userMessage()
                    }
                    break
                }
                val decision = ReceivedTransferIntake.acceptStaged(
                    staged = staged,
                    libraryRoot = libraryRoot,
                    displayName = displayName,
                    expectedSize = sizeBytes,
                    expectedChecksum = checksum,
                    knownPrivateHashes = known,
                )
                val hash = ReceivedTransferIntake.catalogRowOrNull(decision, libraryRoot)
                if (hash != null && decision.libraryPath != null) {
                    known += hash
                    val title = item.title.ifBlank { displayName.substringBeforeLast('.', displayName) }
                    pendingCatalog += Triple(hash, decision.libraryPath, title)
                } else if (!decision.alreadyInLibrary) {
                    failure = decision.failure?.userMessage() ?: ReceiveFailure.CHECKSUM_MISMATCH.userMessage()
                    break
                }
            }
            val reason = failure
            if (reason != null) {
                pendingCatalog.forEach { (_, path, _) -> File(path).delete() }
                incoming.listFiles()?.forEach { child -> if (child.isFile) child.delete() }
                transferManager.setError(reason)
            } else {
                for ((hash, path, title) in pendingCatalog) {
                    songIdentity.adoptVerifiedCopy(
                        hash = hash,
                        libraryPath = path,
                        title = title,
                        durationMs = 0L,
                    )
                    stored++
                }
                transport.receiveLine(ins) // MSG_DONE
                transferManager.setDone()
            }
        } catch (e: Exception) {
            pendingCatalog.forEach { (_, path, _) -> File(path).delete() }
            incoming.listFiles()?.forEach { child -> if (child.isFile) child.delete() }
            stored = 0
            val message = e.message ?: "Transfer failed"
            failure = message
            if (!cancelled) transferManager.setError(message)
        } finally {
            clearPendingReceiver()
            socket.close()
            ShareTransferService.stop(context)
        }
        if (stored > 0 && failure == null) {
            musicRepository.bumpCatalogGeneration()
        }
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
        val file = song.filePath?.let { File(it) }?.takeIf { it.isFile }
        val uri = Uri.parse(song.uri)
        val size = file?.length() ?: try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                pfd.statSize
            } ?: 0L
        } catch (_: Exception) {
            0L
        }
        val checksum = when {
            file != null -> ContentHash.sha256(file)
            else -> try {
                context.contentResolver.openInputStream(uri)?.use { ContentHash.sha256(it) }
            } catch (_: Exception) {
                null
            }
        }
        val fileName = file?.name ?: uri.lastPathSegment
        return ShareableItem(
            id = song.id,
            title = song.title,
            artist = song.artist,
            album = song.album?.name,
            sizeBytes = size,
            mimeType = mimeForFileName(fileName),
            fileName = fileName,
            checksum = checksum,
        )
    }

    private fun mimeForFileName(fileName: String?): String {
        return when (ReceivedTransferIntake.extensionOf(fileName.orEmpty())) {
            "mp3" -> "audio/mpeg"
            "flac" -> "audio/flac"
            "m4a" -> "audio/mp4"
            "aac" -> "audio/aac"
            "ogg" -> "audio/ogg"
            "opus" -> "audio/opus"
            "wav" -> "audio/wav"
            else -> "application/octet-stream"
        }
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
            transport.sendLine(
                out,
                ShareProtocol.encodeFileStart(item.id, item.sizeBytes, item.checksum, item.fileName),
            )
            transport.sendFile(out, Uri.parse(song.uri), item) { sent, total ->
                manager.setTransferring(item, sent, total, index, items.size)
            }
        }
    }
}
