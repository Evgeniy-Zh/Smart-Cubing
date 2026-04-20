package com.blueprint.cubing.cube

import com.blueprint.cubing.core.model.ConnectionState
import com.blueprint.cubing.core.model.CubeDevice
import com.blueprint.cubing.core.model.CubeEvent
import com.blueprint.cubing.core.model.CubeRequest
import com.blueprint.cubing.core.model.DeviceConnection
import com.blueprint.cubing.device.list.CubeListRepository
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

class CubeStateManager(
    private val cubeRepository: CubeRepository,
    private val cubeListRepository: CubeListRepository,
) {

    private val currentConnection: MutableStateFlow<DeviceConnection?> = MutableStateFlow(null)

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    fun observeCubeEvents(): Flow<CubeEvent> = channelFlow resultFlow@{

        cubeListRepository.observeActiveDevice().collectLatest { cubeDevice ->

            disconnect()

            cubeDevice ?: return@collectLatest

            val connection = connect(cubeDevice)
            if (connection == null) {
                triggerReconnect()
                return@collectLatest
            }

            cubeRepository.observeCubeEvents()
                .onEach { if (it is CubeEvent.Error) triggerReconnect() }
                .onCompletion { disconnect() }
                .collect { event ->
                    this@resultFlow.send(event)
                }
        }

    }

    fun observeConnectionEvents(): Flow<ConnectionState> = channelFlow resultFlow@{
        currentConnection.collectLatest { connection ->
            connection ?: return@collectLatest

            cubeRepository.observeConnectionEvents()
                .collect {
                    when (it) {
                        is ConnectionState.Connected -> sync()

                        is ConnectionState.FailedToConnect -> {}

                        is ConnectionState.Disconnected -> {
                            if (cubeListRepository.activeDevice != null) triggerReconnect()
                        }


                        else -> {}
                    }
                    _connectionState.value = it
                    this@resultFlow.send(it)
                }
        }
    }

    suspend fun sync() {
        cubeRepository.sendCubeRequest(CubeRequest.Sync)
    }

    suspend fun reset() {
        cubeRepository.sendCubeRequest(CubeRequest.Reset)
    }

    private suspend fun triggerReconnect() = withContext(NonCancellable) {
        yield()
        val identifier = cubeListRepository.activeDevice ?: return@withContext
        cubeListRepository.setAsActive(null)
        delay(300)
        cubeListRepository.setAsActive(identifier)
        delay(300)
    }

    private suspend fun connect(cubeDevice: CubeDevice): DeviceConnection? {
        val connection = cubeRepository.connect(cubeDevice)
        currentConnection.value = connection
        return connection
    }

    private suspend fun disconnect() {
        currentConnection.value = null
        cubeRepository.disconnect()
        _connectionState.value = ConnectionState.Disconnected
    }

}