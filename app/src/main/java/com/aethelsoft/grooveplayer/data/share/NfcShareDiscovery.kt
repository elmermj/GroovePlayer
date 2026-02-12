package com.aethelsoft.grooveplayer.data.share

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Parcelable
import com.aethelsoft.grooveplayer.domain.model.ShareSessionInfo
import java.nio.charset.StandardCharsets

/**
 * NFC-based discovery.
 * - Sender: Uses setNdefPushMessage (Android Beam) on API < 29; deprecated on 29+.
 * - Receiver: Reads session info from NFC intent.
 * - On API 29+, Android Beam is removed; use "Share with nearby device" instead.
 */
class NfcShareDiscovery(private val activity: Activity) {

    private val nfcAdapter: NfcAdapter? by lazy {
        NfcAdapter.getDefaultAdapter(activity)
    }

    val isNfcAvailable: Boolean
        get() = nfcAdapter != null && nfcAdapter!!.isEnabled

    /** Android Beam (NDEF push) was removed in API 29. */
    val isBeamSupported: Boolean
        get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q

    fun enableForegroundDispatch() {
        val adapter = nfcAdapter ?: return
        val intent = Intent(activity, activity.javaClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val flags = PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val pendingIntent = PendingIntent.getActivity(activity, 0, intent, flags)
        adapter.enableForegroundDispatch(activity, pendingIntent, null, null)
    }

    fun disableForegroundDispatch() {
        nfcAdapter?.disableForegroundDispatch(activity)
    }

    /**
     * Set NDEF message to push when the other device taps (API < 29 only).
     * setNdefPushMessage was removed in API 29; use reflection on older devices.
     */
    @Suppress("DEPRECATION")
    fun setPushMessage(info: ShareSessionInfo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) return
        val adapter = nfcAdapter ?: return
        val payload = ShareProtocol.sessionInfoToNdefPayload(info)
        val bytes = payload.toByteArray(StandardCharsets.UTF_8)
        val record = NdefRecord(
            NdefRecord.TNF_MIME_MEDIA,
            ShareProtocol.MIME_TYPE.toByteArray(StandardCharsets.UTF_8),
            ByteArray(0),
            bytes
        )
        val message = NdefMessage(arrayOf(record))
        try {
            val method = NfcAdapter::class.java.getMethod("setNdefPushMessage", NdefMessage::class.java, Activity::class.java)
            method.invoke(adapter, message, activity)
        } catch (_: Exception) { /* API 29+ - method removed */ }
    }

    /**
     * Write session info to a physical NFC tag (e.g. sticker) - for manual "tap tag to connect".
     */
    fun writeSessionToTag(tag: Tag, info: ShareSessionInfo): Boolean {
        val payload = ShareProtocol.sessionInfoToNdefPayload(info)
        val bytes = payload.toByteArray(StandardCharsets.UTF_8)
        val record = NdefRecord(
            NdefRecord.TNF_MIME_MEDIA,
            ShareProtocol.MIME_TYPE.toByteArray(StandardCharsets.UTF_8),
            ByteArray(0),
            bytes
        )
        val message = NdefMessage(arrayOf(record))
        return try {
            android.nfc.tech.Ndef.get(tag)?.let { ndef ->
                ndef.connect()
                ndef.writeNdefMessage(message)
                ndef.close()
                true
            } ?: false
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Read session info from an NFC intent (receiver gets data from sender or tag).
     */
    fun readSessionFromIntent(intent: Intent): ShareSessionInfo? {
        val raw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES, Parcelable::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
        } ?: return null

        for (i in raw.indices) {
            val msg = raw[i] as? NdefMessage ?: continue
            for (record in msg.records) {
                if (record.type.contentEquals(ShareProtocol.MIME_TYPE.toByteArray(StandardCharsets.UTF_8))) {
                    val payload = String(record.payload, StandardCharsets.UTF_8)
                    return ShareProtocol.ndefPayloadToSessionInfo(payload)
                }
            }
        }
        return null
    }
}
