package com.blueprint.desktopapp

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.blueprint.cubing.core.logic.CubeStateProvider
import com.blueprint.cubing.cube.ui.CubeViewModel
import com.blueprint.cubing.device.debug.CharacteristicDetailScreen
import com.blueprint.cubing.device.debug.CharacteristicDetailViewModel
import com.blueprint.cubing.device.search.DeviceDetailsScreen
import com.blueprint.cubing.device.search.DeviceDetailsViewModel
import com.blueprint.cubing.device.search.SearchDeviceScreen
import com.blueprint.cubing.device.search.SearchDeviceViewModel
import com.blueprint.cubing.navigation.CharacteristicDetailsRoute
import com.blueprint.cubing.navigation.DeviceDetailsRoute
import com.blueprint.cubing.navigation.NavAction
import com.blueprint.cubing.navigation.NavigationEventHandler
import com.blueprint.cubing.navigation.SearchDevicesRoute
import com.blueprint.desktopapp.di.appModule
import com.blueprint.desktopapp.impl.CubeSolverImpl
import com.blueprint.desktopapp.ui.MainScreen
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.context.startKoin
import org.koin.core.parameter.parametersOf


@Composable
fun App() {

    val navHandler: NavigationEventHandler = koinInject()
    val navController = rememberNavController()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        navHandler.handle(lifecycleOwner) { action ->

            when (action) {
                is NavAction.NavigateTo -> {
                    navController.navigate(action.route) {
                        action.popupTo?.let { popUpTo(it) { inclusive = true } }
                    }
                }

                is NavAction.NavigateBack -> {
                    val route = action.route
                    if(route == null) {
                        navController.popBackStack()
                    } else {
                        navController.popBackStack(
                            route = route,
                            inclusive = true,
                            saveState = false
                        )
                    }
                }
            }

        }
    }

    NavHost(
        modifier = Modifier.fillMaxSize(),
        navController = navController,
        startDestination = "Home"
    ) {
        composable(route = "Home") {

            val vm = koinViewModel<CubeViewModel>()
            val solver = koinInject<CubeSolverImpl>()
            val cubeStateProvider = koinInject<CubeStateProvider>()
            MainScreen(vm, solver, cubeStateProvider)
        }

        composable<SearchDevicesRoute>() {
            val viewModel = koinViewModel<SearchDeviceViewModel>()
            SearchDeviceScreen(
                viewModel = viewModel
            )
        }

        composable<DeviceDetailsRoute> {
            val route = it.toRoute<DeviceDetailsRoute>()
            val viewModel = koinViewModel<DeviceDetailsViewModel>(
                parameters = { parametersOf(route.deviceAddress) }
            )
            DeviceDetailsScreen(viewModel = viewModel)
        }

        composable<CharacteristicDetailsRoute> {
            val route = it.toRoute<CharacteristicDetailsRoute>()
            val viewModel = koinViewModel<CharacteristicDetailViewModel>(
                parameters = { parametersOf(route.deviceAddress, route.serviceUuid, route.characteristicUuid) }
            )
            CharacteristicDetailScreen(viewModel = viewModel)
        }
    }


}

fun main(): Unit = application {
    startKoin {
        modules(appModule)
    }
    Window(onCloseRequest = ::exitApplication, title = "Smart Cubing") {
        App()
    }
}

