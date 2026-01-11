package com.aethelsoft.grooveplayer.data.bluetooth.mapper

import com.aethelsoft.grooveplayer.data.bluetooth.model.BluetoothDeviceData
import com.aethelsoft.grooveplayer.domain.model.BluetoothDevice

/**
 * Mapper to convert between data layer BluetoothDeviceData and domain layer BluetoothDevice.
 */
internal object BluetoothDeviceMapper {
    
    fun toDomain(data: BluetoothDeviceData): BluetoothDevice {
        return BluetoothDevice(
            name = data.name,
            address = data.address,
            isConnected = data.isConnected
        )
    }
    
    fun toDomainList(dataList: List<BluetoothDeviceData>): List<BluetoothDevice> {
        return dataList.map { toDomain(it) }
    }
}
