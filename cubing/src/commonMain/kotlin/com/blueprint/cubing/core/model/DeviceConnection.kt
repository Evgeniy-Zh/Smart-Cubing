package com.blueprint.cubing.core.model

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
data class DeviceConnection constructor(
    val uuid: Uuid,
    val identifier: DeviceIdentifier
)