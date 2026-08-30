package com.blueprint.cubing.cube.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blueprint.cubing.core.flow.shareSuspendingWhileNoSubs
import com.blueprint.cubing.core.model.ConnectionState
import com.blueprint.cubing.core.model.CubeDevice
import com.blueprint.cubing.core.model.CubeEvent
import com.blueprint.cubing.cube.CubeStateManager
import com.blueprint.cubing.cube.SolveStateManager
import com.blueprint.cubing.device.list.CubeListRepository
import com.blueprint.cubing.navigation.AppNavigator
import com.blueprint.cubing.navigation.ReplayRoute
import com.blueprint.cubing.navigation.SearchDevicesRoute
import com.blueprint.cubing.replay.SolveHistoryRepository
import com.blueprint.cubing.replay.model.SolvePreview
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CubeViewModel(
    private val cubeListRepository: CubeListRepository,
    private val solveHistoryRepository: SolveHistoryRepository,
    private val cubeStateManager: CubeStateManager,
    private val solveStateManager: SolveStateManager,
    private val appNavigator: AppNavigator,
) : ViewModel() {

    sealed interface Action {
        object Sync : Action
        object Reset : Action
        object Disconnect : Action
        object SolveAction : Action
        object SearchDevices: Action
        object OpenReplays: Action
        data class SelectDevice(val device: CubeDevice) : Action
    }

    data class State(
        val cubeDevices: List<CubeDevice> = emptyList(),
        val solveHistory: List<SolvePreview> = emptyList(),
        val activeDevice: CubeDevice? = null,
        val connectionState: ConnectionState,
        val solveState: SolveStateManager.SolveState,
    )

    val cubeEvents: SharedFlow<CubeEvent> = cubeStateManager.observeCubeEvents()
        .onEach { solveStateManager.onCubeEvent(it) }
        .shareSuspendingWhileNoSubs(
            scope = viewModelScope,
        )

    val connectionEvents = cubeStateManager.observeConnectionEvents().shareIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
    )

    val state = combine(
        cubeStateManager.connectionState,
        solveStateManager.state,
        cubeListRepository.observeActiveDevice(),
        cubeListRepository.observeDevices(),
        solveHistoryRepository.observeSolveList(SolveHistoryRepository.PagingParams.Latest(5))
    ) { connectionState, solveState, activeDevice, devices, solvePreviews ->
        State(
            cubeDevices = devices,
            activeDevice = activeDevice,
            connectionState = connectionState,
            solveState = solveState,
            solveHistory = solvePreviews,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = State(
            connectionState = ConnectionState.Disconnected,
            solveState = SolveStateManager.SolveState.Idle
        )
    )

    fun handleAction(action: Action) {
        when (action) {
            Action.Sync -> sync()
            Action.Reset -> reset()
            Action.Disconnect -> disconnect()
            Action.SolveAction -> solveAction()
            Action.SearchDevices -> searchDevices()
            Action.OpenReplays -> openReplays()
            is Action.SelectDevice -> selectDevice(action.device)
        }
    }

    private fun solveAction() {
        solveStateManager.proceed()
    }

    private fun sync() = viewModelScope.launch {
        cubeStateManager.sync()
    }

    private fun reset() = viewModelScope.launch {
        cubeStateManager.reset()
    }

    private fun searchDevices() {
        appNavigator.navigateTo(route = SearchDevicesRoute)
    }

    private fun selectDevice(device: CubeDevice) = viewModelScope.launch {
        cubeListRepository.setAsActive(device)
    }

    private fun disconnect() = viewModelScope.launch {
        cubeListRepository.setAsActive(null)
    }

    private fun openReplays() {
        appNavigator.navigateTo(route = ReplayRoute)
    }

}