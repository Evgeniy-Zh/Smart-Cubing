package com.blueprint.cubing.cube.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
// ...existing code...
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.blueprint.cubing.cube.ui.CubeViewModel
import com.blueprint.cubing.core.model.ConnectionState
import com.blueprint.cubing.core.model.CubeDevice
import com.blueprint.cubing.cube.SolveStateManager

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

        // Reset button (top-end) — invisible unless there is an active connected device
        if (activeDevice != null && connectionState is ConnectionState.Connected) {
            IconButton(
                onClick = { onAction(CubeViewModel.Action.Reset) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset",
                    tint = Color.White
                )
            }
        }

        ActiveDevicesIndicator(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp),
            connectionState = connectionState,
            cubeList = cubeList,
            activeDevice = activeDevice,
            onAction = onAction,
        )
        // Middle: intentionally left empty; cube will be visible underneath this overlay.

        // Bottom: timer, solve state and action button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                    color = Color.Gray,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

    }
}

@Composable
fun ActiveDevicesIndicator(
    modifier: Modifier = Modifier,
    activeDevice: CubeDevice?,
    connectionState: ConnectionState,
    cubeList: List<CubeDevice>,
    onAction: (CubeViewModel.Action) -> Unit,
) {
    // Dropdown-only indicator: shows collapsed summary and expands to show available devices
    val expanded = remember { mutableStateOf(false) }

    val connectionStr = when (connectionState) {
        is ConnectionState.Connected -> "Connected"
        ConnectionState.Disconnected -> "Disconnected"
        ConnectionState.Connecting -> "Connecting"
        ConnectionState.Disconnecting -> "Disconnecting"
        ConnectionState.FailedToConnect -> "Failed to connect"
        ConnectionState.Initializing -> "Initializing"
    }

    Box(
        modifier = modifier
            .padding(vertical = 8.dp, horizontal = 12.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xAA111111)),
            modifier = Modifier
                .background(Color.Transparent)
                .clickable { expanded.value = true }
                .padding(4.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = activeDevice?.name ?: "No Device",
                    fontSize = 14.sp,
                    color = Color.White
                )
                Text(
                    text = connectionStr,
                    fontSize = 12.sp,
                    color = Color.White
                )
            }
        }

        DropdownMenu(
            containerColor = Color(0xCC111111),
            expanded = expanded.value,
            onDismissRequest = { expanded.value = false }
        ) {
            if (cubeList.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("No Devices", color = Color.White) },
                    onClick = { expanded.value = false }
                )
            } else {
                cubeList.forEach { device ->
                    DropdownMenuItem(
                        text = { Text(device.name, color = Color.White) },
                        onClick = {
                            onAction(CubeViewModel.Action.SelectDevice(device))
                            expanded.value = false
                        }
                    )
                }
            }
        }


    }

}

