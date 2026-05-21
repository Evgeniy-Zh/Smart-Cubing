package com.blueprint.cubing.replay

import com.blueprint.cubing.replay.model.Replay

interface ReplayHistoryRepository {

    sealed interface PagingParams{
        data class Latest(val count: Int): PagingParams
    }

    suspend fun getReplayList(pagingParams: PagingParams): List<Replay>

    suspend fun createNewReplay(rawData: ByteArray)

    suspend fun deleteReplay(replayId: String)

    suspend fun deleteReplay(replay: Replay)

}