package com.blueprint.cubing.device.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blueprint.bleapi.IBleScanner
import com.blueprint.bleapi.model.BtDevice
import com.blueprint.cubing.navigation.AppNavigator
import com.blueprint.cubing.navigation.DeviceDetailsRoute
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class SearchDeviceViewModel(
    private val bleDeviceScanner: IBleScanner,
    private val appNavigator: AppNavigator,
) : ViewModel() {

    sealed interface ListItem {
        val key: String

        data class Device(
            val btDevice: BtDevice,
            override val key: String = btDevice.address
        ) : ListItem

        data class Details(
            val btDevice: BtDevice,
            override val key: String = btDevice.address + "_exp"
        ) : ListItem
    }

    private val _expanded = MutableStateFlow(persistentSetOf<BtDevice>())

    val devices: StateFlow<ImmutableList<ListItem>> = combine(
        flow = bleDeviceScanner.devices.flowOn(Dispatchers.IO),
        flow2 = _expanded,
    ) { list, expanded ->
        buildList {
            list.forEach {
                if (it.name != "None") add(ListItem.Device(it))
                if (expanded.contains(it)) add(ListItem.Details(it))
            }
        }.toPersistentList()
    }.flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = persistentListOf(),
        )

    fun openDeviceDetails(device: BtDevice) {
        appNavigator.navigateTo(DeviceDetailsRoute(device.address))
    }

    fun toggleDeviceProps(device: BtDevice) {
        _expanded.update {
            if (it.contains(device)) {
                it - device
            } else {
                it + device
            }.toPersistentSet()
        }
    }

}