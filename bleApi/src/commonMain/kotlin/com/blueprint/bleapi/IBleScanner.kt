package com.blueprint.bleapi

import com.blueprint.bleapi.model.BtDevice
import com.blueprint.bleapi.model.ConnectionState
import com.blueprint.bleapi.model.DeviceCharacteristic
import com.blueprint.bleapi.model.DeviceService
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
interface IBleScanner {
    val devices: Flow<List<BtDevice>>

    fun getDiscoveredDevices(): List<BtDevice>

    @Throws(TimeoutCancellationException::class)
    suspend fun connect(device: BtDevice)

    fun observeConnectionState(device: BtDevice): Flow<ConnectionState>

    suspend fun getDeviceServices(device: BtDevice): List<DeviceService>

    @Throws(IllegalStateException::class, IllegalArgumentException::class)
    fun observeCharacteristicNotifications(
        device: BtDevice,
        serviceUuid: Uuid,
        characteristic: Uuid
    ): Flow<ByteArray>

    @Throws(IllegalStateException::class, IllegalArgumentException::class)
    fun observeCharacteristicNotifications(
        device: BtDevice,
        characteristic: DeviceCharacteristic
    ): Flow<ByteArray>

    fun findCharacteristicsInConnections(
        uuid: Uuid,
        serviceUuid: Uuid? = null,
        device: BtDevice? = null,
    ): List<DeviceCharacteristic>

    @Throws(IllegalStateException::class, IllegalArgumentException::class)
    suspend fun sendCommand(
        device: BtDevice,
        serviceUuid: Uuid,
        characteristic: Uuid,
        command: ByteArray
    )

    fun findConnectedDeviceByAddress(address: String): BtDevice?

    fun disconnect(device: BtDevice)
}