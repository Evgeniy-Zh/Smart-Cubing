package com.blueprint.cubing.replay.data

import com.blueprint.cubing.core.format.TimeFormat
import com.blueprint.cubing.core.format.formatTime
import com.blueprint.cubing.core.model.SolveSummary
import com.blueprint.cubing.replay.ReplayHistoryRepository
import com.blueprint.cubing.replay.data.mapper.toDomainModel
import com.blueprint.cubing.replay.data.mapper.toEntity
import com.blueprint.cubing.replay.data.persistence.ReplayDB
import com.blueprint.cubing.replay.data.persistence.ReplayRawDataEntity
import com.blueprint.cubing.replay.model.Replay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
class ReplayHistoryRepositoryImpl(
    private val replayDB: ReplayDB,
    private val getSysTimeStamp: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : ReplayHistoryRepository {

    override suspend fun getReplayList(pagingParams: ReplayHistoryRepository.PagingParams): List<Replay> {
        val count = when (pagingParams) {
            is ReplayHistoryRepository.PagingParams.Latest -> pagingParams.count
        }
        return replayDB.replayDao().getLatestReplays(count)
            .map { it.toDomainModel() }
    }

    override fun observeReplayList(pagingParams: ReplayHistoryRepository.PagingParams): Flow<List<Replay>> {
        val count = when (pagingParams) {
            is ReplayHistoryRepository.PagingParams.Latest -> pagingParams.count
        }
        return replayDB.replayDao().observeLatestReplays(count)
            .map { replays -> replays.map { it.toDomainModel() } }
    }

    override suspend fun createNewReplayFromRawData(rawData: ByteArray) {
        val replayId = Uuid.random().toString()
        //TODO: parse raw data to get total time and status
        val replay = TODO()
        val summary = TODO()
        val rawData = TODO()

        replayDB.replayDao().insertReplay(toEntity(replay, summary))
        replayDB.replayRawDataDao().insertRawData(rawData)
    }

    override suspend fun createNewReplay(
        solveSummary: SolveSummary,
        name: String?,
        note: String?,
    ) = withContext(Dispatchers.Default) {
        val replayId = Uuid.random().toString()
        val replay = Replay(
            id = replayId,
            name = name ?: solveSummary.totalTime.formatTime(TimeFormat.SOLVING),
            note = note ?: "",
        )

        replayDB.replayDao().insertReplay(toEntity(replay, solveSummary))

        if (solveSummary.replayData != null && solveSummary.kociembaInitState != null) {
            val entity = ReplayRawDataEntity(
                replayId = replayId,
                kociembaInitState = solveSummary.kociembaInitState,
                rawData = ByteArray(0), //TODO: map moves
            )
            replayDB.replayRawDataDao().insertRawData(entity)
        }
    }

    override suspend fun deleteReplay(replayId: String) {
        replayDB.replayDao().deleteReplayById(replayId)
        replayDB.replayRawDataDao().deleteRawDataByReplayId(replayId)
    }

    override suspend fun deleteReplay(replay: Replay) {
        deleteReplay(replay.id)
    }

}