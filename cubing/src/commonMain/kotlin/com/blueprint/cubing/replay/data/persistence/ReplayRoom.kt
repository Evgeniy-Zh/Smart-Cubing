package com.blueprint.cubing.replay.data.persistence

import androidx.room.ConstructedBy
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabaseConstructor
import kotlinx.coroutines.flow.Flow


@Entity
data class ReplayEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val note: String,
    val dateTimestamp: Long,
    val totalTime: Long,
    val status: String, // enum name stored as String
)

@Entity
data class ReplayRawDataEntity(
    @PrimaryKey
    val replayId: String,
    val kociembaInitState: String,
    val rawData: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ReplayRawDataEntity) return false

        if (replayId != other.replayId) return false
        if(kociembaInitState != other.kociembaInitState) return false
        if (!rawData.contentEquals(other.rawData)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = replayId.hashCode()
        result = 31 * result + rawData.contentHashCode()
        result = 31 * result + kociembaInitState.hashCode()
        return result
    }
}

@Dao
interface ReplayDao {

    @Query("SELECT * FROM ReplayEntity ORDER BY dateTimestamp DESC LIMIT :count")
    suspend fun getLatestReplays(count: Int): List<ReplayEntity>

    @Query("SELECT * FROM ReplayEntity ORDER BY dateTimestamp DESC LIMIT :count")
    fun observeLatestReplays(count: Int): Flow<List<ReplayEntity>>

    @Query("SELECT * FROM ReplayEntity WHERE id = :id")
    suspend fun getReplayById(id: String): ReplayEntity?

    @Insert
    suspend fun insertReplay(replay: ReplayEntity)

    @Delete
    suspend fun deleteReplay(replay: ReplayEntity)

    @Query("DELETE FROM ReplayEntity WHERE id = :id")
    suspend fun deleteReplayById(id: String)

}

@Dao
interface ReplayRawDataDao {

    @Query("SELECT * FROM ReplayRawDataEntity WHERE replayId = :replayId")
    suspend fun getRawData(replayId: String): ReplayRawDataEntity?

    @Query("SELECT * FROM ReplayRawDataEntity WHERE replayId = :replayId")
    fun observeRawData(replayId: String): Flow<ReplayRawDataEntity?>

    @Insert
    suspend fun insertRawData(data: ReplayRawDataEntity)

    @Delete
    suspend fun deleteRawData(data: ReplayRawDataEntity)

    @Query("DELETE FROM ReplayRawDataEntity WHERE replayId = :replayId")
    suspend fun deleteRawDataByReplayId(replayId: String)

}

@androidx.room.Database(
    entities = [ReplayEntity::class, ReplayRawDataEntity::class],
    version = 1,
    exportSchema = false
)
@ConstructedBy(ReplayDatabaseConstructor::class)
abstract class ReplayDB : androidx.room.RoomDatabase() {
    abstract fun replayDao(): ReplayDao
    abstract fun replayRawDataDao(): ReplayRawDataDao
}

// The Room compiler generates the `actual` implementations.
@Suppress("KotlinNoActualForExpect")
expect object ReplayDatabaseConstructor : RoomDatabaseConstructor<ReplayDB> {
    override fun initialize(): ReplayDB
}


