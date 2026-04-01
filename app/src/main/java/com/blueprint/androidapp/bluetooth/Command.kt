package com.blueprint.androidapp.bluetooth

import com.blueprint.bleapi.model.BtDevice

sealed interface Command {
    val device: BtDevice
    data class Disconnect(override val device: BtDevice): Command
    data class Connect(override val device: BtDevice): Command
}