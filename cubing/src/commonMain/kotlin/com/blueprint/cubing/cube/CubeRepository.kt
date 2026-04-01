package com.blueprint.cubing.cube

import com.blueprint.cubing.core.model.ConnectionState
import com.blueprint.cubing.core.model.CubeDevice
import com.blueprint.cubing.core.model.CubeEvent
import com.blueprint.cubing.core.model.CubeRequest
import com.blueprint.cubing.core.model.DeviceConnection
import kotlinx.coroutines.flow.Flow

interface CubeRepository {
    fun observeCubeEvents(): Flow<CubeEvent>
    fun observeConnectionEvents(): Flow<ConnectionState>
    suspend fun sendCubeRequest(request: CubeRequest)
    suspend fun connect(cubeDevice: CubeDevice): DeviceConnection?
    suspend fun disconnect()
}

