package com.blueprint.androidapp.ui.cube.mapper

import com.blueprint.cubing.core.model.CubeEvent
import com.blueprint.cubing.core.pipeline.UiMapperNode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

const val ANIM_CUBE_STATE = "AnimCube"

class UiMapperNodeImpl : UiMapperNode {
    override suspend fun apply(inputFlow: Flow<CubeEvent>): Flow<CubeEvent> {
        return inputFlow.map {
            if (it is CubeEvent.CubeStateUpdated) {
                it.copy(arbitraryFormattedStates = mapOf(ANIM_CUBE_STATE to it.kociembaState.mapToAnimCubeState()))
            } else {
                it
            }
        }
    }
}