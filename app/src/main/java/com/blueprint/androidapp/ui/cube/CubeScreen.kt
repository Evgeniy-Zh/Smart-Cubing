package com.blueprint.androidapp.ui.cube

import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import com.blueprint.androidapp.R
import com.blueprint.androidapp.ui.cube.ext.animateSequenceAsync
import com.blueprint.androidapp.ui.cube.ext.disconnectedCubeState
import com.blueprint.cubing.core.model.ConnectionState
import com.blueprint.cubing.core.model.CubeEvent
import com.blueprint.androidapp.impl.AnimCubeViewSolverNode
import com.blueprint.cubing.navigation.AppNavigator
import com.blueprint.cubing.navigation.SearchDevicesRoute
import com.blueprint.cubing.cube.ui.components.Cube2D
import com.blueprint.cubing.cube.ui.components.CubeOverlay
import com.blueprint.cubing.cube.ui.components.rememberCube2DState
import com.blueprint.androidapp.ui.cube.mapper.ANIM_CUBE_STATE
import com.blueprint.cubing.cube.ui.CubeViewModel
import com.blueprint.cubing.core.pipeline.CubeSolverNode
import com.catalinjurjiu.animcubeandroid.AnimCube
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CubeScreen(
    modifier: Modifier = Modifier,
    animCubeViewSolverNode: CubeSolverNode = koinInject(),
    cubeViewModel: CubeViewModel = koinViewModel(),
    appNavigator: AppNavigator = koinInject(),
) {
    animCubeViewSolverNode as AnimCubeViewSolverNode
    val activity = LocalActivity.current
    DisposableEffect(Unit) {
        val window = activity?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val state by cubeViewModel.state.collectAsState()
    var cubeView by remember { mutableStateOf<AnimCube?>(null) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val context = LocalContext.current

    val cube2DState = rememberCube2DState()

    LaunchedEffect(Unit) {
        launch(Dispatchers.Main) {
            cubeViewModel.cubeEvents
                .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
                .collect { event ->
                    when (event) {
                        is CubeEvent.CubeStateUpdated -> {
                            cube2DState.setState(event.kociembaState)
                            cubeView?.setCubeModel(event.arbitraryFormattedStates[ANIM_CUBE_STATE])
                        }

                        is CubeEvent.Move -> {
                            cube2DState.move(event.moveSequence)
                            val speed = 1
                            cubeView?.setSingleRotationSpeed(speed)
                            cubeView?.setDoubleRotationSpeed(speed)
                            cubeView?.animateSequenceAsync(event.moveSequence)
                            animCubeViewSolverNode.onAnimationEnded()
                        }

                        is CubeEvent.RequestRequired -> {}

                        CubeEvent.Solved -> {
//                        Toast.makeText(context, "Cube Solved!", Toast.LENGTH_SHORT).show()
                        }

                        CubeEvent.Unsupported -> {
                            // Handle unsupported event if needed
                        }

                    }

                }
        }
    }

    LaunchedEffect(Unit) {
        cubeViewModel.connectionEvents
            .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
            .collect { event ->
                when (event) {
                    is ConnectionState.Connected -> {
                        Toast.makeText(context, "Cube Connected!", Toast.LENGTH_SHORT).show()
                    }

                    ConnectionState.Disconnected -> {
                        cubeView?.setCubeModel(disconnectedCubeState)
                    }

                    else -> {}
                }
            }
    }


    Scaffold { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier.consumeWindowInsets(paddingValues)
        ) {
            var bundle by rememberSaveable() { mutableStateOf(Bundle()) }
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    val themedContext = ContextThemeWrapper(context, R.style.AnimCubeDark)
                    AnimCube(themedContext)
                        .apply {
                            setSingleRotationSpeed(3)
                            setDoubleRotationSpeed(3)
                            if (state.connectionState == ConnectionState.Disconnected) setCubeModel(
                                disconnectedCubeState
                            )
                            animCubeViewSolverNode.cubeView = this
                            if (!bundle.isEmpty) restoreState(bundle)
                        }
                        .also { cubeView = it }
                },
                update = { view -> },
                onRelease = {
                    animCubeViewSolverNode.cubeView = null
                    bundle = it.saveState() ?: Bundle()
                }
            )

            if (maxWidth > 400.dp) {
                Cube2D(
                    state = cube2DState,
                    modifier = Modifier
                        .size(240.dp)
                        .align(Alignment.CenterEnd)
                )
            }


            Box(
                modifier = Modifier
                    .padding(paddingValues)
            ) {
                CubeOverlay(
                    modifier = Modifier
                        .fillMaxSize(),
                    state = state,
                    onAction = { action -> cubeViewModel.handleAction(action) },
                )

                Icon(
                    modifier = Modifier //TODO: Remove appNavigator from the screen
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .clickable(onClick = { appNavigator.navigateTo(SearchDevicesRoute) })
                        .background(color = Color.Gray, shape = CircleShape)
                        .padding(8.dp),
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search"
                )
            }

        }
    }
}

