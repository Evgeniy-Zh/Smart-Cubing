package com.blueprint.cubing.device.list.data

import com.blueprint.cubing.core.model.BtNameIdentifier
import com.blueprint.cubing.core.model.CubeDevice
import com.blueprint.cubing.device.list.CubeListRepository
import com.blueprint.cubing.device.list.data.persistence.CubeDeviceDB
import com.blueprint.cubing.device.list.data.persistence.CubeDeviceEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class CubeListRepositoryImpl(
    private val cubeDeviceDB: CubeDeviceDB,
) : CubeListRepository {


    private val cubeDevicesDao = cubeDeviceDB.cubeDeviceDao()
    private val activeDeviceDao = cubeDeviceDB.activeCubeDeviceDao()

    private val devices: StateFlow<List<CubeDevice>> = cubeDevicesDao.observeAll().map { entities ->
        entities.map { entity ->
           entity.toCubeDevice()
        }
    }.stateIn(cubeDeviceDB.getCoroutineScope(), SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _activeDevice: StateFlow<CubeDevice?> = activeDeviceDao.observeActive().map { entity ->
        entity?.toCubeDevice()
    }.stateIn(cubeDeviceDB.getCoroutineScope(), SharingStarted.WhileSubscribed(5_000), null)


    override val lastActiveDevice: CubeDevice? get() = TODO("Not yet implemented")

    override val activeDevice: CubeDevice?
        get() = _activeDevice.value

    override fun observeActiveDevice(): StateFlow<CubeDevice?> {
        return _activeDevice
    }

    override fun observeDevices(): StateFlow<List<CubeDevice>> {
        return devices
    }

    override suspend fun setAsActive(cube: CubeDevice?) {
        val id = cubeDevicesDao.findByName(cube?.name)?.firstOrNull()?.id
        if (id != null) {
            activeDeviceDao.setLastActive(id)
        }
        activeDeviceDao.setActive(id)
    }

    override suspend fun addDevice(cube: CubeDevice) {
        cubeDevicesDao.insert(
            CubeDeviceEntity(
                name = cube.name,
                model = cube.model,
                address = null,
            )
        )
    }

    override suspend fun removeDevice(cube: CubeDevice) {
        TODO("Not yet implemented")
    }

    private fun CubeDeviceEntity.toCubeDevice(): CubeDevice {
        return CubeDevice(
            name = this.name,
            model = this.model ?: "Unknown",
            identifier = BtNameIdentifier(this.name)
        )
    }
}