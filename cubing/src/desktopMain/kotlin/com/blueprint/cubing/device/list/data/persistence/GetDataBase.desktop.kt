package com.blueprint.cubing.device.list.data.persistence

import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

actual fun getCubeDatabaseBuilder(args: Any?): RoomDatabase.Builder<CubeDeviceDB> {
    val dbFile = File(System.getProperty("java.io.tmpdir"), "cube_room.db")
    return Room.databaseBuilder<CubeDeviceDB>(
        name = dbFile.absolutePath,
    )
}