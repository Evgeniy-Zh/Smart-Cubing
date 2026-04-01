package com.blueprint.cubing.cube

import com.blueprint.cubing.core.model.CubeEvent
import com.blueprint.cubing.cube.timer.CubeTimer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SolveStateManager {

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

            CubeEvent.Solved -> onSolved()

            is CubeEvent.CubeStateUpdated -> {}

            is CubeEvent.RequestRequired -> {}

            CubeEvent.Unsupported -> {}
        }
    }

    fun proceed() {
       when(state.value) {
           is SolveState.Idle -> inspect()
           is SolveState.Scrambling -> inspect()
           is SolveState.Inspecting -> start()
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
        if(state.value is SolveState.Solved) return
        coroutineScope.launch {
            _state.emit(SolveState.Inspecting(15.toString()))
        }
    }

    private fun start() {
        timer.reset()
        timer.start(coroutineScope)
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
                start()
            }

            is SolveState.Idle, is SolveState.Solved -> {
                _state.emit(SolveState.Scrambling(event.moveSequence))
            }

            else -> {}
        }
    }

    private suspend fun onSolved() {
        if (state.value is SolveState.Solving) {
            timer.stop()
            _state.emit(SolveState.Solved(timer.getTotalTimeFormatted()))
        } else {
            _state.emit(SolveState.Idle)
        }
    }

}