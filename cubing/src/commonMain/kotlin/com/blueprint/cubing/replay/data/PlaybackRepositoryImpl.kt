package com.blueprint.cubing.replay.data

import com.blueprint.cubing.core.model.CubeEvent
import com.blueprint.cubing.core.model.CubePermState
import com.blueprint.cubing.core.pipeline.UiMapperNode
import com.blueprint.cubing.core.pipeline.base.applyPipeline
import com.blueprint.cubing.replay.PlaybackRepository
import com.blueprint.cubing.replay.data.persistence.SolveDB
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList

class PlaybackRepositoryImpl(
    private val solveDB: SolveDB,
    private val uiMapperNode: UiMapperNode,
) : PlaybackRepository {

    private class Impl(
        override val CP: Array<Int>,
        override val CO: Array<Int>,
        override val EP: Array<Int>,
        override val EO: Array<Int>
    ) : CubePermState


    private fun solvedState(): Impl = Impl(
        CP = Array(8) { it },
        CO = Array(8) { 0 },
        EP = Array(12) { it },
        EO = Array(12) { 0 }
    )

    override fun replay(solveId: String): Flow<CubeEvent> {
        TODO()
    }

    override suspend fun getAllReplayEvents(solveId: String): List<CubeEvent> {
        val replay = solveDB.replayDao().getReplay(solveId)!!

        val list =  buildList {
            add(CubeEvent.CubeStateUpdated(
                state = solvedState(),
                kociembaState = replay.initialState,
            ))

            for(m in replay.moves){
                val move = CubeEvent.Move(m.move, m.timestamp)
                add(move)
            }
        }

        val result = list.asFlow()
            .applyPipeline(uiMapperNode)
            .toList()

        return result
    }
}