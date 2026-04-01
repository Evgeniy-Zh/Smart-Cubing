package com.blueprint.androidapp.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import com.blueprint.bleapi.model.BtDevice
import com.blueprint.androidapp.bluetooth.Identifiers.COMMAND_CHARACTERISTIC_UUID
import com.blueprint.androidapp.bluetooth.Identifiers.CONNECTED_DEVICES_CHARACTERISTIC_UUID
import com.blueprint.androidapp.bluetooth.Identifiers.SERVICE_UUID
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel

@SuppressLint("MissingPermission")
class BlePeripheral(private val context: Context) {


    private val _commandChannel = Channel<Command>()
    val commandChannel: ReceiveChannel<Command> = _commandChannel

    private val adapter = BluetoothAdapter.getDefaultAdapter()
    private val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private var gattServer: BluetoothGattServer? = null
    private var connectedDevices: BluetoothGattCharacteristic? = null

    fun start() {
        gattServer = manager.openGattServer(context, gattServerCallback)

        val service = BluetoothGattService(
            SERVICE_UUID,
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        )

        val characteristic = BluetoothGattCharacteristic(
            COMMAND_CHARACTERISTIC_UUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE or
                    BluetoothGattCharacteristic.PROPERTY_READ or
                    BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_WRITE or
                    BluetoothGattCharacteristic.PERMISSION_READ
        )
        service.addCharacteristic(characteristic)

        connectedDevices = BluetoothGattCharacteristic(
            CONNECTED_DEVICES_CHARACTERISTIC_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ or
                    BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ
        )
        service.addCharacteristic(connectedDevices)

        gattServer?.addService(service)

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(true)
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .addServiceUuid(ParcelUuid(SERVICE_UUID))
            .build()

        adapter.bluetoothLeAdvertiser.startAdvertising(settings, data, advertiseCallback)
        Log.d("BLE", "Peripheral Started")
    }

    fun advertiseConnectedDevices(devices: List<BtDevice>) {
        connectedDevices?.value = devices.joinToString { it.name }.toByteArray()
    }

    private val advertiseCallback = object : AdvertiseCallback() {}

    private val gattServerCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(
            device: BluetoothDevice?,
            status: Int,
            newState: Int
        ) {
        }


        override fun onCharacteristicReadRequest(
            device: BluetoothDevice,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic
        ) {
            if(characteristic.uuid == CONNECTED_DEVICES_CHARACTERISTIC_UUID) {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic.value)
            }
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice?,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray
        ) {
            val command = value.decodeToString().parseCommand()
            Log.d("BLE", "Peripheral received: $command")
            _commandChannel.trySend(command)
            gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
        }
    }

    private fun String.parseCommand(): Command {
        return when {
            startsWith("connect") -> {
                val arr = split("|")
                val name = arr[1]
                val address = arr[2]
                Command.Connect(BtDevice(name, address))
            }
            startsWith("disconnect") -> {
                val arr = split("|")
                val name = arr[1]
                val address = arr[2]
                Command.Disconnect(BtDevice(name, address))
            }

            else ->{ throw IllegalArgumentException("Unknown command: $this") }
        }
    }
}

