package com.blueprint.cubing.cube

import com.blueprint.cubing.core.model.CubeEvent
import com.blueprint.cubing.core.pipeline.SolveStartEvents
import com.blueprint.cubing.core.pipeline.SolveStartNotifier
import com.blueprint.cubing.cube.timer.CubeTimer
import com.blueprint.cubing.log.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SolveStateManager(
    private val solveStartNotifier: SolveStartNotifier,
) {

    interface WithTime {
        val time: String
    }

    sealed interface SolveState {
        data object Idle : SolveState
        data class Scrambling(val scrumbleSequence: String) : SolveState
        data class Inspecting(override val time: String) : SolveState, WithTime
        data class Solving(override val time: String) : SolveState, WithTime
        data class Solved(override val time: String) : SolveState, WithTime
    }

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    private val timer = CubeTimer()

    private val _state = MutableStateFlow<SolveState>(SolveState.Idle)
    val state = _state.asStateFlow()

    suspend fun onCubeEvent(event: CubeEvent) {
        when (event) {
            is CubeEvent.Move -> onMove(event)

            is CubeEvent.Solved -> onSolved(event)

            is CubeEvent.CubeStateUpdated -> {}

            is CubeEvent.RequestRequired -> {}

            is CubeEvent.Error -> {}

            CubeEvent.Unsupported -> {}
        }
    }

    fun proceed() {
        when (state.value) {
            is SolveState.Idle -> inspect()
            is SolveState.Scrambling -> inspect()
            is SolveState.Inspecting -> start(move = null)
            is SolveState.Solving -> giveUp()
            is SolveState.Solved -> idle()
        }
    }

    private fun idle() {
        coroutineScope.launch {
            timer.stop()
            _state.emit(SolveState.Idle)
        }
    }

    private fun inspect() {
        if (state.value is SolveState.Solved) return
        coroutineScope.launch {
            _state.emit(SolveState.Inspecting(15.toString()))
        }
    }

    private fun start(move: CubeEvent.Move?) {
        timer.reset()
        timer.start(coroutineScope)
        sendSolveStartedEvent(move)
        coroutineScope.launch {
            timer.currentTime.collectLatest {
                _state.emit(SolveState.Solving(it))
            }
        }
    }

    private fun giveUp() {
        coroutineScope.launch {
            timer.stop()
            _state.emit(SolveState.Idle)
        }
    }

    private suspend fun onMove(event: CubeEvent.Move) {
        when (state.value) {
            is SolveState.Inspecting -> {
                start(move = event)
            }

            is SolveState.Idle, is SolveState.Solved -> {
                _state.emit(SolveState.Scrambling(event.moveSequence))
            }

            else -> {}
        }
    }

    private suspend fun onSolved(event: CubeEvent.Solved) {
        if (state.value is SolveState.Solving) {
            timer.stop()

            val time = event.totalTime?.let { CubeTimer.formatTime(it) }
                ?: timer.getTotalTimeFormatted()
            Logger.log("SolveStateManager","Measured time: ${event.totalTime?.let { CubeTimer.formatTime(it) }}")
            Logger.log("SolveStateManager","Timer time: ${timer.getTotalTimeFormatted()}")
            _state.emit(SolveState.Solved(time = time))
        } else {
            _state.emit(SolveState.Idle)
        }
    }

    private fun sendSolveStartedEvent(move: CubeEvent.Move?) {
        val timeStamp = move?.systemTimeStamp
        solveStartNotifier.notifySolveStart(SolveStartEvents.EventData(firstMoveTimeStamp = timeStamp))
    }

}