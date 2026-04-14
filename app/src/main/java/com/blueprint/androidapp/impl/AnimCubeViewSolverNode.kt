package com.blueprint.androidapp.impl

import android.util.Log
import com.blueprint.androidapp.ui.cube.ext.isSolved
import com.blueprint.cubing.core.model.CubeEvent
import com.blueprint.cubing.core.pipeline.CubeSolverNode
import com.catalinjurjiu.animcubeandroid.AnimCube
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onCompletion

class AnimCubeViewSolverNode : CubeSolverNode {

    private val channel = MutableSharedFlow<Unit>()
    private var applied = false

    var cubeView: AnimCube? = null

    override suspend fun apply(inputFlow: Flow<CubeEvent>): Flow<CubeEvent> {
        applied = true
        return merge(
            inputFlow,
            channel.mapNotNull {
                val solved = cubeView?.isSolved()
                return@mapNotNull if (solved == true) {
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
        Log.d("AnimCubeViewSolverNode", "Animation ended, checking if cube is solved")
        channel.emit(Unit)
    }
}