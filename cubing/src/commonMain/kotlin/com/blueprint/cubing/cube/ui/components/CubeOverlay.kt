package com.blueprint.cubing.cube.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blueprint.cubing.core.model.ConnectionState
import com.blueprint.cubing.core.model.CubeDevice
import com.blueprint.cubing.cube.SolveStateManager
import com.blueprint.cubing.cube.ui.CubeViewModel
import com.blueprint.cubing.replay.model.SolvePreview
import com.blueprint.cubing.replay.ui.SolveHistoryList

var TimerFontFamily: FontFamily = FontFamily.Monospace

@Composable
fun CubeOverlay(
    modifier: Modifier = Modifier,
    state: CubeViewModel.State,
    onAction: (CubeViewModel.Action) -> Unit,
    mainButtonFocusRequester: FocusRequester? = null,
) {
    val solveState = state.solveState
    val connectionState = state.connectionState
    val cubeList = state.cubeDevices
    val activeDevice = state.activeDevice

    CubeOverlayContent(
        modifier = modifier,
        connectionState = connectionState,
        solveState = solveState,
        cubeList = cubeList,
        solveHistory = state.solveHistory,
        activeDevice = activeDevice,
        onAction = onAction,
        mainButtonFocusRequester = mainButtonFocusRequester,
    )
}

@Composable
fun CubeOverlayContent(
    modifier: Modifier = Modifier,
    connectionState: ConnectionState,
    solveState: SolveStateManager.SolveState,
    cubeList: List<CubeDevice>,
    solveHistory: List<SolvePreview>,
    activeDevice: CubeDevice?,
    onAction: (CubeViewModel.Action) -> Unit,
    mainButtonFocusRequester: FocusRequester? = null,
) {

    fun Modifier.spaceBarEvents(): Modifier {
        var m = this.onKeyEvent { event ->
            if (event.key == Key.Spacebar) {
                onAction(CubeViewModel.Action.SolveAction)
                true
            } else {
                false
            }
        }
        if (mainButtonFocusRequester != null)
            m = m.focusRequester(mainButtonFocusRequester)
        return m
    }


    // Top-right reset button should not affect centering of the active devices indicator.

    // determine if current state is solved and any time text
    val solved = solveState is SolveStateManager.SolveState.Solved
    val timeText = when (solveState) {
        is SolveStateManager.WithTime -> solveState.time
        else -> "00:00:000"
    }

    // simple scale animation for the timer when solved
    val scale by animateFloatAsState(targetValue = if (solved) 1.08f else 1f, animationSpec = tween(durationMillis = 400))

    Box(
        modifier = modifier.fillMaxSize(), // allow parent to control size (the caller can pass full screen modifier)
        contentAlignment = Alignment.Center
    ) {
        // Transparent background by default so cube remains visible behind this overlay

        CubeControls(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
            connectionState = connectionState,
            cubeList = cubeList,
            activeDevice = activeDevice,
            onAction = onAction,
        )
        // Middle: intentionally left empty; cube will be visible underneath this overlay.


        // Bottom: timer, solve state and action button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {

            // Clickable timer area
            val tapActionLabel = when (solveState) {
                SolveStateManager.SolveState.Idle -> "Tap to Start"
                is SolveStateManager.SolveState.Inspecting -> "Move any face to Start"
                is SolveStateManager.SolveState.Solving -> "Tap to Give Up"
                is SolveStateManager.SolveState.Scrambling -> "Tap to Start"
                is SolveStateManager.SolveState.Solved -> "Tap to Reset"
            }

            Column(
                modifier = Modifier
                    .widthIn(max = 400.dp)
                    .fillMaxWidth()
                    .spaceBarEvents()
                    .clickable { onAction(CubeViewModel.Action.SolveAction) }
                    .padding(vertical = 24.dp)
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Timer (digital watch style)
                Text(
                    text = timeText,
                    fontFamily = TimerFontFamily, // uses custom font when available, otherwise monospace fallback
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center,
                    color = if (solved) Color(0xFFC5F3C1) else Color.LightGray,
                    modifier = Modifier.scale(scale)
                )

                // Solve state label
                Text(
                    text = when (solveState) {
                        SolveStateManager.SolveState.Idle -> ""
                        is SolveStateManager.SolveState.Solved -> "Solved"
                        is SolveStateManager.SolveState.Solving -> "Solving..."
                        is SolveStateManager.SolveState.Inspecting -> "Inspecting..."
                        is SolveStateManager.SolveState.Scrambling -> "Scrambling..."
                    },
                    fontSize = 16.sp,
                    color = Color.White
                )

                // Tap action label
                Text(
                    text = tapActionLabel,
                    fontSize = 13.sp,
                    color = Color.LightGray,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            SolveHistoryList(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .heightIn(max = 100.dp),
                solvePreviews = solveHistory,
                onItemClick = { onAction(CubeViewModel.Action.OpenReplays) }
            )

        }

    }
}

