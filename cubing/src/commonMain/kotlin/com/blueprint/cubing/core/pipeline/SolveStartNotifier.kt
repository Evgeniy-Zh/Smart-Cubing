package com.blueprint.cubing.core.pipeline

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

interface SolveStartEvents {
    class EventData(val firstMoveTimeStamp: Long?)
    val eventFlow: SharedFlow<EventData>
}

class SolveStartNotifier: SolveStartEvents {

    private val _eventFlow = MutableSharedFlow<SolveStartEvents.EventData>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val eventFlow = _eventFlow.asSharedFlow()

    fun notifySolveStart(firstMoveTimeStamp: Long?) {
        val emitted = _eventFlow.tryEmit(SolveStartEvents.EventData(firstMoveTimeStamp))
        require(emitted) { "Failed to emit solve start event" }
    }
}