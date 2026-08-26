package com.blueprint.cubing.replay.data.persistence

import androidx.room.ConstructedBy
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabaseConstructor
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow


@Entity
data class SolveEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val note: String,
    val dateTimestamp: Long,
    val totalTime: Long,
    val status: String, // enum name stored as String
    val deviceId: Long?,
)

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = SolveEntity::class,
            parentColumns = ["id"],
            childColumns = ["solveId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["solveId"], unique = true)]
)
data class ReplayEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val solveId: String,
    val initialStateId: Long,
)

@Entity (
    foreignKeys = [
        ForeignKey(
            entity = ReplayEntity::class,
            parentColumns = ["id"],
            childColumns = ["replayId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MoveEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val replayId: Long = -1,
    val move: String,
    val timestamp: Long,
    val order: Int,
)

@Entity(
    indices = [Index(value = ["state"], unique = true)]
)
data class InitialStateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val state: String,
)

data class Replay(
    val moves: List<MoveEntity>,
    val initialState: String,
)

@Dao
abstract class SolveDao {

    @Query("SELECT * FROM SolveEntity ORDER BY dateTimestamp DESC LIMIT :count")
    abstract suspend fun getLatestSolves(count: Int): List<SolveEntity>

    @Query("SELECT * FROM SolveEntity ORDER BY dateTimestamp DESC LIMIT :count")
    abstract fun observeLatestSolves(count: Int): Flow<List<SolveEntity>>

    @Query("SELECT * FROM SolveEntity WHERE id = :id")
    abstract suspend fun getSolveById(id: String): SolveEntity?

    @Transaction
    open suspend fun getReplay(solveId: String): Replay? {
        val replayEntity = getReplayInternalBySolveId(solveId) ?: return null
        val initialState = getInitialStateById(replayEntity.initialStateId) ?: return null
        val moves = getMovesByReplayId(replayEntity.id)
        return Replay(
            initialState = initialState.state,
            moves = moves,
        )
    }

    @Insert
    abstract suspend fun insertSolve(replay: SolveEntity)

    @Transaction
    open suspend fun insertSolveWithReplay(solve: SolveEntity, replayValues: Replay?) {
        insertSolve(solve)
        if (replayValues != null) {
            insertReplay(solve.id, replayValues)
        }
    }

    @Transaction
    open suspend fun insertReplay(solveId: String, replayValues: Replay) {
        val initialState = InitialStateEntity(state = replayValues.initialState)

        var initialStateId =
            insertInitialState(initialState)

        if(initialStateId == -1L) {
            initialStateId = getInitialStateId(initialState.state)!!
        }

        val replay = ReplayEntity(
            solveId = solveId,
            initialStateId = initialStateId
        )

        val replayId =
            insertReplay(replay)

        val moves = replayValues.moves.map { move ->
            move.copy(replayId = replayId)
        }

        insertMoves(moves)
    }

    @Transaction
    open suspend fun deleteSolveById(solveId: String) {
        val solve = getSolveById(solveId) ?: return
        val replay =  getReplayInternalBySolveId(solveId)
        deleteSolveInternal(solve) // replay should be deleted by foreign key policy
        if (replay != null) {
            deleteInitialStateIfNotUsed(replay.initialStateId)
        }
    }

    @Transaction
    open suspend fun getMovesBySolveId(solveId: String): List<MoveEntity> {
        val replay = getReplayInternalBySolveId(solveId) ?: return emptyList()
        return getMovesByReplayId(replay.id)
    }

    @Query("SELECT * FROM InitialStateEntity")
    abstract suspend fun getInitialStates(): List<InitialStateEntity>

    @Query("SELECT * FROM MoveEntity WHERE replayId = :replayId ORDER BY `order`")
    protected abstract suspend fun getMovesByReplayId(replayId: Long): List<MoveEntity>

    @Transaction
    protected open suspend fun deleteReplay(replay: ReplayEntity) {
        deleteReplayInternal(replay)
        deleteInitialStateIfNotUsed(replay.initialStateId)
    }

    @Transaction
    protected open suspend fun deleteReplayById(id: Long) {
        val replay = getReplayInternalById(id)
        if (replay != null) {
            deleteReplay(replay)
        }
    }

    @Delete(SolveEntity::class)
    protected abstract suspend fun deleteSolveInternal(solve: SolveEntity)

    @Query("SELECT * FROM ReplayEntity WHERE id = :id")
    protected abstract suspend fun getReplayInternalById(id: Long): ReplayEntity?

    @Query("SELECT * FROM ReplayEntity WHERE solveId = :solveId")
    protected abstract suspend fun getReplayInternalBySolveId(solveId: String): ReplayEntity?

    @Query("SELECT * FROM InitialStateEntity WHERE id = :id")
    protected abstract suspend fun getInitialStateById(id: Long): InitialStateEntity?

    @Insert
    protected abstract suspend fun insertReplay(replay: ReplayEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun insertMoves(moves: List<MoveEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertInitialState(initialState: InitialStateEntity): Long

    @Query("SELECT id FROM InitialStateEntity WHERE state = :state")
    protected abstract suspend fun getInitialStateId(state: String): Long?

    @Delete
    protected abstract suspend fun deleteReplayInternal(replay: ReplayEntity)

    @Query("DELETE FROM ReplayEntity WHERE id = :id")
    protected abstract suspend fun deleteReplayByIdInternal(id: Long)

    @Query("DELETE FROM InitialStateEntity" +
            " WHERE id = :initialStateId AND NOT EXISTS (SELECT 1 FROM ReplayEntity WHERE initialStateId = :initialStateId)"
    )
    protected abstract suspend fun deleteInitialStateIfNotUsed(initialStateId: Long)

}

@androidx.room.Database(
    entities = [SolveEntity::class, ReplayEntity::class, MoveEntity::class, InitialStateEntity::class],
    version = 2,
    exportSchema = false
)
@ConstructedBy(ReplayDatabaseConstructor::class)
abstract class SolveDB : androidx.room.RoomDatabase() {
    abstract fun replayDao(): SolveDao
}

// The Room compiler generates the `actual` implementations.
@Suppress("KotlinNoActualForExpect")
expect object ReplayDatabaseConstructor : RoomDatabaseConstructor<SolveDB> {
    override fun initialize(): SolveDB
}


