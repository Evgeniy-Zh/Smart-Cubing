package com.blueprint.cubing.provider

import com.blueprint.cubing.core.endpoint.Endpoints
import com.blueprint.cubing.core.model.CubeDevice
import com.blueprint.cubing.core.model.CubeEndpoints
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class EndpointProvider {
    fun provideEndPoints(cubeDevice: CubeDevice): CubeEndpoints {
        // TODO: check cube model
        return CubeEndpoints(
            serviceUuid = Uuid.parse(Endpoints.GAN_GEN2_SERVICE),
            notificationUuid = Uuid.parse(Endpoints.GAN_GEN2_STATE_CHARACTERISTIC),
            commandUuid = Uuid.parse(Endpoints.GAN_GEN2_COMMAND_CHARACTERISTIC),
        )
    }
}