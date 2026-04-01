package com.blueprint.bleapi.model

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
data class DeviceService (
    val uuid: Uuid,
    val id: Int,
    val characteristics: List<DeviceCharacteristic> = emptyList()
)