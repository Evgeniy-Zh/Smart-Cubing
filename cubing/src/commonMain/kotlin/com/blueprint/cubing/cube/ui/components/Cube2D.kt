package com.blueprint.cubing.cube.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.blueprint.cubing.core.logic.CubeMoves


class Cube2DState(
    private val initialState: String = "UUUUUUUUURRRRRRRRRFFFFFFFFFDDDDDDDDDLLLLLLLLLBBBBBBBBB"
) {

    val faceletNums by lazy {  List<String>(54) { i-> i.toString() } } // May be used for debug purposes

    var facelets by mutableStateOf(initialState)
        private set

    fun move(moveSequence: String) {
        facelets = CubeMoves.applyMoves(facelets, moveSequence)
    }

    fun setState(state: String) {
        facelets = state
    }

    fun reset() {
        facelets = initialState
    }

}

private class Cube2DStateSaver: Saver<Cube2DState, String> {
    override fun SaverScope.save(value: Cube2DState): String? {
        return value.facelets
    }

    override fun restore(value: String): Cube2DState? {
        return Cube2DState(value)
    }

}

@Composable
fun rememberCube2DState(initialState: String = "UUUUUUUUURRRRRRRRRFFFFFFFFFDDDDDDDDDLLLLLLLLLBBBBBBBBB") =
    rememberSaveable(saver = Cube2DStateSaver()) { Cube2DState(initialState) }

@Composable
fun Cube2D(state: Cube2DState, modifier: Modifier = Modifier) {
    val facelets = state.facelets

    Column(
        modifier = modifier
            .aspectRatio(4f / 3f) // Adjust aspect ratio for unfolded cube
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top row: empty, U, empty, empty
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) // Empty
            Face(facelets, 0..9,Modifier.weight(1f)) // U
            Box(modifier = Modifier.weight(1f)) // Empty
            Box(modifier = Modifier.weight(1f)) // Empty
        }

        // Middle row: L, F, R, B
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Face(facelets,36..45, Modifier.weight(1f)) // L
            Face(facelets, 18..27,Modifier.weight(1f)) // F
            Face(facelets, 9..18, Modifier.weight(1f)) // R
            Face(facelets, 45..54, Modifier.weight(1f)) // B
        }

        // Bottom row: empty, D, empty, empty
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) // Empty
            Face(facelets, 27..36,Modifier.weight(1f)) // D
            Box(modifier = Modifier.weight(1f)) // Empty
            Box(modifier = Modifier.weight(1f)) // Empty
        }
    }
}

@Composable
fun Face(facelets: String, range: IntRange, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.aspectRatio(1f),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        for (row in 0..2) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(1.dp)
            ) {

                for (col in 0..2) {
                    val index = range.start + row * 3 + col
                    val color = getColor(facelets[index])

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .background(color = Color.Black)
                            .padding(2.dp)
                            .background(color),
                        contentAlignment = Alignment.Center,
                        content = {}
                    )
                }
            }
        }
    }
}

private fun getColor(char: Char): Color {
    return when (char) {
        'U' -> Color.White
        'R' -> Color.Red
        'F' -> Color.Green
        'D' -> Color.Yellow
        'L' -> Color(0xFFFFA500) // Orange
        'B' -> Color.Blue
        else -> Color.Gray
    }
}

//@Preview
@Composable
private fun Preview() {
    val state = rememberCube2DState()
    Cube2D(state = state, modifier = Modifier.padding(16.dp))
}
