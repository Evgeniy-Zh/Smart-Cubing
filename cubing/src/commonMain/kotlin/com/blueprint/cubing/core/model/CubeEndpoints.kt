package com.blueprint.cubing.core.model

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
data class CubeEndpoints (
    val serviceUuid: Uuid,
    val notificationUuid: Uuid,
    val commandUuid: Uuid,
)