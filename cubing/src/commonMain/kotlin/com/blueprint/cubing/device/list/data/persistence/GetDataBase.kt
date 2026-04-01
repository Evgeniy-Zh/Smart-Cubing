package com.blueprint.cubing.device.list.data.persistence

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

fun getCubeDatabase(
    args: Any? = null
): CubeDeviceDB {
    val builder: RoomDatabase.Builder<CubeDeviceDB> = getCubeDatabaseBuilder(args)
    return builder
        .setDriver(BundledSQLiteDriver())
        .fallbackToDestructiveMigration(true)
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}

expect fun getCubeDatabaseBuilder(args: Any?): RoomDatabase.Builder<CubeDeviceDB>