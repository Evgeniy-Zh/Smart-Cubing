package com.blueprint.cubing.core.pipeline.base

import kotlinx.coroutines.flow.Flow

interface PipelineNode<I, O> {
    suspend fun apply(inputFlow: Flow<I>): Flow<O>

    fun <P> then(next: PipelineNode<O, P>): PipelineNode<I, P> {
        val currentNode = this
        return object : PipelineNode<I, P> {
            override suspend fun apply(inputFlow: Flow<I>): Flow<P> {
                val intermediateFlow = currentNode.apply(inputFlow)
                return next.apply(intermediateFlow)
            }
        }
    }

}

open class EmptyNode<T> : PipelineNode<T, T> {
    override suspend fun apply(inputFlow: Flow<T>): Flow<T> = inputFlow
}

inline fun <reified T> emptyNode(): PipelineNode<T, T> {
    return EmptyNode<T>()
}

suspend fun <I, O> Flow<I>.applyPipeline(pipeline: PipelineNode<I, O>): Flow<O> {
    return pipeline.apply(this)
}
