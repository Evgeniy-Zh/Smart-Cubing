package com.blueprint.blewindows

import com.blueprint.bleapi.IBleScanner
import com.blueprint.bleapi.model.BtDevice
import com.blueprint.bleapi.model.ConnectionState
import com.blueprint.bleapi.model.DeviceCharacteristic
import com.blueprint.bleapi.model.DeviceService
import com.blueprint.bleapi.model.Property
import com.blueprint.blewindows.SimpleBLE.libsimpleble
import com.blueprint.blewindows.SimpleBLE.libsimpleble.NotifyCallback
import com.sun.jna.Pointer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class BleScanner : IBleScanner {

    companion object {
        private var notifyCallback: NotifyCallback? = null
    }

    private data class ConnectionStateChange(
        val deviceAddress: String,
        val state: ConnectionState
    )

    private val simpleBLE: SimpleBLE = SimpleBLE()

    private val discoveredDevices: MutableMap<String, BtDevice> = mutableMapOf()
    private val connections: MutableList<BtDevice> = mutableListOf()

    private val connectionStateChanges =
        MutableSharedFlow<ConnectionStateChange>(extraBufferCapacity = 5)

    override val devices: Flow<List<BtDevice>> = channelFlow {

        val adapterHandle = simpleBLE.getAdapterHandle(0)

        withContext(Dispatchers.IO) {
            while (isActive) {
                simpleBLE.scanFor(adapterHandle, 1000)
                val scanResultsCount = simpleBLE.getScanResultsCount(adapterHandle)
                for (i in 0..<scanResultsCount) {
                    val peripheralHandle = simpleBLE.getPeripheralHandle(adapterHandle, i)
                    val peripheralIdentifier = simpleBLE.getPeripheralIdentifier(peripheralHandle)?.takeIf { it.isNotBlank() } ?: "None"
                    val peripheralAddress = simpleBLE.getPeripheralAddress(peripheralHandle)
                    val device = BtDevice(
                        name = peripheralIdentifier,
                        address = peripheralAddress,
                        rawData = peripheralHandle,
                    )
                    send(device)
                }
            }
        }
    }.scan(emptyList()) { devices, device ->
        if (!discoveredDevices.contains(device.address)) {
            discoveredDevices[device.address] = device
        }
        discoveredDevices.values.toList()
    }

    override fun getDiscoveredDevices(): List<BtDevice> {
        return discoveredDevices.values.toList()
    }

    override suspend fun connect(device: BtDevice) = withContext(Dispatchers.IO) {
        val peripheralHandle = device.rawData as Pointer
        val connectRes = simpleBLE.connectToPeripheral(peripheralHandle)
        if (connectRes) {
            connections.add(device)
        }
    }

    override fun observeConnectionState(device: BtDevice): Flow<ConnectionState> = channelFlow {
        //TODO: return the actual connection status
        println("observe connection")
        withContext(Dispatchers.IO) {
            delay(100)
            while (isActive) {
                if (!connections.contains(device)) {
                    send(ConnectionState.DISCONNECTED)
                } else {
                    send(ConnectionState.CONNECTING)
                    delay(300)
                    send(ConnectionState.CONNECTED)
                    delay(300)
                    send(ConnectionState.SERVICES_DISCOVERED)
                    delay(300)
                    send(ConnectionState.NOTIFICATIONS_ENABLED)
                    return@withContext
                }
                delay(1000)
            }
        }
        awaitClose { }
    }.distinctUntilChanged().onEach {
        println("emitting connection state: $it")
    }

    override suspend fun getDeviceServices(device: BtDevice): List<DeviceService> =
        withContext(Dispatchers.IO) {
            connect(device)
            val peripheralHandle = device.rawData as Pointer
            val count = simpleBLE.getPeripheralServicesCount(peripheralHandle).toInt()

            return@withContext buildList {
                for (i in 0..count) {
                    val s = simpleBLE.getPeripheralService(peripheralHandle, i) ?: continue
                    val serviceUuid = Uuid.parse(s.uuid.toString())
                    add(
                        DeviceService(
                            id = i,
                            uuid = serviceUuid,
                            characteristics = s.characteristics.mapNotNull { char ->
                                createCharacteristic(char, s)
                            }
                        )
                    )
                }
            }
        }

    override fun observeCharacteristicNotifications(
        device: BtDevice,
        characteristic: DeviceCharacteristic
    ): Flow<ByteArray> {
        return observeCharacteristicNotifications(
            device,
            characteristic.serviceUuid,
            characteristic.uuid
        )
    }

    override fun observeCharacteristicNotifications(
        device: BtDevice,
        serviceUuid: Uuid,
        characteristic: Uuid
    ): Flow<ByteArray> = callbackFlow {
        notifyCallback = object : NotifyCallback {
            override fun invoke(
                service: libsimpleble.uuid_t?,
                characteristic: libsimpleble.uuid_t?,
                data: Pointer,
                length: Long,
                userdata: Pointer?
            ) {
                val byteArray = data.getByteArray(0L, Math.toIntExact(length))
                println("len:" + length + ",data:" + byteArray.contentToString() + ",time:" + LocalDateTime.now())
                trySendBlocking(byteArray)
            }
        }

        while (!connections.contains(device) && isActive) {
            delay(1000)
        }
        val peripheralHandle = device.rawData as Pointer

        simpleBLE.subscribeToNotifications(
            peripheralHandle,
            serviceUuid.toString(),
            characteristic.toString(),
            notifyCallback,
            null
        )
        awaitClose {
            simpleBLE.unsubscribeFromCharacteristic(
                peripheralHandle,
                serviceUuid.toString(),
                characteristic.toString()
            )
        }
    }.onCompletion { println("observeCharacteristicNotifications complited") }


    override fun findCharacteristicsInConnections(
        uuid: Uuid,
        serviceUuid: Uuid?,
        device: BtDevice?
    ): List<DeviceCharacteristic> {
        val list = mutableListOf<DeviceCharacteristic>()
        val devices = if (device != null) listOf(device) else connections
        for (d in devices) {
            val peripheralHandle = d.rawData as Pointer
            val count = simpleBLE.getPeripheralServicesCount(peripheralHandle).toInt()
            for (i in 0..count) {
                val service = simpleBLE.getPeripheralService(peripheralHandle, i)
                if (serviceUuid != null && service?.uuid?.toString() != serviceUuid.toString()) continue

                service.characteristics.forEach { ch ->
                    if (ch.uuid.toString() != uuid.toString()) return@forEach

                    val characteristic = createCharacteristic(ch, service)
                    if (characteristic != null) {
                        list.add(characteristic)
                    }
                }
            }
        }
        return list
    }

    override suspend fun sendCommand(
        device: BtDevice,
        serviceUuid: Uuid,
        characteristic: Uuid,
        command: ByteArray
    ) {
        return withContext(Dispatchers.IO) {
            val peripheralHandle = device.rawData as Pointer
            println("sending command to $characteristic")
            val res = simpleBLE.writeCharacteristicCommand(
                peripheralHandle,
                serviceUuid.toString(),
                characteristic.toString(),
                command
            )
            println("sending result: $res")
        }
    }

    override fun findConnectedDeviceByAddress(address: String): BtDevice? {
        return connections.firstOrNull { it.address == address }
    }

    override fun disconnect(device: BtDevice) {
        simpleBLE.disconnectPeripheral(device.rawData as Pointer)
    }

    private fun createCharacteristic(
        ch: SimpleBLE.libsimpleble.characteristic_t,
        service: SimpleBLE.libsimpleble.service_t
    ): DeviceCharacteristic? {

        val uuid = Uuid.parse(ch.uuid.toString().takeIf { it.isNotBlank() } ?: return null)
        val serviceUuid = Uuid.parse(service.uuid.toString().takeIf { it.isNotBlank() } ?: return null)

        return DeviceCharacteristic(
            uuid = uuid,
            serviceUuid = serviceUuid,
            properties = buildList {
                //TODO: check property values
                if (ch.can_read > 0) add(Property.READ)
                if (ch.can_indicate > 0) add(Property.INDICATE)
                if (ch.can_notify > 0) add(Property.NOTIFY)
                if (ch.can_write_command > 0) add(Property.WRITE)
            }
        )

    }

}