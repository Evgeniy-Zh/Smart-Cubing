package com.blueprint.desktopapp.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.blueprint.cubing.core.model.ConnectionState
import com.blueprint.cubing.cube.SolveStateManager
import com.blueprint.cubing.cube.ui.CubeViewModel
import com.blueprint.cubing.cube.ui.components.CubeOverlayContent


// --- Preview and mocked data ---
@Preview(showBackground = true, backgroundColor = 0xFFFFFF)
@Composable
fun CubeOverlayPreview() {
    // Create a mock solve state using the concrete data class
    val mockSolveState = SolveStateManager.SolveState.Solved(time = "00:12.345")

    // A simple lambda for action
    val onAction: (CubeViewModel.Action) -> Unit = {}

    // Use the content composable directly with mocked, simple values for preview.
    // This avoids needing a real CubeViewModel.State in the preview.
    CubeOverlayContent(
        modifier = Modifier.fillMaxSize(),
        connectionState = ConnectionState.Connecting,
        solveState = mockSolveState,
        cubeList = emptyList(),
        activeDevice = null,
        onAction = onAction
    )
}
