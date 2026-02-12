package com.aethelsoft.grooveplayer.data.share

import android.content.Context
import android.net.Uri
import com.aethelsoft.grooveplayer.domain.model.ShareableItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TCP-based transport for share protocol.
 * Messages are length-prefixed UTF-8 JSON strings.
 */
@Singleton
class ShareTransport @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val readBuffer = ByteArray(64 * 1024) // 64KB chunks for file transfer

    suspend fun sendLine(out: OutputStream, line: String) = withContext(Dispatchers.IO) {
        val bytes = (line + "\n").toByteArray(StandardCharsets.UTF_8)
        val dos = if (out is DataOutputStream) out else DataOutputStream(out)
        dos.writeInt(bytes.size)
        dos.write(bytes)
        dos.flush()
    }

    suspend fun receiveLine(ins: InputStream): String? = withContext(Dispatchers.IO) {
        val dis = if (ins is DataInputStream) ins else DataInputStream(ins)
        val len = dis.readInt()
        if (len <= 0 || len > 1024 * 1024) return@withContext null // max 1MB per line
        val buf = ByteArray(len)
        var read = 0
        while (read < len) {
            val n = dis.read(buf, read, len - read)
            if (n < 0) return@withContext null
            read += n
        }
        String(buf, 0, len, StandardCharsets.UTF_8).trimEnd('\n')
    }

    suspend fun sendFile(
        out: OutputStream,
        uri: Uri,
        item: ShareableItem,
        onProgress: suspend (bytesTransferred: Long, totalBytes: Long) -> Unit
    ) = withContext(Dispatchers.IO) {
        val dos = if (out is DataOutputStream) out else DataOutputStream(out)
        context.contentResolver.openInputStream(uri)?.use { input ->
            var total = 0L
            var n: Int
            while (input.read(readBuffer).also { n = it } > 0) {
                dos.write(readBuffer, 0, n)
                total += n
                onProgress(total, item.sizeBytes)
            }
        }
    }

    suspend fun receiveFile(
        ins: InputStream,
        outUri: Uri,
        totalBytes: Long,
        onProgress: suspend (bytesTransferred: Long, totalBytes: Long) -> Unit
    ) = withContext(Dispatchers.IO) {
        val dis = if (ins is DataInputStream) ins else DataInputStream(ins)
        context.contentResolver.openOutputStream(outUri, "w")?.use { output ->
            var total = 0L
            while (total < totalBytes) {
                val toRead = minOf(readBuffer.size.toLong(), totalBytes - total).toInt()
                val n = dis.read(readBuffer, 0, toRead)
                if (n < 0) break
                output.write(readBuffer, 0, n)
                total += n
                onProgress(total, totalBytes)
            }
        }
    }

    fun createServerSocket(port: Int): ServerSocket = ServerSocket(port)

    suspend fun connect(host: String, port: Int, timeoutMs: Int = 15_000): Socket = withContext(Dispatchers.IO) {
        Socket().apply {
            connect(InetSocketAddress(host, port), timeoutMs)
        }
    }
}
