package com.blueprint.cubing.replay.data.persistence

import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

actual fun getReplayDatabaseBuilder(args: Any?): RoomDatabase.Builder<ReplayDB> {
    val dbFile = File(System.getProperty("java.io.tmpdir"), "replay_room.db")
    return Room.databaseBuilder<ReplayDB>(
        name = dbFile.absolutePath,
    )
}

