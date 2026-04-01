package com.blueprint.cubing.core.model

sealed class CubeRequest {

    data object Sync: CubeRequest()
    data object Reset: CubeRequest()
    data object Battery: CubeRequest()
    data object Hardware: CubeRequest()
    data class RawRequest(val bytes: List<Byte>): CubeRequest()

}