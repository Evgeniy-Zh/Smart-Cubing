package com.blueprint.cubing.core.model

import kotlinx.datetime.LocalDateTime

data class SolveSummary(
    val totalTime: Long,
    val date: LocalDateTime,
    val status: Status,
    val replayData: ReplayData? = null,
    val kociembaInitState: String? = null
) {

    enum class Status {
        SOLVED, DNF, GAVE_UP,
    }

}

data class ReplayData(
    val moves: List<ReplayData.Move>,
) {
    data class Move(
        val moveSequence: String,
        val elapsed: Long = 0,
    )
}