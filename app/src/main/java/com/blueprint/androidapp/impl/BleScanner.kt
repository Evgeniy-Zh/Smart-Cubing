package com.blueprint.androidapp.impl

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.util.Log
import com.blueprint.bleapi.IBleScanner
import com.blueprint.bleapi.model.BtDevice
import com.blueprint.bleapi.model.ConnectionState
import com.blueprint.bleapi.model.DeviceCharacteristic
import com.blueprint.bleapi.model.DeviceService
import com.blueprint.bleapi.model.Property
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.UUID
import kotlin.collections.iterator
import kotlin.collections.toList
import kotlin.coroutines.resumeWithException
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
@SuppressLint("MissingPermission")
class BleScanner(private val context: Context) : IBleScanner {

    private data class DeviceConnection(
        val device: BtDevice,
        var gatt: BluetoothGatt? = null,
    )

    private data class ConnectionStateChange(
        val deviceAddress: String,
        val state: ConnectionState
    )

    private class CharacteristicNotification(
        val deviceAddress: String,
        val characteristicUuid: Uuid,
        val serviceUuid: Uuid,
        val value: ByteArray
    )

    private val characteristicNotifications = MutableSharedFlow<CharacteristicNotification?>(extraBufferCapacity = 20)
    private val connectionStateChanges = MutableSharedFlow<ConnectionStateChange>(extraBufferCapacity = 5)

    private val adapter = BluetoothAdapter.getDefaultAdapter()
    private val connections: MutableMap<String, DeviceConnection> = mutableMapOf()
    private val discoveredDevices: MutableMap<String, BtDevice> = mutableMapOf()

    override val devices: Flow<List<BtDevice>> = callbackFlow {
        val scanner = adapter.bluetoothLeScanner

        val filter = ScanFilter.Builder().build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
            .build()

        val scanCallback = object : ScanCallback() {
            override fun onScanResult(code: Int, result: ScanResult) {
                val device = BtDevice(result.device)
                discoveredDevices[device.address] = device
                trySend(device)
            }
        }

        scanner.startScan(listOf(filter), settings, scanCallback)
        awaitClose { scanner.stopScan(scanCallback) }
    }.scan(mutableListOf<BtDevice>()) { devices, device ->
        // Добавляем новое устройство в список если его нет
        if (!devices.any { it.address == device.address }) {
            Log.d("BLE", "found device: ${device.name}")
            devices.add(device)
        }
        devices
    }.map { it.toList() }

    override fun getDiscoveredDevices(): List<BtDevice> {
        return discoveredDevices.values.toList()
    }

