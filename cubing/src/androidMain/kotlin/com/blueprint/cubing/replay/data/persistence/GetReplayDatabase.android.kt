package com.blueprint.cubing.replay.data.persistence

import androidx.room.Room
import androidx.room.RoomDatabase

actual fun getReplayDatabaseBuilder(args: Any?): RoomDatabase.Builder<ReplayDB> {
    val context = args as android.content.Context
    val appContext = context.applicationContext
    val dbFile = appContext.getDatabasePath("replay_room.db")
    return Room.databaseBuilder<ReplayDB>(
        context = appContext,
        name = dbFile.absolutePath
    )
}

