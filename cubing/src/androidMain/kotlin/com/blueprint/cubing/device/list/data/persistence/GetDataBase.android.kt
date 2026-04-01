package com.blueprint.cubing.device.list.data.persistence

import androidx.room.Room
import androidx.room.RoomDatabase

actual fun getCubeDatabaseBuilder(args: Any?): RoomDatabase.Builder<CubeDeviceDB> {
    val context = args as android.content.Context
    val appContext = context.applicationContext
    val dbFile = appContext.getDatabasePath("cube_room.db")
    return Room.databaseBuilder<CubeDeviceDB>(
        context = appContext,
        name = dbFile.absolutePath
    )
}