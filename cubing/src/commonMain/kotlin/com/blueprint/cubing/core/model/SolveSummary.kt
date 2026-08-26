package com.blueprint.cubing.core.model

import kotlinx.datetime.LocalDateTime

data class SolveSummary(
    val totalTime: Long,
    val date: LocalDateTime,
    val status: Status,
    val replayData: ReplayData? = null,
) {

    enum class Status {
        SOLVED, DNF, GAVE_UP,
    }

}

data class ReplayData(
    val kociembaInitState: String,
    val moves: List<ReplayData.Move>,
) {
    data class Move(
        val moveSequence: String,
        val elapsed: Long = 0,
    )
}