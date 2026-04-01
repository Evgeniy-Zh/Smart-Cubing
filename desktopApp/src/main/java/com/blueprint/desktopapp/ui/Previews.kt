package com.blueprint.desktopapp.ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.blueprint.cubing.cube.ui.components.IsometricCube
import com.blueprint.cubing.cube.ui.components.rememberCube2DState


@Preview
@Composable
private fun IsometricPreview() = Box(modifier = Modifier.size(300.dp)) {
    val state = rememberCube2DState("UUUUUUUUURRRRRRRRRFFFFFFFFFDDDDDDDDDLLLLLLLLLBBBBBBBBB")
    IsometricCube(
        modifier = Modifier.align(Alignment.Center).size(150.dp),
        cubeState = state,
    )
}