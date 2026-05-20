package com.blueprint.androidapp.ui.cube.ext

import com.catalinjurjiu.animcubeandroid.AnimCube
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

suspend fun AnimCube.animateSequenceAsync(moveSequence: String) {
   moveSequence.split(" ").forEach {
       animateMoveAsync(it)
   }
}

// there are a bug in AnimCube for multiple moves in a sequence
// so we process it one by one
private suspend fun AnimCube.animateMoveAsync(move: String) {
    suspendCancellableCoroutine { continuation ->
        setMoveSequence(move)
        animateMoveSequence()
        setOnAnimationFinishedListener {
            if (continuation.isActive)
                continuation.resume(Unit)
        }
        continuation.invokeOnCancellation {
            setOnAnimationFinishedListener(null)
        }
    }
}

val disconnectedCubeState = CharArray(54) { '0' }.concatToString()

private val solvedState: Array<IntArray> by lazy {
    val array = Array(6) { IntArray(9) }

    for (i in 0..<6) {
        for (j in 0..8) {
            array[i][j] = i
        }
    }
    array
}

suspend fun AnimCube.isSolved(): Boolean = withContext(Dispatchers.Default) {
    val currentState = this@isSolved.cubeModel
    for (i in 0..<6) {
        if (!currentState[i].contentEquals(solvedState[i])) {
            return@withContext false
        }
    }
    return@withContext true
}