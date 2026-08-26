package com.blueprint.cubing.replay.data.persistence

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

fun getSolveDataBase(
    args: Any? = null
): SolveDB {
    val builder: RoomDatabase.Builder<SolveDB> = getSolveDatabaseBuilder(args)
    return builder
        .setDriver(BundledSQLiteDriver())
        .fallbackToDestructiveMigration(true)
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}

expect fun getSolveDatabaseBuilder(args: Any?): RoomDatabase.Builder<SolveDB>

expect fun getInMemorySolveDatabaseBuilder(): RoomDatabase.Builder<SolveDB>
