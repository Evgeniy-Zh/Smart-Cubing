package com.blueprint.cubing.replay.data.persistence

import androidx.room.Room
import androidx.room.RoomDatabase
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

actual fun getSolveDatabaseBuilder(args: Any?): RoomDatabase.Builder<SolveDB> {
    val fileManager = NSFileManager.defaultManager()
    val documentDirectory = NSSearchPathForDirectoriesInDomains(
        NSDocumentDirectory,
        NSUserDomainMask,
        true
    ).firstOrNull() as? String

    val dbPath = if (documentDirectory != null) {
        "$documentDirectory/replay_room.db"
    } else {
        "replay_room.db"
    }

    return Room.databaseBuilder<SolveDB>(
        name = dbPath,
    )
}

