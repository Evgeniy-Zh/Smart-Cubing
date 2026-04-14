package com.blueprint.cubing.core.pipeline

import com.blueprint.cubing.core.model.CubeEvent
import com.blueprint.cubing.core.pipeline.base.PipelineNode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class TimerNode(
    private val getSysTimeStamp: () -> Long = { Clock.System.now().toEpochMilliseconds() }
) : PipelineNode<CubeEvent, CubeEvent> {

    private var cubeTimeStamp: Long = 0
    private var lastMoveTimeStamp: Long = 0

    override suspend fun apply(inputFlow: Flow<CubeEvent>): Flow<CubeEvent> {

        return flow {
            inputFlow.collect { event ->
                if (event is CubeEvent.Move) {
                    val now = getSysTimeStamp()

                    var elapsed = event.elapsed
                    if(elapsed == 0L) {
                        elapsed = now - lastMoveTimeStamp
                    }

                    lastMoveTimeStamp = now

                    cubeTimeStamp += elapsed

                    emit(event.copy(elapsed = elapsed, cubeTimeStamp = cubeTimeStamp, systemTimeStamp = now))
                } else {
                    emit(event)
                }
            }
        }

    }
}