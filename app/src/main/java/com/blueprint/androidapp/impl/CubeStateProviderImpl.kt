package com.blueprint.androidapp.impl

import com.blueprint.cubing.core.logic.CubeStateProvider
import com.blueprint.cubing.cube.ui.components.Cube2DState

class CubeStateProviderImpl: CubeStateProvider {
    var cube2dState2d: Cube2DState? = null

    override fun getKociembaState(): String {
        return cube2dState2d?.facelets ?: throw IllegalStateException("Cube2DState is not set")
    }
}