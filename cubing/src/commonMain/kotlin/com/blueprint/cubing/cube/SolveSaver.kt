package com.blueprint.cubing.cube

import com.blueprint.cubing.core.model.SolveSummary
import com.blueprint.cubing.replay.ReplayHistoryRepository

interface SolveSaver {

    suspend fun saveSuccessfulSolve(name: String, solve: SolveSummary)

}

class SolveSaverImpl(
    private val replayHistoryRepository: ReplayHistoryRepository
): SolveSaver {

    override suspend fun saveSuccessfulSolve(name: String, solve: SolveSummary) {
        replayHistoryRepository.createNewReplay(
            solveSummary = solve,
            name = null,
            note = null
        )
    }

}