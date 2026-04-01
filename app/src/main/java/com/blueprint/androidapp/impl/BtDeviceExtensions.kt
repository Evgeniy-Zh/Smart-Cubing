package com.blueprint.androidapp.impl

import android.Manifest
import android.bluetooth.BluetoothDevice
import androidx.annotation.RequiresPermission
import com.blueprint.bleapi.model.BtDevice

@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
fun BtDevice(rawData: BluetoothDevice): BtDevice {
    return BtDevice(rawData.name ?: "None", rawData.address, rawData)
}

fun BtDevice.getRawData(): BluetoothDevice {
    return this.rawData!! as BluetoothDevice
}