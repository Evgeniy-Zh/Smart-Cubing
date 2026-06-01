package com.blueprint.cubing.replay

import com.blueprint.cubing.core.model.SolveSummary
import com.blueprint.cubing.replay.model.Replay
import kotlinx.coroutines.flow.Flow

interface ReplayHistoryRepository {

    sealed interface PagingParams{
        data class Latest(val count: Int): PagingParams
    }

    suspend fun getReplayList(pagingParams: PagingParams): List<Replay>

    fun observeReplayList(pagingParams: PagingParams): Flow<List<Replay>>

    suspend fun createNewReplayFromRawData(rawData: ByteArray)

    suspend fun deleteReplay(replayId: String)

    suspend fun deleteReplay(replay: Replay)

    suspend fun createNewReplay(
        solveSummary: SolveSummary,
        name: String? = null,
        note: String? = null
    )
}