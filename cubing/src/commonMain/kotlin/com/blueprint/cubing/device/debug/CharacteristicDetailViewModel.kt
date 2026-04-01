package com.blueprint.cubing.device.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blueprint.bleapi.IBleScanner
import com.blueprint.bleapi.model.DeviceCharacteristic
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalStdlibApi::class, ExperimentalUuidApi::class)
class CharacteristicDetailViewModel(
    private val deviceAddress: String,
    private val serviceUuid: String,
    private val characteristicUuid: String,
    private val bleScanner: IBleScanner
) : ViewModel() {

    data class State(
        val uuid: String = "",
        val properties: List<String> = emptyList(),
        val values: List<String> = emptyList(),
        val observing: Boolean = false,
    )

    sealed interface Action {
        object ToggleObserve : Action
        object ClearLog : Action
    }

    private val characteristic: DeviceCharacteristic by lazy {
        bleScanner.findCharacteristicsInConnections(
            uuid = Uuid.parse(characteristicUuid),
            serviceUuid = Uuid.parse(serviceUuid),
        ).firstOrNull() ?: TODO()
    }
    private val values = MutableStateFlow(emptyList<String>())
    private val observing = MutableStateFlow(false)
    private val characteristicData = MutableStateFlow(characteristic)
    private var observingJob: Job? = null

    val state: StateFlow<State> = combine(
        characteristicData,
        values,
        observing,
    ) { characteristic, values, observing ->
        State(
            uuid = characteristic.uuid.toString(),
            properties = characteristic.properties.map { it.name },
            values = values,
            observing = observing,
        )
    }.stateIn(viewModelScope, WhileSubscribed(), State())


    fun handleAction(action: Action) {
        when (action) {
            is Action.ToggleObserve -> toggleObserve()
            is Action.ClearLog -> { values.update { emptyList() } }
        }
    }

    private fun toggleObserve() {

        if (observing.value) {
            observing.value = false
            observingJob?.cancel()
            return
        }

        observing.value = true
        if (observingJob?.isActive == true) return

        observingJob = viewModelScope.launch {
            bleScanner.observeCharacteristicNotifications(
                bleScanner.findConnectedDeviceByAddress(deviceAddress)!!,
                characteristic.serviceUuid,
                characteristic.uuid,
            )
                .catch { TODO() }
                .collect { value ->
                    values.update { it + value.toUiString() }
                }
        }
    }

    private fun ByteArray.toUiString(): String {
        val hexFormat = HexFormat {
            upperCase = false
            bytes {
                byteSeparator = ":"
            }
        }
        return toHexString(format = hexFormat)
    }

}