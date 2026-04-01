package com.blueprint.cubing.core.serialize.base

import com.blueprint.cubing.core.model.CubeRequest

interface RequestSerializer {
    fun serialize(cubeRequest: CubeRequest): ByteArray
}