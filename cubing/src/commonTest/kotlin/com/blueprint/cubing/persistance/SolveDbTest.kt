package com.blueprint.cubing.persistance

import com.blueprint.cubing.replay.data.persistence.ReplayDB
import com.blueprint.cubing.replay.data.persistence.ReplayDatabaseConstructor
import kotlin.test.BeforeTest

class SolveDbTest {

    lateinit var db: ReplayDB

    @BeforeTest
    fun setUp() {
        db = ReplayDatabaseConstructor.initialize()
    }

    @kotlin.test.AfterTest
    fun tearDown() {
        db.close()
    }

}