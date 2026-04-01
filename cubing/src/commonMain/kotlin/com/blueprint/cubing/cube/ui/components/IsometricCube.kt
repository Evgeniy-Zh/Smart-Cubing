package com.blueprint.cubing.cube.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp


@Composable
fun IsometricCube(
    modifier: Modifier = Modifier,
    cubeState: Cube2DState,
) {

    Box(
        modifier = modifier
    ) {

        IsoFace( //U
            modifier = Modifier.graphicsLayer {
                val dY = -this.size.height / 2.7f
                translationY = dY
                rotationZ = -300f
            },
            facelets = cubeState.facelets,
            range = 0..8,
            angle1 = 1,
            angle2 = -1
        )
        IsoFace( //R
            modifier = Modifier.graphicsLayer {
                val dY = this.size.height / 3.99f
                val dX = this.size.height / 2.8f
                translationY = dY
                translationX = dX
                rotationZ = 0f
            },
            facelets = cubeState.facelets,
            range = 9..18,
            angle1 = -1,
            angle2 = 1
        )

        IsoFace( //F
            modifier = Modifier.graphicsLayer {
                val dY = this.size.height / 3.99f
                val dX = -this.size.height / 2.8f
                translationY = dY
                translationX = dX
                rotationZ = 0f
            },
            facelets = cubeState.facelets,
            range = 18..27,
            angle1 = 1,
            angle2 = 1
        )

    }

}


@Composable
private fun IsoFace(
    modifier: Modifier,
    facelets: String, range: IntRange,
    angle1: Int,
    angle2: Int,
) {
    val dist = 400.dp.value
    Box(modifier) {
        Face(
            modifier = Modifier.graphicsLayer {
                rotationX = angle2 * 35.264f
                rotationY = angle1 * 45f
                cameraDistance = dist

            },
            facelets = facelets,
            range = range,
        )
    }
}