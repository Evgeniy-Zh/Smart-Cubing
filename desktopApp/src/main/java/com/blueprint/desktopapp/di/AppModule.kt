package com.blueprint.desktopapp.di

import com.blueprint.bleapi.IBleScanner
import com.blueprint.blewindows.BleScanner
import com.blueprint.cubing.core.model.CubeEvent
import com.blueprint.cubing.core.pipeline.CubeSolverNode
import com.blueprint.cubing.core.pipeline.UiMapperNode
import com.blueprint.cubing.cube.ui.CubeViewModel
import com.blueprint.cubing.device.debug.CharacteristicDetailViewModel
import com.blueprint.cubing.device.list.data.persistence.CubeDeviceDB
import com.blueprint.cubing.device.list.data.persistence.getCubeDatabase
import com.blueprint.cubing.device.search.DeviceDetailsViewModel
import com.blueprint.cubing.device.search.SearchDeviceViewModel
import com.blueprint.cubing.di.commonMainModule
import com.blueprint.cubing.navigation.AppNavigator
import com.blueprint.cubing.navigation.AppNavigatorCommonImpl
import com.blueprint.cubing.navigation.NavigationEventHandler
import com.blueprint.desktopapp.impl.CubeSolverImpl
import kotlinx.coroutines.flow.Flow
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.binds
import org.koin.dsl.module

val appModule = module {

    includes(commonMainModule)

    single { AppNavigatorCommonImpl() } binds arrayOf(AppNavigator::class, NavigationEventHandler::class)

    single<IBleScanner> { BleScanner() }

    single<CubeSolverImpl> { CubeSolverImpl() } binds arrayOf(CubeSolverNode::class)

    single<UiMapperNode> {
        object : UiMapperNode {
            override suspend fun apply(inputFlow: Flow<CubeEvent>): Flow<CubeEvent> {
                return inputFlow
            }
        }
    }

    single<CubeDeviceDB> { getCubeDatabase() }

    viewModelOf(::CubeViewModel)
    viewModelOf(::SearchDeviceViewModel)
    viewModelOf(::DeviceDetailsViewModel)
    viewModelOf(::CharacteristicDetailViewModel)
}
