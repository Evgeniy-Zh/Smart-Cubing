package com.blueprint.cubing.core.model

sealed class CubeEvent {
    data class Move(
        val moveSequence: String,
        val elapsed: Long = 0,
        val cubeTimeStamp: Long = 0,
        val systemTimeStamp: Long = 0
    ) : CubeEvent()
    data class Solved(val solveSummary: SolveSummary? = null) : CubeEvent()
    data class CubeStateUpdated(
        val state: CubePermState,
        val kociembaState: String,
        val arbitraryFormattedStates: Map<String, String> = emptyMap(),
    ) : CubeEvent()
    data class RequestRequired(val request: CubeRequest): CubeEvent()
    class Error(val throwable: Throwable, val displayMessage: String? = null) : CubeEvent()
    data object Unsupported : CubeEvent()
}