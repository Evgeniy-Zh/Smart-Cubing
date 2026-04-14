package com.blueprint.desktopapp.impl

import androidx.compose.runtime.Immutable
import com.blueprint.cubing.core.model.CubeEvent
import com.blueprint.cubing.core.pipeline.CubeSolverNode
import com.blueprint.cubing.cube.ui.components.Cube2DState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onCompletion

@Immutable
internal class CubeSolverImpl: CubeSolverNode {

    var cubeState: Cube2DState? = null
    private val solvedStr = "UUUUUUUUURRRRRRRRRFFFFFFFFFDDDDDDDDDLLLLLLLLLBBBBBBBBB"

    private val channel = MutableSharedFlow<Unit>()
    private var applied = false

    override suspend fun apply(inputFlow: Flow<CubeEvent>): Flow<CubeEvent> {
        applied = true
        return merge(
            inputFlow,
            channel.mapNotNull {
                val solved = cubeState?.facelets == solvedStr
                return@mapNotNull if (solved) {
                    CubeEvent.Solved()
                } else {
                    null
                }
            },
        ).onCompletion {
            applied = false
        }
    }

    suspend fun onAnimationEnded() {
        if (!applied) return
        channel.emit(Unit)
    }
}