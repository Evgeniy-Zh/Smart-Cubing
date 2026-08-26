package com.blueprint.cubing.replay.data.persistence

import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

actual fun getSolveDatabaseBuilder(args: Any?): RoomDatabase.Builder<SolveDB> {
    val dbFile = File(System.getProperty("java.io.tmpdir"), "replay_room.db")
    return Room.databaseBuilder<SolveDB>(
        name = dbFile.absolutePath,
    )
}

actual fun getInMemorySolveDatabaseBuilder(): RoomDatabase.Builder<SolveDB> {
    return Room.inMemoryDatabaseBuilder()
}

