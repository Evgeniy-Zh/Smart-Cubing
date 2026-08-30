package com.blueprint.cubing.core.pipeline

import com.blueprint.cubing.core.model.CubeEvent
import com.blueprint.cubing.core.model.ReplayData
import com.blueprint.cubing.core.model.SolveSummary
import com.blueprint.cubing.core.pipeline.base.PipelineNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class SolveSummaryNode(
    private val getSysTimeStamp: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    private val getCurrentDateTime: () -> LocalDateTime = { Clock.System.now().toLocalDateTime(TimeZone.UTC) },
    private val solveEvents: SolveEvents,
) : PipelineNode<CubeEvent, CubeEvent> {

    private val scope = CoroutineScope(Dispatchers.Main.immediate)
    private var eventObserverJob: Job? = null

    private var systemStartTime: Long? = null
    private var cubeStartTime: Long? = null

    private var totalTime: Long = 0
    private val moves: MutableList<ReplayData.Move> = mutableListOf()
    private var initialState: String? = null

    override suspend fun apply(inputFlow: Flow<CubeEvent>): Flow<CubeEvent> {
        return flow {
            inputFlow.collect { event ->
                var updatedEvent = event

                if (event is CubeEvent.Move) {
                    onEvent(event)
                }
                if (event is CubeEvent.Solved) {
                    updatedEvent = addSummary(event)
                    reset()
                }

                emit(updatedEvent)
            }
        }.onStart {
            eventObserverJob = scope.launch {
                solveEvents.eventFlow.collect { event ->
                    when (event) {
                        is SolveEvents.Event.SolveStart -> onSolveStart(event)
                        is SolveEvents.Event.InspectionStart -> onInspectionStart(event.cubeKociembaState)
                        is SolveEvents.Event.GiveUp -> onGiveUp()
                    }
                }
            }
        }.onCompletion { eventObserverJob?.cancel() }
    }

    /**
     *  if a solve is triggered by a move, the method is called after the move is made.
     */
    private fun onSolveStart(event: SolveEvents.Event.SolveStart) {
        initialState = event.cubeKociembaState
        moves.clear()
        event.firstMove?.let {
            moves.add(ReplayData.Move(moveSequence = it.moveSequence, elapsed = 0))
        }
        val firstMoveTimeStamp = event.firstMove?.systemTimeStamp

        if (firstMoveTimeStamp != null) {
            cubeStartTime = firstMoveTimeStamp
        } else {
            systemStartTime = getSysTimeStamp()
        }
    }

    private fun onInspectionStart(state: String) {

    }

    private fun onGiveUp() {
        reset()
    }


    private fun onEvent(e: CubeEvent.Move) {
        if (cubeStartTime == null && systemStartTime == null) { // if no active solve, ignore the move event
            return
        }
        var event = e

        if (totalTime == 0L) {
            if (cubeStartTime == null) { // if not triggered by a move, calculate elapsed time using system time
                event = event.copy(elapsed =  event.systemTimeStamp - systemStartTime!!)
            }
        }
        moves.add(ReplayData.Move(moveSequence = event.moveSequence, elapsed = event.elapsed))
        totalTime += event.elapsed
    }

    private fun addSummary(event: CubeEvent.Solved): CubeEvent.Solved {
        val solveSummary = SolveSummary(
            totalTime = totalTime,
            date = getCurrentDateTime(),
            status = SolveSummary.Status.SOLVED,
            replayData = initialState?.let {
                ReplayData(
                    kociembaInitState = it,
                    moves = moves.toList()
                )
            },
        )

        return event.copy(solveSummary = solveSummary)
    }

    private fun reset() {
        systemStartTime = null
        cubeStartTime = null
        totalTime = 0
        initialState = null
        moves.clear()
    }

}