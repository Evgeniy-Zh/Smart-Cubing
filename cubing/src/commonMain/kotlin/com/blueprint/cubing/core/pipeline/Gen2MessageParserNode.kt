package com.blueprint.cubing.core.pipeline

import com.blueprint.cubing.core.parse.Gen2MessageParser
import com.blueprint.cubing.core.model.CubeEvent
import com.blueprint.cubing.core.pipeline.base.PipelineNode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class Gen2MessageParserNode : PipelineNode<ByteArray, CubeEvent> {

    private val messageParser by lazy { Gen2MessageParser() }

    override suspend fun apply(inputFlow: Flow<ByteArray>): Flow<CubeEvent> {
        return flow {
            inputFlow.collect { bytes ->
                val event = messageParser.parseEvent(bytes)
                emit(event)
            }
        }

    }

}