    @OptIn(ExperimentalStdlibApi::class)
    override suspend fun connect(device: BtDevice) {
        try {
            withTimeout(5_000) {
                suspendCancellableCoroutine<Unit> { continuation ->
                    val gattCallback = object : BluetoothGattCallback() {
                        override fun onConnectionStateChange(
                            gatt: BluetoothGatt,
                            status: Int,
                            newState: Int
                        ) {

                            if (status != BluetoothGatt.GATT_SUCCESS) {
                                connectionStateChanges.tryEmit(
                                    ConnectionStateChange(
                                        deviceAddress = device.address,
                                        state = ConnectionState.CONNECTION_FAILED
                                    )
                                )
                                continuation.resumeWithException(Exception("Failed to connect to device: $device"))
                            }

                            val address = gatt.device.address
                            when (newState) {
                                BluetoothProfile.STATE_CONNECTED -> {
                                    Log.d("BLE", "Connected to ${address}")
                                    connectionStateChanges.tryEmit(
                                        ConnectionStateChange(
                                            deviceAddress = address,
                                            state = ConnectionState.CONNECTED
                                        )
                                    )
                                    gatt.discoverServices()
                                }

                                BluetoothProfile.STATE_DISCONNECTED -> {
                                    Log.d("BLE", "Disconnected from ${address}")
                                    connectionStateChanges.tryEmit(
                                        ConnectionStateChange(
                                            deviceAddress = address,
                                            state = ConnectionState.DISCONNECTED
                                        )
                                    )
                                    connections.remove(address)
                                }

                                BluetoothProfile.STATE_CONNECTING -> {
                                    Log.d("BLE", "Connecting ${address}")
                                    connectionStateChanges.tryEmit(
                                        ConnectionStateChange(
                                            deviceAddress = address,
                                            state = ConnectionState.CONNECTING
                                        )
                                    )
                                }

                                BluetoothProfile.STATE_DISCONNECTING -> {
                                    Log.d("BLE", "Disconnecting ${address}")
                                    connectionStateChanges.tryEmit(
                                        ConnectionStateChange(
                                            deviceAddress = address,
                                            state = ConnectionState.DISCONNECTING
                                        )
                                    )
                                }
                            }
                        }


                        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                Log.d("BLE", "Services discovered for ${gatt.device.name}")
                                connectionStateChanges.tryEmit(
                                    ConnectionStateChange(
                                        deviceAddress = gatt.device.address,
                                        state = ConnectionState.SERVICES_DISCOVERED
                                    )
                                )
                                continuation.resumeWith(Result.success(Unit))
                            }
                        }

                        override fun onDescriptorWrite(
                            gatt: BluetoothGatt,
                            descriptor: BluetoothGattDescriptor,
                            status: Int
                        ) {
                            Log.d("BLE", "onDescriptorWrite $status")
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                connectionStateChanges.tryEmit(
                                    ConnectionStateChange(
                                        deviceAddress = gatt.device.address,
                                        state = ConnectionState.NOTIFICATIONS_ENABLED
                                    )
                                )
                            }
                        }

                        override fun onCharacteristicChanged(
                            gatt: BluetoothGatt,
                            characteristic: BluetoothGattCharacteristic,
                            value: ByteArray
                        ) {
                            val deviceAddress = gatt.device.address

                            val emitted = characteristicNotifications.tryEmit(
                                CharacteristicNotification(
                                    deviceAddress = deviceAddress,
                                    characteristicUuid = characteristic.uuid.toKotlinUuid(),
                                    serviceUuid = characteristic.service.uuid.toKotlinUuid(),
                                    value = value
                                )
                            )
                            Log.d(
                                "BLE",
                                "Characteristic ${characteristic.uuid} changed: ${value.toHexString()} emitted: $emitted"
                            )
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onCharacteristicChanged(
                            gatt: BluetoothGatt?,
                            characteristic: BluetoothGattCharacteristic?
                        ) {
                            onCharacteristicChanged(
                                gatt ?: return,
                                characteristic ?: return,
                                characteristic.value ?: return
                            )
                        }

                        override fun onCharacteristicWrite(
                            gatt: BluetoothGatt,
                            characteristic: BluetoothGattCharacteristic,
                            status: Int
                        ) {
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                Log.d("BLE", "Write successful for ${characteristic.uuid}")
                            } else {
                                Log.e("BLE", "Write failed for ${characteristic.uuid}: $status")
                            }
                        }
                    }

                    val deviceAddress = device.address
                    if (connections.containsKey(deviceAddress)) {
                        continuation.resumeWith(Result.success(Unit))
                        return@suspendCancellableCoroutine
                    }

                    val gatt = device.getRawData().connectGatt(context, false, gattCallback)
                    if (gatt != null) {
                        connections[deviceAddress] = DeviceConnection(device, gatt)
                    } else {
                        connectionStateChanges.tryEmit(
                            ConnectionStateChange(
                                deviceAddress = device.address,
                                state = ConnectionState.CONNECTION_FAILED
                            )
                        )
                        continuation.resumeWith(Result.failure(Exception("Failed to connect to device")))
                    }
                }
            }
        } catch (e: Exception) {
            if(e is TimeoutCancellationException) disconnect(device)
            connectionStateChanges.tryEmit(
                ConnectionStateChange(
                    deviceAddress = device.address,
                    state = ConnectionState.CONNECTION_FAILED
                )
            )
            throw e
        }
    }

    override fun observeConnectionState(device: BtDevice): Flow<ConnectionState> {
        return connectionStateChanges
            .filter { it.deviceAddress == device.address }.map { it.state }
        }

    override suspend fun getDeviceServices(device: BtDevice): List<DeviceService> {
        connect(device)
        val connection = connections[device.address] ?: return emptyList()
        val gatt = connection.gatt ?: return emptyList()

        return gatt.services.map { service ->
            val characteristics = service.characteristics.map { characteristic ->
                val properties = mutableListOf<Property>()
                if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_READ != 0) {
                    properties.add(Property.READ)
                }
                if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) {
                    properties.add(Property.WRITE)
                }
                if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) {
                    properties.add(Property.NOTIFY)
                }
                if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) {
                    properties.add(Property.INDICATE)
                }
                DeviceCharacteristic(
                    uuid = characteristic.uuid.toKotlinUuid(),
                    serviceUuid = service.uuid.toKotlinUuid(),
                    properties = properties,
                )
            }
            DeviceService(service.uuid.toKotlinUuid(), service.instanceId, characteristics)
        }
    }

    override fun observeCharacteristicNotifications(device: BtDevice, characteristic: DeviceCharacteristic): Flow<ByteArray> {
        return observeCharacteristicNotifications(device,characteristic.uuid, characteristic.serviceUuid)
    }

    override fun observeCharacteristicNotifications(
        device: BtDevice,
        serviceUuid: Uuid,
        characteristic: Uuid
    ): Flow<ByteArray> {
        return characteristicNotifications
            .onStart { enableNotifications(device, serviceUuid, characteristic) }
            .retry(3)
            .onEach { Log.d("BLE", "characteristicNotifications = $it") }
            .filterNotNull()
            .buffer(capacity = 20)
            .filter { it.deviceAddress == device.address && it.serviceUuid == serviceUuid && it.characteristicUuid == characteristic }
            .map { it.value }
    }

    override fun findCharacteristicsInConnections(
        uuid: Uuid,
        serviceUuid: Uuid?,
        device: BtDevice?,
    ): List<DeviceCharacteristic> {
        val list = mutableListOf<DeviceCharacteristic>()
        //TODO: use device param
        for ((_, connection) in connections) {
            val gatt = connection.gatt ?: continue
            val services = serviceUuid?.let { gatt.services.filter{ it.uuid == serviceUuid.toJavaUuid()} } ?: gatt.services
            for (service in services) {
                val characteristic = service.getCharacteristic(uuid.toJavaUuid())
                if (characteristic != null) {
                    val properties = mutableListOf<Property>()
                    if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_READ != 0) {
                        properties.add(Property.READ)
                    }
                    if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) {
                        properties.add(Property.WRITE)
                    }
                    if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) {
                        properties.add(Property.NOTIFY)
                    }
                    if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) {
                        properties.add(Property.INDICATE)
                    }
                    val c = DeviceCharacteristic(
                        uuid = characteristic.uuid.toKotlinUuid(),
                        serviceUuid = service.uuid.toKotlinUuid(),
                        properties = properties
                    )
                    list.add(c)
                }
            }
        }
        return list
    }

    override fun findConnectedDeviceByAddress(address: String): BtDevice? {
        return connections[address]?.device
    }

    private fun enableNotifications(device: BtDevice, serviceUuid: Uuid, charUuid: Uuid) {

        val gatt = connections[device.address]?.gatt
            ?: throw IllegalStateException("Device not connected")
        val service = gatt.getService(serviceUuid.toJavaUuid())
            ?: throw IllegalStateException("Service not found")
        val characteristic = service.getCharacteristic(charUuid.toJavaUuid())
            ?: throw IllegalStateException("Characteristic not connected")

        // Check if characteristic supports notify
        if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) {
            gatt.setCharacteristicNotification(characteristic, true)

            val descriptor = characteristic.getDescriptor(
                UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
            )
            if (descriptor != null) {
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(descriptor)
                Log.d("BLE", "Notifications enabled for ${characteristic.uuid}")
            }

        } else {
            throw IllegalArgumentException("Could not enable notifications for $charUuid. No NOTIFY property")
        }

    }

    override suspend fun sendCommand(
        device: BtDevice,
        serviceUuid: Uuid,
        characteristic: Uuid,
        command: ByteArray
    ): Unit = withContext(Dispatchers.IO) {
        val connection = connections[device.address] ?: throw IllegalStateException("Device not connected")
        val gatt = connection.gatt ?: throw IllegalStateException("Device not connected")
        val service = gatt.getService(serviceUuid.toJavaUuid()) ?: throw IllegalArgumentException("Service not found")
        val charToWrite = service.getCharacteristic(characteristic.toJavaUuid()) ?: throw IllegalArgumentException("Characteristic not connected")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeCharacteristic(
                charToWrite,
                command,
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            )
        } else {
            charToWrite.value = command
            charToWrite.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            gatt.writeCharacteristic(charToWrite)
        }

        Log.d("BLE", "Command sent to ${characteristic}")
    }

    override fun disconnect(device: BtDevice) {
        val connection = connections[device.address] ?: return
        connection.gatt?.disconnect()
        connection.gatt?.close()
        connections.remove(device.address)
        Log.d("BLE", "Disconnected from ${device.name}")
    }

}