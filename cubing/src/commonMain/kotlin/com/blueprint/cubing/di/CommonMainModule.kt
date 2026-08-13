package com.blueprint.cubing.di

import com.blueprint.cubing.core.pipeline.SolveStartEvents
import com.blueprint.cubing.core.pipeline.SolveStartNotifier
import com.blueprint.cubing.core.pipeline.SolveSummaryNode
import com.blueprint.cubing.cube.CubeRepository
import com.blueprint.cubing.cube.CubeStateManager
import com.blueprint.cubing.cube.SolveSaver
import com.blueprint.cubing.cube.SolveSaverImpl
import com.blueprint.cubing.cube.SolveStateManager
import com.blueprint.cubing.cube.data.CubeRepositoryImpl
import com.blueprint.cubing.device.list.CubeListRepository
import com.blueprint.cubing.device.list.SupportedDevices
import com.blueprint.cubing.device.list.data.CubeListRepositoryImpl
import com.blueprint.cubing.device.list.data.persistence.CubeDeviceDB
import com.blueprint.cubing.device.list.data.persistence.getCubeDatabase
import com.blueprint.cubing.navigation.AppNavigator
import com.blueprint.cubing.navigation.AppNavigatorCommonImpl
import com.blueprint.cubing.navigation.NavigationEventHandler
import com.blueprint.cubing.provider.PipelineProvider
import com.blueprint.cubing.replay.ReplayHistoryRepository
import com.blueprint.cubing.replay.data.ReplayHistoryRepositoryImpl
import org.koin.dsl.binds
import org.koin.dsl.module

val commonMainModule = module {

    single { AppNavigatorCommonImpl() } binds arrayOf(AppNavigator::class, NavigationEventHandler::class)
    single<CubeRepository> { CubeRepositoryImpl(bleScanner = get(), pipeLineProvider = get()) }
//    single<CubeStateRepository> { FakeCubeStateRepository() }

    single { PipelineProvider(cubeSolverNode = get(), uiMapperNode = get(), solveSummaryNode = get()) }
    single { SolveStateManager(solveStartNotifier = get(), solveSaver = get()) }
    single<CubeListRepository> { CubeListRepositoryImpl(cubeDeviceDB = get()) }
    single { CubeStateManager(cubeRepository = get(), cubeListRepository = get()) }
    single { SupportedDevices() }
    single { SolveStartNotifier() } binds arrayOf(SolveStartNotifier::class, SolveStartEvents::class)
    single { SolveSummaryNode(solveStartEvents = get()) }

    //replay
    single<ReplayHistoryRepository> { ReplayHistoryRepositoryImpl(replayDB = get()) }
    single<SolveSaver> { SolveSaverImpl(replayHistoryRepository = get()) }

}