package com.blueprint.cubing.replay.data.persistence

import androidx.room.Room
import androidx.room.RoomDatabase

actual fun getSolveDatabaseBuilder(args: Any?): RoomDatabase.Builder<SolveDB> {
    val context = args as android.content.Context
    val appContext = context.applicationContext
    val dbFile = appContext.getDatabasePath("replay_room.db")
    return Room.databaseBuilder<SolveDB>(
        context = appContext,
        name = dbFile.absolutePath
    )
}

actual fun getInMemorySolveDatabaseBuilder(): RoomDatabase.Builder<SolveDB> {
    TODO("Not yet implemented")
}