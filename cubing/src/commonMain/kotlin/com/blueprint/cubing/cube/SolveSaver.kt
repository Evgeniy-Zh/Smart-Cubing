package com.blueprint.cubing.cube

import com.blueprint.cubing.core.model.SolveSummary
import com.blueprint.cubing.replay.SolveHistoryRepository

interface SolveSaver {

    suspend fun saveSuccessfulSolve(name: String, solve: SolveSummary)

}

class SolveSaverImpl(
    private val solveHistoryRepository: SolveHistoryRepository
): SolveSaver {

    override suspend fun saveSuccessfulSolve(name: String, solve: SolveSummary) {
        solveHistoryRepository.createNewSolve(
            solveSummary = solve,
            name = null,
            note = null
        )
    }

}