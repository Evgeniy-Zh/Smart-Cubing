package com.blueprint.cubing.device.list

import com.blueprint.bleapi.model.BtDevice
import com.blueprint.bleapi.model.DeviceService
import com.blueprint.cubing.core.endpoint.Endpoints.GAN_GEN2_SERVICE
import com.blueprint.cubing.core.endpoint.Endpoints.GAN_GEN3_SERVICE
import com.blueprint.cubing.core.endpoint.Endpoints.GAN_GEN4_SERVICE
import com.blueprint.cubing.core.model.BtNameIdentifier
import com.blueprint.cubing.core.model.CubeDevice
import com.blueprint.cubing.core.model.DeviceIdentifier
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
class SupportedDevices {

    private val supportedServiceUuids = listOf(
        GAN_GEN2_SERVICE,
        GAN_GEN3_SERVICE,
        GAN_GEN4_SERVICE,
    )
    suspend fun isSupported(
        btDevice: BtDevice,
        services: List<DeviceService> = emptyList()
    ): Boolean {
        return services.any {  supportedServiceUuids.contains(it.uuid.toString()) }
    }

    suspend fun getCubeDevice(
        btDevice: BtDevice,
        services: List<DeviceService> = emptyList()
    ): CubeDevice? {
        for (s in services) {
            when (s.uuid.toString()) {
                GAN_GEN2_SERVICE,
                GAN_GEN3_SERVICE,
                GAN_GEN4_SERVICE -> {
                    return CubeDevice(
                        name = btDevice.name,
                        model = "Cube",
                        identifier = BtNameIdentifier(btDevice.name)
                    )
                }
                else -> {}
            }
        }
        return null
    }

    suspend fun getDeviceIdentifier(btDevice: BtDevice): DeviceIdentifier? {
        TODO()
    }

}