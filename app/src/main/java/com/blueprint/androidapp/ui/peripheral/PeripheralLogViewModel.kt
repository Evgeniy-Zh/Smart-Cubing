package com.blueprint.androidapp.ui.peripheral

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import com.blueprint.androidapp.bluetooth.BlePeripheral
import com.blueprint.androidapp.bluetooth.ConnectionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

@SuppressLint("MissingPermission")
class PeripheralLogViewModel(
    private val peripheral: BlePeripheral,
    private val connectionManager: ConnectionManager,
) : ViewModel() {

    private val _log = MutableStateFlow<List<String>>(emptyList())
    val log: StateFlow<List<String>> = _log

    fun startService() {
        peripheral.start()
        logMessage("service started")
    }

    @SuppressLint("NewApi")
    fun advertiseConnectedDevices() {
        val connectedDevices = connectionManager.getConnectedDevices()
        peripheral.advertiseConnectedDevices(connectedDevices)
        logMessage("advertising ${connectedDevices.map { it.name }}")
    }

    private fun logMessage(m: String) {
        _log.update { it + m }
    }

}