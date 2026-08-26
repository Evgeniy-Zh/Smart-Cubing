package com.blueprint.androidapp.di

import android.content.Context
import com.blueprint.androidapp.bluetooth.BlePeripheral
import com.blueprint.androidapp.bluetooth.ConnectionManager
import com.blueprint.androidapp.impl.AnimCubeViewSolverNode
import com.blueprint.androidapp.impl.BleScanner
import com.blueprint.androidapp.impl.CubeStateProviderImpl
import com.blueprint.androidapp.ui.cube.mapper.UiMapperNodeImpl
import com.blueprint.bleapi.IBleScanner
import com.blueprint.cubing.core.logic.CubeStateProvider
import com.blueprint.cubing.core.pipeline.CubeSolverNode
import com.blueprint.cubing.core.pipeline.UiMapperNode
import com.blueprint.cubing.cube.ui.CubeViewModel
import com.blueprint.cubing.device.debug.CharacteristicDetailViewModel
import com.blueprint.cubing.device.list.data.persistence.CubeDeviceDB
import com.blueprint.cubing.device.list.data.persistence.getCubeDatabase
import com.blueprint.cubing.device.search.DeviceDetailsViewModel
import com.blueprint.cubing.device.search.SearchDeviceViewModel
import com.blueprint.cubing.di.commonMainModule
import com.blueprint.cubing.replay.data.persistence.SolveDB
import com.blueprint.cubing.replay.data.persistence.getSolveDataBase
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.binds
import org.koin.dsl.module

val appModule = module {

    includes(commonMainModule)

    single { ConnectionManager(context = get()) }
    single { BlePeripheral(context = get()) }
    single<IBleScanner> { BleScanner(context = get()) }

    single<CubeSolverNode> { AnimCubeViewSolverNode() }
    single { UiMapperNodeImpl() } binds arrayOf(UiMapperNode::class)
    single <CubeStateProvider> { CubeStateProviderImpl() }

    single<CubeDeviceDB> { getCubeDatabase(args = get<Context>()) }
    single<SolveDB> { getSolveDataBase(args = get<Context>()) }

    viewModelOf(::CubeViewModel)
    viewModelOf(::SearchDeviceViewModel)
    viewModelOf(::DeviceDetailsViewModel)
    viewModelOf(::CharacteristicDetailViewModel)
}