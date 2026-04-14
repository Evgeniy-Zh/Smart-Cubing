package com.blueprint.cubing.provider

import com.blueprint.bleapi.model.BtDevice
import com.blueprint.cubing.core.model.CubeEvent
import com.blueprint.cubing.core.pipeline.CubeSolverNode
import com.blueprint.cubing.core.pipeline.Gen2DecryptionNode
import com.blueprint.cubing.core.pipeline.Gen2MessageParserNode
import com.blueprint.cubing.core.pipeline.SolveSummaryNode
import com.blueprint.cubing.core.pipeline.base.PipelineNode
import com.blueprint.cubing.core.pipeline.TimerNode
import com.blueprint.cubing.core.pipeline.UiMapperNode
import com.blueprint.cubing.core.pipeline.base.emptyNode

class PipelineProvider(
    private val cubeSolverNode: CubeSolverNode,
    private val uiMapperNode: UiMapperNode,
    private val solveSummaryNode: SolveSummaryNode,
) {

    fun createPipeline(device: BtDevice): PipelineNode<ByteArray, CubeEvent> {
        //TODO: Check cube model
        val node = emptyNode<ByteArray>()

        val decryptionNode = Gen2DecryptionNode(macAddress = device.address)
        val messageParserNode = Gen2MessageParserNode()
        val timerNode = TimerNode()

        return node
            .then(decryptionNode)
            .then(messageParserNode)
            .then(timerNode)
            .then(cubeSolverNode)
            .then(solveSummaryNode)
            .then(uiMapperNode)
    }

}