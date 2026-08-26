package com.blueprint.cubing.replay.data

import com.blueprint.cubing.core.model.CubeEvent
import com.blueprint.cubing.core.pipeline.Gen2MessageParserNode
import com.blueprint.cubing.core.pipeline.TimerNode
import com.blueprint.cubing.core.pipeline.base.applyPipeline
import com.blueprint.cubing.core.pipeline.base.emptyNode
import com.blueprint.cubing.replay.PlaybackRepository
import com.blueprint.cubing.replay.data.persistence.SolveDB
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class PlaybackRepositoryImpl(
    private val solveDB: SolveDB,
) : PlaybackRepository {

    override fun replay(solveId: String): Flow<CubeEvent> = flow {

        val rawDataFlow = flow<ByteArray> {
            val rawData =  TODO()
        }
        val node = emptyNode<ByteArray>()
        val messageParserNode = Gen2MessageParserNode()
        val timerNode = TimerNode()

        val pipeline = node
            .then(messageParserNode)
            .then(timerNode)

        rawDataFlow.applyPipeline(pipeline)
            .collect { event ->
                emit(event)
            }
    }

}