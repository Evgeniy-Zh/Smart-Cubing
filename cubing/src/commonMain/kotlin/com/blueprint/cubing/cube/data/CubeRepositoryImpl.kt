package com.blueprint.cubing.cube.data

import com.blueprint.bleapi.IBleScanner
import com.blueprint.bleapi.model.BtDevice
import com.blueprint.cubing.cube.CubeRepository
import com.blueprint.cubing.core.model.BtNameIdentifier
import com.blueprint.cubing.core.model.ConnectionState
import com.blueprint.cubing.core.model.CubeDevice
import com.blueprint.cubing.core.model.CubeEndpoints
import com.blueprint.cubing.core.model.CubeEvent
import com.blueprint.cubing.core.model.CubeRequest
import com.blueprint.cubing.core.model.DeviceConnection
import com.blueprint.cubing.core.pipeline.base.PipelineNode
import com.blueprint.cubing.core.serialize.base.RequestSerializer
import com.blueprint.cubing.core.pipeline.base.applyPipeline
import com.blueprint.cubing.provider.EndpointProvider
import com.blueprint.cubing.provider.PipelineProvider
import com.blueprint.cubing.provider.RequestSerializeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class CubeRepositoryImpl(
    private val bleScanner: IBleScanner,
    private val pipeLineProvider: PipelineProvider,
    private val requestSerializeProvider: RequestSerializeProvider = RequestSerializeProvider(),
    private val endpointProvider: EndpointProvider = EndpointProvider(),
) : CubeRepository {

    private var connectedDevice: BtDevice? = null
    private var pipeline: PipelineNode<ByteArray, CubeEvent>? = null
    private var serializer: RequestSerializer? = null
    private var endpoints: CubeEndpoints? = null

    override suspend fun connect(cubeDevice: CubeDevice): DeviceConnection? {
        return try {
            //TODO: search for device by address
            val deviceIdentifier = cubeDevice.identifier as BtNameIdentifier
            val device = bleScanner.devices.mapNotNull { list ->
                list.firstOrNull { it.name == deviceIdentifier.name }
            }.first()
            connectedDevice = device

            bleScanner.connect(device)

            setUpConnection(
                endpoints = endpointProvider.provideEndPoints(cubeDevice),
                requestSerializer = requestSerializeProvider.provideSerializer(cubeDevice, device),
                pipeline = pipeLineProvider.createPipeline(device),
            )

            DeviceConnection(uuid = Uuid.Companion.random(), identifier = cubeDevice.identifier)
        } catch (e: Exception) {
            // CONNECTION_FAILED event will be emitted
            e.printStackTrace()
            null
        }

    }

    override fun observeCubeEvents(): Flow<CubeEvent> = flow {
        val pipeline = pipeline ?: throw IllegalStateException("Pipeline not provided")
        val endpoints = endpoints ?: throw IllegalStateException("Endpoints not provided")
        val device = connectedDevice ?: throw IllegalStateException("No device was connected")

        bleScanner.observeCharacteristicNotifications(
            device = device,
            serviceUuid = endpoints.serviceUuid,
            characteristic = endpoints.notificationUuid,
        )
            .applyPipeline(pipeline)
            .collect { event ->
                emit(event)
            }

    }

    override fun observeConnectionEvents(): Flow<ConnectionState> {
        val device = connectedDevice ?: throw IllegalStateException("No device was connected")
        return bleScanner.observeConnectionState(device).map {
            when (it) {
                com.blueprint.bleapi.model.ConnectionState.NOTIFICATIONS_ENABLED -> ConnectionState.Connected
                com.blueprint.bleapi.model.ConnectionState.SERVICES_DISCOVERED -> ConnectionState.Initializing
                com.blueprint.bleapi.model.ConnectionState.CONNECTED -> ConnectionState.Initializing
                com.blueprint.bleapi.model.ConnectionState.CONNECTING -> ConnectionState.Connecting
                com.blueprint.bleapi.model.ConnectionState.DISCONNECTED -> ConnectionState.Disconnected
                com.blueprint.bleapi.model.ConnectionState.DISCONNECTING -> ConnectionState.Disconnecting
                com.blueprint.bleapi.model.ConnectionState.CONNECTION_FAILED -> ConnectionState.FailedToConnect
            }
        }
    }

    override suspend fun sendCubeRequest(request: CubeRequest) {
        val endpoints = endpoints ?: throw IllegalStateException("Endpoints not provided")
        val serializer = serializer ?: throw IllegalStateException("Serializer not provided")
        val device = connectedDevice ?: throw IllegalStateException("No device was connected")

        bleScanner.sendCommand(
            device = device,
            serviceUuid = endpoints.serviceUuid,
            characteristic = endpoints.commandUuid,
            command = serializer.serialize(request)
        )
    }


    override suspend fun disconnect() {
        connectedDevice?.let { bleScanner.disconnect(it) }
        this.connectedDevice = null
        this.endpoints = null
        this.serializer = null
        this.pipeline = null
    }

    private suspend fun setUpConnection(
        endpoints: CubeEndpoints,
        pipeline: PipelineNode<ByteArray, CubeEvent>,
        requestSerializer: RequestSerializer
    ) {
        this.endpoints = endpoints
        this.serializer = requestSerializer
        this.pipeline = pipeline
    }

}