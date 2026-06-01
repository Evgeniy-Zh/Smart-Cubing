package com.blueprint.cubing.core.model

import kotlinx.datetime.LocalDateTime

data class SolveSummary(
    val totalTime: Long,
    val date: LocalDateTime,
    val status: Status,
    val replayRawData: List<Byte>? = null,
    val kociembaInitState: String? = null
) {

    enum class Status {
        SOLVED, DNF, GAVE_UP,
    }

}