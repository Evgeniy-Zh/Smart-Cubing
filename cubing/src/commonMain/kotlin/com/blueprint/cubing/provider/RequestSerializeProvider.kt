package com.blueprint.cubing.provider

import com.blueprint.bleapi.model.BtDevice
import com.blueprint.cubing.core.model.CubeDevice
import com.blueprint.cubing.core.serialize.Gen2RequestSerializer
import com.blueprint.cubing.core.serialize.base.RequestSerializer

class RequestSerializeProvider {
    fun provideSerializer(cubeDevice: CubeDevice, btDevice: BtDevice): RequestSerializer {
        //TODO: Check cube model
        return Gen2RequestSerializer(btDevice)
    }
}