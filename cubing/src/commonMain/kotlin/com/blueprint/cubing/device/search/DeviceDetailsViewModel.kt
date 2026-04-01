package com.blueprint.cubing.device.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blueprint.bleapi.IBleScanner
import com.blueprint.bleapi.model.BtDevice
import com.blueprint.bleapi.model.DeviceCharacteristic
import com.blueprint.bleapi.model.DeviceService
import com.blueprint.cubing.core.model.CubeDevice
import com.blueprint.cubing.device.list.CubeListRepository
import com.blueprint.cubing.device.list.SupportedDevices
import com.blueprint.cubing.navigation.AppNavigator
import com.blueprint.cubing.navigation.CharacteristicDetailsRoute
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
class DeviceDetailsViewModel(
    private val deviceAddress: String,
    private val bleDeviceScanner: IBleScanner,
    private val supportedDevices: SupportedDevices,
    private val cubeListRepository: CubeListRepository,
    private val appNavigator: AppNavigator,
) : ViewModel() {

    data class State(
        val loading: Boolean,
        val device: BtDevice? = null,
        val recognizedCubeDevice: CubeDevice? = null,
        val saved: Boolean = false,
        val list: ImmutableList<DeviceService> = persistentListOf(),
    )

    sealed interface Action {
        class OpenCharacteristicDetails(val characteristic: DeviceCharacteristic) : Action
        class SaveDevice : Action
        object Disconnect : Action
    }


    val state: StateFlow<State> = flow {
        val device = bleDeviceScanner.getDiscoveredDevices().first { it.address == deviceAddress }
        val services = bleDeviceScanner.getDeviceServices(device).toPersistentList()
        val cubeDevice = supportedDevices.getCubeDevice(device, services)
        val st = State(
            device = device,
            loading = false,
            list = services,
            saved = false,
            recognizedCubeDevice = cubeDevice,
        )

        emit(st)

        cubeListRepository.observeDevices()
            .map { list -> list.any { it.identifier == cubeDevice?.identifier } }
            .collect { saved ->
                emit(state.value.copy(saved = saved))
            }

    }.retry {
        delay(1000);
        true
    }.stateIn(viewModelScope, WhileSubscribed(), State(loading = true))

    fun handleAction(action: Action) {
        when (action) {
            is Action.OpenCharacteristicDetails -> openCharacteristicDetails(action.characteristic)
            is Action.SaveDevice -> save()
            Action.Disconnect -> disconnect()
        }
    }

    override fun onCleared() {
        disconnect()
    }

    private fun disconnect() {
        bleDeviceScanner.disconnect(state.value.device ?: return)
    }

    private fun save() = viewModelScope.launch {
        val cubeDevice = state.value.recognizedCubeDevice ?: return@launch
        cubeListRepository.addDevice(cubeDevice)
        cubeListRepository.setAsActive(cubeDevice)
    }

    private fun openCharacteristicDetails(characteristic: DeviceCharacteristic) {
        appNavigator.navigateTo(
            CharacteristicDetailsRoute(
                deviceAddress = deviceAddress,
                serviceUuid = characteristic.serviceUuid.toString(),
                characteristicUuid = characteristic.uuid.toString()
            )
        )
    }

}