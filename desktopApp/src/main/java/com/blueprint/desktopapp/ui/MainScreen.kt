package com.blueprint.desktopapp.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.blueprint.cubing.core.logic.CubeStateProvider
import com.blueprint.cubing.core.model.CubeEvent
import com.blueprint.cubing.cube.ui.CubeViewModel
import com.blueprint.cubing.cube.ui.components.Cube2D
import com.blueprint.cubing.cube.ui.components.CubeOverlay
import com.blueprint.cubing.cube.ui.components.IsometricCube
import com.blueprint.cubing.cube.ui.components.rememberCube2DState
import com.blueprint.desktopapp.impl.CubeSolverImpl
import com.blueprint.desktopapp.impl.CubeStateProviderImpl

@Composable
internal fun MainScreen(
    vm: CubeViewModel,
    solverImpl: CubeSolverImpl,
    cubeStateProvider: CubeStateProvider,
) {

    val cube2dState = rememberCube2DState()

    cubeStateProvider as CubeStateProviderImpl

    val requester = remember { FocusRequester() }

    DisposableEffect(Unit) {
        cubeStateProvider.cube2dState2d = cube2dState
        onDispose {
            cubeStateProvider.cube2dState2d = null
        }
    }

    LaunchedEffect(Unit) {
        solverImpl.cubeState = cube2dState
        vm.cubeEvents.collect { event ->
            requester.requestFocus()
            println("Ui Event $event")
            when (event) {
                is CubeEvent.CubeStateUpdated -> {
                    cube2dState.setState(event.kociembaState)
                }

                is CubeEvent.Move -> {
                    cube2dState.move(event.moveSequence)
                    solverImpl.onAnimationEnded()
                }

                is CubeEvent.RequestRequired -> {}
                is CubeEvent.Solved -> {}
                is CubeEvent.Error -> {}
                CubeEvent.Unsupported -> {}
            }
        }
    }

    val state by vm.state.collectAsState()

    Scaffold(
        modifier = Modifier,
        backgroundColor = Color.Gray,
    ) { paddings ->
        BoxWithConstraints(modifier = Modifier.padding(paddings)) {
            IsometricCube(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 140.dp)
                    .size(180.dp),
                cubeState = cube2dState
            )

            if (maxWidth > 680.dp) {
                Cube2D(
                    modifier = Modifier.align(Alignment.CenterEnd).size(190.dp),
                    state = cube2dState
                )
            }

            CubeOverlay(
                modifier = Modifier.fillMaxSize(),
                mainButtonFocusRequester = requester,
                state = state,
                onAction = { vm.handleAction(it) }
            )

        }
    }

}
