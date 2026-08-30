package com.blueprint.androidapp

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.blueprint.androidapp.permission.TryToRequestPermissions
import com.blueprint.cubing.navigation.AppNavigator
import com.blueprint.cubing.navigation.CharacteristicDetailsRoute
import com.blueprint.cubing.navigation.NavAction
import com.blueprint.cubing.navigation.NavigationEventHandler
import com.blueprint.cubing.navigation.SearchDevicesRoute
import com.blueprint.cubing.device.debug.CharacteristicDetailScreen
import com.blueprint.androidapp.ui.cube.CubeScreen
import com.blueprint.androidapp.ui.replay.ReplayScreen
import com.blueprint.cubing.device.search.SearchDeviceScreen
import com.blueprint.androidapp.ui.theme.CubingTheme
import com.blueprint.cubing.device.search.DeviceDetailsScreen
import com.blueprint.cubing.device.search.DeviceDetailsViewModel
import com.blueprint.cubing.navigation.DeviceDetailsRoute
import com.blueprint.cubing.navigation.ReplayRoute
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

class MainActivity : ComponentActivity() {
    private val navHandler: NavigationEventHandler by inject()


    @SuppressLint("MissingPermission", "UnusedMaterial3ScaffoldPaddingParameter")
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TryToRequestPermissions (onResult = {})
            CubingTheme {
                val navController = rememberNavController()
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                ) { paddingValues ->

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
                            CubeScreen()
                        }

                        composable<SearchDevicesRoute>() {
                            SearchDeviceScreen(viewModel = koinViewModel())
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
                            CharacteristicDetailScreen(
                                onBack = ::back,
                                viewModel = koinViewModel(parameters = {
                                    route.run {
                                        parametersOf(
                                            deviceAddress,
                                            serviceUuid,
                                            characteristicUuid
                                        )
                                    }
                                })
                            )
                        }

                        composable<ReplayRoute> {
                            ReplayScreen()
                        }
                    }


                }
            }

        }
    }

    private fun back() {
        val nav = navHandler as AppNavigator
        nav.back()
    }
}




