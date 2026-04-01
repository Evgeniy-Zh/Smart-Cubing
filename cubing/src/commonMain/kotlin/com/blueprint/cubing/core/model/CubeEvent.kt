package com.blueprint.cubing.core.model

sealed class CubeEvent {
    data class Move(val moveSequence: String, val timestamp: Long = 0) : CubeEvent()
    data object Solved : CubeEvent()
    data class CubeStateUpdated(
        val state: CubePermState,
        val kociembaState: String,
        val arbitraryFormattedStates: Map<String, String> = emptyMap(),
    ) : CubeEvent()
    data class RequestRequired(val request: CubeRequest): CubeEvent()
    data object Unsupported : CubeEvent()
}