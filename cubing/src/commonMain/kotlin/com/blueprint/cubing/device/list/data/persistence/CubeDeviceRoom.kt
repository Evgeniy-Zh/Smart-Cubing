package com.blueprint.cubing.device.list.data.persistence

import androidx.room.ConstructedBy
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabaseConstructor
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow


@Entity
data class CubeDeviceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val model: String?,
    val address: String?,
)

@Dao
interface CubeDeviceDao {

    @Query("SELECT * FROM CubeDeviceEntity")
    fun observeAll(): Flow<List<CubeDeviceEntity>>

    @Query("SELECT * FROM CubeDeviceEntity WHERE name = :name")
    suspend fun findByName(name: String?): List<CubeDeviceEntity>?

    @Insert
    suspend fun insert(cube: CubeDeviceEntity)

    @Delete
    suspend fun delete(cube: CubeDeviceEntity)

}

@Entity
data class ActiveCubeDeviceEntity(
    @PrimaryKey
    val id: Long = 0,
    val lastActiveId: Long?,
    val activeId: Long?,
)

@Dao
interface ActiveCubeDeviceDao {

    @Query("SELECT CubeDeviceEntity.* FROM CubeDeviceEntity INNER JOIN ActiveCubeDeviceEntity ON CubeDeviceEntity.id = ActiveCubeDeviceEntity.activeId LIMIT 1")
    fun observeActive(): Flow<CubeDeviceEntity?>

    @Query("INSERT OR REPLACE INTO ActiveCubeDeviceEntity (id, activeId) VALUES (0, :id)")
    suspend fun setActive(id: Long?)

    @Query("INSERT OR REPLACE INTO ActiveCubeDeviceEntity (id, lastActiveId) VALUES (0, :id)")
    suspend fun setLastActive(id: Long)

    @Query("SELECT CubeDeviceEntity.* FROM CubeDeviceEntity INNER JOIN ActiveCubeDeviceEntity ON CubeDeviceEntity.id = ActiveCubeDeviceEntity.lastActiveId LIMIT 1")
    suspend fun getLastActive(): CubeDeviceEntity?

    @Query("DELETE FROM ActiveCubeDeviceEntity")
    suspend fun clearActive()

}

@androidx.room.Database(
    entities = [CubeDeviceEntity::class, ActiveCubeDeviceEntity::class],
    version = 1,
    exportSchema = false
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class CubeDeviceDB : androidx.room.RoomDatabase() {
    abstract fun cubeDeviceDao(): CubeDeviceDao
    abstract fun activeCubeDeviceDao(): ActiveCubeDeviceDao
}

// The Room compiler generates the `actual` implementations.
@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<CubeDeviceDB> {
    override fun initialize(): CubeDeviceDB
}