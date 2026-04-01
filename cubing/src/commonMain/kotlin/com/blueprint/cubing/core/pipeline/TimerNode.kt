package com.blueprint.cubing.core.pipeline

import com.blueprint.cubing.core.model.CubeEvent
import com.blueprint.cubing.core.pipeline.base.PipelineNode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class TimerNode : PipelineNode<CubeEvent, CubeEvent> {
    private var lastEventTimeStamp: Long = 0

    override suspend fun apply(inputFlow: Flow<CubeEvent>): Flow<CubeEvent> {
        //TODO: Use timestamps from cube events
        return flow {
            inputFlow.collect { event ->
                if (event is CubeEvent.Move) {
                    val timeBetweenMoves = Clock.System.now().toEpochMilliseconds() - lastEventTimeStamp
                    lastEventTimeStamp = Clock.System.now().toEpochMilliseconds()
                    emit(event.copy(timestamp = timeBetweenMoves))
                } else {
                    emit(event)
                }
            }
        }

    }
}