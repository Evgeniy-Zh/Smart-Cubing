package com.blueprint.desktopapp.di

import com.blueprint.bleapi.IBleScanner
import com.blueprint.blewindows.BleScanner
import com.blueprint.cubing.core.model.CubeEvent
import com.blueprint.cubing.core.pipeline.CubeSolverNode
import com.blueprint.cubing.core.pipeline.SolveStartEvents
import com.blueprint.cubing.core.pipeline.SolveStartNotifier
import com.blueprint.cubing.core.pipeline.SolveSummaryNode
import com.blueprint.cubing.core.pipeline.UiMapperNode
import com.blueprint.cubing.cube.CubeRepository
import com.blueprint.cubing.cube.CubeStateManager
import com.blueprint.cubing.cube.SolveStateManager
import com.blueprint.cubing.cube.data.CubeRepositoryImpl
import com.blueprint.cubing.cube.ui.CubeViewModel
import com.blueprint.cubing.device.debug.CharacteristicDetailViewModel
import com.blueprint.cubing.device.list.CubeListRepository
import com.blueprint.cubing.device.list.SupportedDevices
import com.blueprint.cubing.device.list.data.CubeListRepositoryImpl
import com.blueprint.cubing.device.list.data.persistence.CubeDeviceDB
import com.blueprint.cubing.device.list.data.persistence.getCubeDatabase
import com.blueprint.cubing.device.search.DeviceDetailsViewModel
import com.blueprint.cubing.device.search.SearchDeviceViewModel
import com.blueprint.cubing.navigation.AppNavigator
import com.blueprint.cubing.navigation.AppNavigatorCommonImpl
import com.blueprint.cubing.navigation.NavigationEventHandler
import com.blueprint.cubing.provider.PipelineProvider
import com.blueprint.desktopapp.impl.CubeSolverImpl
import kotlinx.coroutines.flow.Flow
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.binds
import org.koin.dsl.module

val appModule = module {

    single { AppNavigatorCommonImpl() } binds arrayOf(AppNavigator::class, NavigationEventHandler::class)

    single<IBleScanner> { BleScanner() }
    single<CubeRepository> {
        CubeRepositoryImpl(
            bleScanner = get(),
            pipeLineProvider = get()
        )
    }
//    single<CubeStateRepository> { FakeCubeStateRepository() }

    single<CubeSolverImpl> { CubeSolverImpl() } binds arrayOf(CubeSolverNode::class)

    single<UiMapperNode> {
        object : UiMapperNode {
            override suspend fun apply(inputFlow: Flow<CubeEvent>): Flow<CubeEvent> {
                return inputFlow
            }
        }
    }
    single { PipelineProvider(cubeSolverNode = get(), uiMapperNode = get(), solveSummaryNode = get()) }
    single { SolveStateManager(solveStartNotifier = get()) }
    single<CubeDeviceDB> { getCubeDatabase() }
    single<CubeListRepository> { CubeListRepositoryImpl(cubeDeviceDB = get()) }
    single { CubeStateManager(repository = get(), deviceRepository = get()) }
    single { SupportedDevices() }
    single { SolveStartNotifier() } binds arrayOf(SolveStartNotifier::class, SolveStartEvents::class)
    single { SolveSummaryNode(solveStartEvents = get()) }

    viewModelOf(::CubeViewModel)
    viewModelOf(::SearchDeviceViewModel)
    viewModelOf(::DeviceDetailsViewModel)
    viewModelOf(::CharacteristicDetailViewModel)
}
