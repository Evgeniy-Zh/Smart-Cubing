package com.blueprint.cubing.replay.data.persistence

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

fun getReplayDatabase(
    args: Any? = null
): ReplayDB {
    val builder: RoomDatabase.Builder<ReplayDB> = getReplayDatabaseBuilder(args)
    return builder
        .setDriver(BundledSQLiteDriver())
        .fallbackToDestructiveMigration(true)
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}

expect fun getReplayDatabaseBuilder(args: Any?): RoomDatabase.Builder<ReplayDB>

