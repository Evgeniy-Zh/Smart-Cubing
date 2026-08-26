package com.blueprint.cubing.replay.data.mapper

import com.blueprint.cubing.core.model.SolveSummary
import com.blueprint.cubing.replay.data.persistence.SolveEntity
import com.blueprint.cubing.replay.model.SolvePreview
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
fun SolveEntity.toDomainModel(): SolvePreview = SolvePreview(
    id = id,
    name = name,
    note = note,
)

@OptIn(ExperimentalTime::class)
fun toEntity(solvePreview: SolvePreview, solveSummary: SolveSummary): SolveEntity {
    val millis = solveSummary.date.toInstant(timeZone = TimeZone.currentSystemDefault()).toEpochMilliseconds()

    return SolveEntity(
        id = solvePreview.id,
        name = solvePreview.name,
        note = solvePreview.note,
        dateTimestamp = millis,
        totalTime = solveSummary.totalTime,
        status = solveSummary.status.name,
        deviceId = null, //TODO: add device ID
    )
}


