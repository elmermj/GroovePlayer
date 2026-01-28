package com.aethelsoft.grooveplayer.utils.helpers

import androidx.compose.ui.graphics.vector.ImageVector
import com.aethelsoft.grooveplayer.utils.theme.icons.XBluetooth
import com.aethelsoft.grooveplayer.utils.theme.icons.XHeadphones
import com.aethelsoft.grooveplayer.utils.theme.icons.XSmartphone
import com.aethelsoft.grooveplayer.utils.theme.icons.XSpeaker

enum class BluetoothDeviceType {
    EARBUDS, SPEAKER, PHONE, HEADPHONES, UNKNOWN
}

object BluetoothHelpers {
    fun normalizedNameKey(name: String): String = name.lowercase().trim()

    fun detectBluetoothDeviceType(name: String): BluetoothDeviceType {
        val n = name.lowercase()
        return when {
            n.contains("bud") || n.contains("airpod") || n.contains("tws") ->
                BluetoothDeviceType.EARBUDS
            n.contains("headphone") || n.contains("xm") || n.contains("beats") ->
                BluetoothDeviceType.HEADPHONES
            n.contains("speaker") || n.contains("jbl") || n.contains("sound") ->
                BluetoothDeviceType.SPEAKER
            n.contains("phone") || n.contains("pixel") || n.contains("iphone") ->
                BluetoothDeviceType.PHONE
            else -> BluetoothDeviceType.UNKNOWN
        }
    }

    fun bluetoothIconFor(type: BluetoothDeviceType): ImageVector = when (type) {
        BluetoothDeviceType.EARBUDS,
        BluetoothDeviceType.HEADPHONES -> XHeadphones
        BluetoothDeviceType.SPEAKER -> XSpeaker
        BluetoothDeviceType.PHONE -> XSmartphone
        BluetoothDeviceType.UNKNOWN -> XBluetooth
    }

    fun connectedBtIconFromName(name: String?): ImageVector =
        bluetoothIconFor(detectBluetoothDeviceType(name.orEmpty()))

}

