package com.blueprint.cubing.replay

import com.blueprint.cubing.core.model.SolveSummary
import com.blueprint.cubing.replay.model.SolvePreview
import kotlinx.coroutines.flow.Flow

interface SolveHistoryRepository {

    sealed interface PagingParams {
        data class Latest(val count: Int): PagingParams
    }

    suspend fun getSolveList(pagingParams: PagingParams): List<SolvePreview>

    fun observeSolveList(pagingParams: PagingParams): Flow<List<SolvePreview>>

    suspend fun deleteSolve(solveId: String)

    suspend fun deleteSolve(solvePreview: SolvePreview)

    suspend fun createNewSolve(
        solveSummary: SolveSummary,
        name: String? = null,
        note: String? = null
    )
}