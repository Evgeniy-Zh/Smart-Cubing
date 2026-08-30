package com.blueprint.cubing.core.pipeline

import com.blueprint.cubing.core.model.CubeEvent
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

interface SolveEvents {
    sealed interface Event {
        data class SolveStart(val cubeKociembaState: String, val firstMove: CubeEvent.Move?) : Event
        data class InspectionStart(val cubeKociembaState: String) : Event
        data object GiveUp : Event
    }
    val eventFlow: SharedFlow<Event>
}

class SolveNotifier: SolveEvents {

    private val _eventFlow = MutableSharedFlow<SolveEvents.Event>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val eventFlow = _eventFlow.asSharedFlow()

    fun notify(event: SolveEvents.Event) {
        val emitted = _eventFlow.tryEmit(event)
        require(emitted) { "Failed to emit solve start event" }
    }
}