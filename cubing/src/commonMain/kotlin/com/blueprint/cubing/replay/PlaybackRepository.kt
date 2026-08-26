package com.blueprint.cubing.replay

import com.blueprint.cubing.core.model.CubeEvent
import kotlinx.coroutines.flow.Flow

interface PlaybackRepository {

    fun replay(solveId: String): Flow<CubeEvent>

}