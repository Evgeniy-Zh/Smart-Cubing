package com.blueprint.cubing.replay.data

import com.blueprint.cubing.core.format.TimeFormat
import com.blueprint.cubing.core.format.formatTime
import com.blueprint.cubing.core.model.SolveSummary
import com.blueprint.cubing.replay.SolveHistoryRepository
import com.blueprint.cubing.replay.data.mapper.toDomainModel
import com.blueprint.cubing.replay.data.mapper.toEntity
import com.blueprint.cubing.replay.data.persistence.MoveEntity
import com.blueprint.cubing.replay.data.persistence.Replay
import com.blueprint.cubing.replay.data.persistence.SolveDB
import com.blueprint.cubing.replay.model.SolvePreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
class SolveHistoryRepositoryImpl(
    private val solveDB: SolveDB,
    private val getSysTimeStamp: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : SolveHistoryRepository {

    override suspend fun getSolveList(pagingParams: SolveHistoryRepository.PagingParams): List<SolvePreview> {
        val count = when (pagingParams) {
            is SolveHistoryRepository.PagingParams.Latest -> pagingParams.count
        }
        return solveDB.replayDao().getLatestSolves(count)
            .map { it.toDomainModel() }
    }

    override fun observeSolveList(pagingParams: SolveHistoryRepository.PagingParams): Flow<List<SolvePreview>> {
        val count = when (pagingParams) {
            is SolveHistoryRepository.PagingParams.Latest -> pagingParams.count
        }
        return solveDB.replayDao().observeLatestSolves(count)
            .map { replays -> replays.map { it.toDomainModel() } }
    }

    override suspend fun createNewSolve(
        solveSummary: SolveSummary,
        name: String?,
        note: String?,
    ) = withContext(Dispatchers.Default) {
        val solveId = Uuid.random().toString()
        val replay = SolvePreview(
            id = solveId,
            name = name ?: solveSummary.totalTime.formatTime(TimeFormat.SOLVING),
            note = note ?: "",
        )

        val solve = toEntity(replay, solveSummary)
        if(solveSummary.replayData != null) {
            val replayValues = Replay(
                initialState = solveSummary.replayData.kociembaInitState,
                moves = solveSummary.replayData.moves.mapIndexed { index, move ->
                    MoveEntity(move = move.moveSequence, timestamp = move.elapsed, order = index)
                }
            )
            solveDB.replayDao().insertSolveWithReplay(
                solve = solve,
                replayValues = replayValues,
            )
        } else {
            solveDB.replayDao().insertSolve(solve)
        }

    }

    override suspend fun deleteSolve(solveId: String) {
        TODO("Not Implemented")
    }

    override suspend fun deleteSolve(solvePreview: SolvePreview) {
        deleteSolve(solvePreview.id)
    }

}