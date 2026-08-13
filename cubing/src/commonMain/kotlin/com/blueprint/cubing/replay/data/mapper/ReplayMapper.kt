package com.blueprint.cubing.replay.data.mapper

import com.blueprint.cubing.core.model.SolveSummary
import com.blueprint.cubing.replay.data.persistence.ReplayEntity
import com.blueprint.cubing.replay.model.Replay
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
fun ReplayEntity.toDomainModel(): Replay = Replay(
    id = id,
    name = name,
    note = note,
)

@OptIn(ExperimentalTime::class)
fun toEntity(replay: Replay, solveSummary: SolveSummary): ReplayEntity {
    val millis = solveSummary.date.toInstant(timeZone = TimeZone.currentSystemDefault()).toEpochMilliseconds()

    return ReplayEntity(
        id = replay.id,
        name = replay.name,
        note = replay.note,
        dateTimestamp = millis,
        totalTime = solveSummary.totalTime,
        status = solveSummary.status.name,
    )
}


