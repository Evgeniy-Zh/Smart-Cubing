package com.blueprint.cubing.device.list

import com.blueprint.cubing.core.model.CubeDevice
import kotlinx.coroutines.flow.StateFlow

interface CubeListRepository {

    val lastActiveDevice: CubeDevice?
    val activeDevice: CubeDevice?

    fun observeActiveDevice(): StateFlow<CubeDevice?>
    fun observeDevices(): StateFlow<List<CubeDevice>>
    suspend fun setAsActive(cube: CubeDevice?)
    suspend fun addDevice(cube: CubeDevice)
    suspend fun removeDevice(cube: CubeDevice)
}