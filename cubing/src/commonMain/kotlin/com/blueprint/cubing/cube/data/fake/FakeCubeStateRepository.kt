package com.blueprint.cubing.cube.data.fake

import com.blueprint.bleapi.model.BtDevice
import com.blueprint.cubing.cube.CubeRepository
import com.blueprint.cubing.core.model.ConnectionState
import com.blueprint.cubing.core.model.CubeDevice
import com.blueprint.cubing.core.model.CubeEvent
import com.blueprint.cubing.core.model.CubePermState
import com.blueprint.cubing.core.model.CubeRequest
import com.blueprint.cubing.core.model.DeviceConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

class FakeCubeStateRepository : CubeRepository {

    // Simple data class implementing CubePermState for testing/demo
    class Impl(
        override val CP: Array<Int>,
        override val CO: Array<Int>,
        override val EP: Array<Int>,
        override val EO: Array<Int>
    ) : CubePermState

    private val states = mapOf(
        "solved" to Impl(
            CP = Array(8) { it },
            CO = Array(8) { 0 },
            EP = Array(12) { it },
            EO = Array(12) { 0 }
        ),
        "afterUMove" to Impl (
            CP = arrayOf(3,0,1,2,4,5,6,7),
            CO = Array(8) { 0 },
            EP = arrayOf(3,0,1,2,4,5,6,7,8,9,10,11),
            EO = Array(12) { 0 }
        ),
        "afterU2Move" to Impl (
            CP = arrayOf(2,3,0,1,4,5,6,7),
            CO = Array(8) { 0 },
            EP = arrayOf(2,3,0,1,4,5,6,7,8,9,10,11),
            EO = Array(12) { 0 }
        ),
        "afterUPrimeMove" to Impl (
            CP = arrayOf(1,2,3,0,4,5,6,7),
            CO = Array(8) { 0 },
            EP = arrayOf(1,2,3,0,4,5,6,7,8,9,10,11),
            EO = Array(12) { 0 }
        )
    )
    private val  scope = CoroutineScope(Dispatchers.Main)
    private val eventFlow = MutableSharedFlow<CubeEvent>()

    override fun observeCubeEvents(): Flow<CubeEvent> {
        return eventFlow
    }

    override fun observeConnectionEvents(): Flow<ConnectionState> = flow {
        emit(ConnectionState.Disconnected)
        delay(400)
        emit(ConnectionState.Connecting)
        delay(400)
        emit(ConnectionState.Connected)
    }

    override suspend fun sendCubeRequest(
        request: CubeRequest
    ) {
        TODO("Not yet implemented")
    }

    var index = 0


    private suspend fun syncCubeState(device: BtDevice) {
        eventFlow.emit(
            CubeEvent.CubeStateUpdated(
                state = states["solved"]!!,
                kociembaState = "000000000111111111222222222333333333444444444555555555"
            )
        )
        scope.launch { emitTestMoves() }
    }

    private suspend fun resetCube(device: BtDevice) {
        eventFlow.emit(CubeEvent.Move("U"))
        index = 0
    }

    override suspend fun connect(cubeDevice: CubeDevice): DeviceConnection? {
        TODO("Not yet implemented")
    }

    override suspend fun disconnect() {
        TODO("Not yet implemented")
    }

    private suspend fun emitTestMoves() {
        val moves = listOf("R U R' U'", "R U2 R' U'", "R U R' U'", "R U' R' U'")
        var moveIndex = 0
        delay(5000)
        while (true) {
            moveIndex = (moveIndex + 1) % moves.size
            delay(5000)
            eventFlow.emit(CubeEvent.Move(moves[moveIndex]))
        }
    }

    val kState = CharArray(54) { i-> '0' }
    private suspend fun facePointer(): CubePermState {
        kState[index] = '3'
        val event = CubeEvent.CubeStateUpdated(
            state = states["solved"]!!,
            kociembaState = kState.concatToString()
        )
        eventFlow.emit(event)
        kState[index] = '0'
        index++
        return event.state
    }
}