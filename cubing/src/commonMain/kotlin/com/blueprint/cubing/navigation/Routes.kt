package com.blueprint.cubing.navigation

import kotlinx.serialization.Serializable


interface Route

@Serializable
data class DeviceDetailsRoute(
    val deviceAddress: String,
): Route

@Serializable
data class CharacteristicDetailsRoute(
    val deviceAddress: String,
    val serviceUuid: String,
    val characteristicUuid: String,
) : Route

@Serializable
data object SearchDevicesRoute: Route

