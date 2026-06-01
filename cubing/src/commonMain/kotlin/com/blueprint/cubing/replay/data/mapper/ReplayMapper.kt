package com.blueprint.cubing.replay.data.mapper

import com.blueprint.cubing.core.model.SolveSummary
import com.blueprint.cubing.replay.data.persistence.ReplayEntity
import com.blueprint.cubing.replay.model.Replay
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
fun ReplayEntity.toDomainModel(): Replay = Replay(
    id = id,
    name = name,
    note = note,
)

fun Replay.toEntity(): ReplayEntity = ReplayEntity(
    id = id,
    name = name,
    note = note,
    dateTimestamp = solveSummary.date.nanosecond / 1000L,
    totalTime = solveSummary.totalTime,
    status = solveSummary.status.name,
)


