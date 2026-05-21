package com.blueprint.cubing.replay

import com.blueprint.cubing.core.model.CubeEvent
import kotlinx.coroutines.flow.Flow

interface PlaybackRepository {

    fun replay(replayId: String): Flow<CubeEvent>

}