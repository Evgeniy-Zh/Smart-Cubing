package com.blueprint.cubing.cube

import com.blueprint.cubing.core.model.ConnectionState
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
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

class CubeStateManager(
    private val repository: CubeRepository,
    private val deviceRepository: CubeListRepository,
) {

    private val currentConnection: MutableStateFlow<DeviceConnection?> = MutableStateFlow(null)

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    fun observeCubeEvents(): Flow<CubeEvent> = channelFlow {

        deviceRepository.observeActiveDevice().collectLatest { cubeDevice ->
            cubeDevice ?: return@collectLatest

            if (currentConnection.value != null) {
                disconnect()
            }

            val connection = repository.connect(cubeDevice)
            if(connection == null) {
                triggerReconnect()
                return@collectLatest
            }
            currentConnection.value = connection

            repository.observeCubeEvents()
                .onCompletion {
                    it?.printStackTrace()
                    disconnect()
                }
                .collect { event ->
                    send(event)
                }
        }
    }

    fun observeConnectionEvents(): Flow<ConnectionState> = channelFlow {
        currentConnection.collectLatest { connection ->
            connection ?: return@collectLatest

            repository.observeConnectionEvents()
                .collect {
                    when (it) {
                        is ConnectionState.Connected -> sync()
                        is ConnectionState.Disconnected,
                        ConnectionState.FailedToConnect -> {
                            if (deviceRepository.activeDevice != null) triggerReconnect()
                        }

                        else -> {}
                    }
                    _connectionState.value = it
                    send(it)
                }
        }
    }

    suspend fun sync() {
        repository.sendCubeRequest(CubeRequest.Sync)
    }

    suspend fun reset() {
        repository.sendCubeRequest(CubeRequest.Reset)
    }

    private suspend fun triggerReconnect() = withContext(NonCancellable) {
        yield()
        val identifier = deviceRepository.activeDevice ?: return@withContext
        currentConnection.value = null
        deviceRepository.setAsActive(null)
        delay(300)
        deviceRepository.setAsActive(identifier)
        delay(300)
    }

    private suspend fun disconnect() {
        currentConnection.value = null
        repository.disconnect()
        _connectionState.value = ConnectionState.Disconnected
    }

}