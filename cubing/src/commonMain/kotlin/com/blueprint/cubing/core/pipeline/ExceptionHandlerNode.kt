package com.blueprint.cubing.core.pipeline

import com.blueprint.cubing.core.model.CubeEvent
import com.blueprint.cubing.core.pipeline.base.PipelineNode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

class ExceptionHandlerNode: PipelineNode<CubeEvent, CubeEvent> {

    override suspend fun apply(inputFlow: Flow<CubeEvent>): Flow<CubeEvent> {
        return inputFlow.catch { e ->
            e.printStackTrace()
            emit(CubeEvent.Error(throwable = e))
        }
    }

}