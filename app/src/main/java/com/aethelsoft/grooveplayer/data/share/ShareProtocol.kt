package com.aethelsoft.grooveplayer.data.share

import com.aethelsoft.grooveplayer.domain.model.ShareableItem
import com.aethelsoft.grooveplayer.domain.model.ShareSessionInfo
import org.json.JSONArray
import org.json.JSONObject
import java.net.ServerSocket
import java.util.UUID

/**
 * Simple newline-delimited JSON protocol for share handshake and transfer.
 */
object ShareProtocol {

    const val MIME_TYPE = "application/vnd.grooveplayer.share"
    const val DEFAULT_PORT = 38472

    // Message types
    const val MSG_OFFER = "offer"
    const val MSG_APPROVE = "approve"
    const val MSG_REJECT = "reject"
    const val MSG_FILE_START = "file_start"
    const val MSG_FILE_CHUNK = "file_chunk"
    const val MSG_FILE_END = "file_end"
    const val MSG_PROGRESS = "progress"
    const val MSG_DONE = "done"
    const val MSG_ERROR = "error"

    fun encodeOffer(items: List<ShareableItem>): String {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("artist", item.artist)
                    put("album", item.album ?: "")
                    put("sizeBytes", item.sizeBytes)
                    put("mimeType", item.mimeType)
                }
            )
        }
        return JSONObject()
            .put("type", MSG_OFFER)
            .put("items", arr)
            .toString()
    }

    fun decodeOffer(json: String): List<ShareableItem> {
        val obj = JSONObject(json)
        if (obj.optString("type") != MSG_OFFER) return emptyList()
        val arr = obj.getJSONArray("items")
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            ShareableItem(
                id = o.getString("id"),
                title = o.getString("title"),
                artist = o.getString("artist"),
                album = o.optString("album").takeIf { it.isNotEmpty() },
                sizeBytes = o.getLong("sizeBytes"),
                mimeType = o.optString("mimeType", "audio/mpeg")
            )
        }
    }

    fun encodeApprove(ids: List<String>): String {
        val arr = JSONArray()
        ids.forEach { arr.put(it) }
        return JSONObject()
            .put("type", MSG_APPROVE)
            .put("ids", arr)
            .toString()
    }

    fun decodeApprove(json: String): List<String> {
        val obj = JSONObject(json)
        if (obj.optString("type") != MSG_APPROVE) return emptyList()
        val arr = obj.getJSONArray("ids")
        return (0 until arr.length()).map { arr.getString(it) }
    }

    fun encodeReject(): String =
        JSONObject().put("type", MSG_REJECT).toString()

    fun encodeFileStart(id: String, sizeBytes: Long): String =
        JSONObject()
            .put("type", MSG_FILE_START)
            .put("id", id)
            .put("sizeBytes", sizeBytes)
            .toString()

    fun encodeProgress(id: String, bytesTransferred: Long, totalBytes: Long): String =
        JSONObject()
            .put("type", MSG_PROGRESS)
            .put("id", id)
            .put("bytesTransferred", bytesTransferred)
            .put("totalBytes", totalBytes)
            .toString()

    fun encodeDone(): String =
        JSONObject().put("type", MSG_DONE).toString()

    fun encodeError(message: String): String =
        JSONObject()
            .put("type", MSG_ERROR)
            .put("message", message)
            .toString()

    fun decodeMessage(json: String): Pair<String, JSONObject>? {
        return try {
            val obj = JSONObject(json)
            obj.optString("type") to obj
        } catch (_: Exception) {
            null
        }
    }

    fun sessionInfoToNdefPayload(info: ShareSessionInfo): String {
        return JSONObject()
            .put("host", info.host)
            .put("port", info.port)
            .put("token", info.sessionToken)
            .put("name", info.deviceName)
            .toString()
    }

    fun ndefPayloadToSessionInfo(payload: String): ShareSessionInfo? {
        return try {
            val obj = JSONObject(payload)
            ShareSessionInfo(
                host = obj.getString("host"),
                port = obj.getInt("port"),
                sessionToken = obj.getString("token"),
                deviceName = obj.optString("name", "Device")
            )
        } catch (_: Exception) {
            null
        }
    }

    fun generateSessionToken(): String = UUID.randomUUID().toString()

    fun createServerSocket(port: Int = 0): ServerSocket = ServerSocket(port)
}
