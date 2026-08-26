package com.blueprint.cubing.persistance

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.blueprint.cubing.replay.data.persistence.MoveEntity
import com.blueprint.cubing.replay.data.persistence.Replay
import com.blueprint.cubing.replay.data.persistence.SolveDB
import com.blueprint.cubing.replay.data.persistence.SolveDao
import com.blueprint.cubing.replay.data.persistence.SolveEntity
import com.blueprint.cubing.replay.data.persistence.getInMemorySolveDatabaseBuilder
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SolveDbTest {

    lateinit var db: SolveDB

    lateinit var dao: SolveDao

    val solve = SolveEntity(
        id = "1",
        name = "Solve 1",
        note = "Regular Solve",
        dateTimestamp = 0,
        totalTime = 12000,
        status = "Solved",
        deviceId = null,
    )

    @BeforeTest
    fun setUp() {
        db = getInMemorySolveDatabaseBuilder()
            .setDriver(BundledSQLiteDriver())
            .build()

        dao = db.replayDao()
    }

    @Test
    fun `insert solve`() = runBlocking {

        dao.insertSolve(solve)

        val result = dao.getSolveById("1")

        println(result)

        val nullResult = dao.getSolveById("2")

        assertEquals(solve, result)

        assertEquals(null, nullResult)
    }

    @Test
    fun `insert solve with replay`() = runBlocking {

        val replayValues = Replay(
            initialState = "UUUURRRBBB",
            moves = List(20) { i-> MoveEntity(move = ('A' + i).toString(), order = i, timestamp = 1000L + i) }
        )
        dao.insertSolveWithReplay(solve, replayValues)

        val result = dao.getReplay(solve.id)

        assertNotNull(result)

        for(i in 0..<20) {
            assertEquals(replayValues.moves[i].move, result.moves[i].move)
        }

        assertEquals(replayValues.initialState, result.initialState)
    }

    @Test
    fun `delete solve`() = runBlocking {
        dao.insertSolve(solve)

        val result = dao.getSolveById("1")

        assertEquals(solve, result)

        dao.deleteSolveById("1")

        val nullResult = dao.getSolveById("1")

        assertEquals(null, nullResult)
    }

    @Test
    fun `delete solve with replay`() = runBlocking {
        val replayValues = Replay(
            initialState = "UUUURRRBBB",
            moves = List(20) { i-> MoveEntity(move = ('A' + i).toString(), order = i, timestamp = 1000L + i) }
        )
        dao.insertSolveWithReplay(solve, replayValues)

        val result = dao.getReplay(solve.id)

        assertNotNull(result)

        for(i in 0..<20) {
            assertEquals(replayValues.moves[i].move, result.moves[i].move)
        }

        assertEquals(replayValues.initialState, result.initialState)

        dao.deleteSolveById("1")

        val replayResult = dao.getReplay("1")
        assertEquals(null, replayResult)

        val movesResult = dao.getMovesBySolveId("1")

        assertEquals(emptyList(), movesResult)

        val initialStatesResult = dao.getInitialStates()

        assertEquals(emptyList(), initialStatesResult)
    }

    @Test
    fun `insert two solves with the same initialState`() = runBlocking {
        val replayValues = Replay(
            initialState = "UUUURRRBBB",
            moves = List(20) { i-> MoveEntity(move = ('A' + i).toString(), order = i, timestamp = 1000L + i) }
        )

        dao.insertSolveWithReplay(solve, replayValues)
        dao.insertSolveWithReplay(solve.copy(id = "2"), replayValues)

        val result = dao.getInitialStates()

        assertEquals(1, result.size)

        assertEquals(replayValues.initialState, result[0].state)

        val replay1 = dao.getReplay("1")!!
        val replay2 = dao.getReplay("2")!!

        assertEquals(replayValues.initialState, replay1.initialState)
        assertEquals(replayValues.initialState, replay2.initialState)
    }

    @Test
    fun `insert two solves with the same initialState and delete one`() = runBlocking {
        val replayValues = Replay(
            initialState = "UUUURRRBBB",
            moves = List(20) { i-> MoveEntity(move = ('A' + i).toString(), order = i, timestamp = 1000L + i) }
        )

        dao.insertSolveWithReplay(solve, replayValues)
        dao.insertSolveWithReplay(solve.copy(id = "2"), replayValues)

        var result = dao.getInitialStates()

        assertEquals(1, result.size)

        assertEquals(replayValues.initialState, result[0].state)

        dao.deleteSolveById("1")

        result = dao.getInitialStates()

        assertEquals(1, result.size)

        assertEquals(replayValues.initialState, result[0].state)

    }

    @AfterTest
    fun tearDown() {
        db.close()
    }

}