package com.blueprint.androidapp.ui.cube.mapper

import android.util.Log
import kotlin.text.iterator


/*

        0 1 2
        3 4 5
        6 7 8
*/

private val uMap = mapOf(0 to 6, 1 to 7, 2 to 8, 3 to 3, 4 to 4, 5 to 5, 6 to 0, 7 to 1, 8 to 2)
private val dMap = mapOf(0 to 0, 1 to 3, 2 to 6, 3 to 1, 4 to 4, 5 to 7, 6 to 2, 7 to 5, 8 to 8)
private val fMap = mapOf(0 to 0, 1 to 3, 2 to 6, 3 to 1, 4 to 4, 5 to 7, 6 to 2, 7 to 5, 8 to 8)
private val bMap = mapOf(0 to 0, 1 to 3, 2 to 6, 3 to 1, 4 to 4, 5 to 7, 6 to 2, 7 to 5, 8 to 8)
private val lMap = mapOf(0 to 2, 1 to 1, 2 to 0, 3 to 5, 4 to 4, 5 to 3, 6 to 8, 7 to 7, 8 to 6)
private val rMap = mapOf(0 to 0, 1 to 3, 2 to 6, 3 to 1, 4 to 4, 5 to 7, 6 to 2, 7 to 5, 8 to 8)

/**
 * This ugly mapper is needed because AnimCubeView has nonconventional facelet order
 */
fun String.mapToAnimCubeState(): String {

    //U R F D L B -> U D F B L R
    val array = Array<String>(6) { "" }
    val faceChunks =  this.chunked(9)
    for((i, s) in faceChunks.withIndex()) {
        val index = when(i) {
            0 -> 0 // U
            1 -> 5 // R -> D
            2 -> 2 // F
            3 -> 1 // D -> B
            4 -> 4 // L
            5 -> 3 // B -> R
            else -> throw IllegalStateException("Invalid face index: $i")
        }
        array[index] = s
    }
    val indexMappers = arrayOf(uMap, dMap, fMap, bMap, lMap, rMap)
    for (i in array.indices) {
        val mapper = indexMappers[i]
        val original = array[i]
        val mapped = CharArray(9)
        for (j in original.indices) {
            val newIndex = mapper[j]!!
            mapped[newIndex] = original[j]
        }
        array[i] = String(mapped)
    }
    val reordered = array.joinToString ("")
    val sb = StringBuilder()
    for (ch in reordered) {
        sb.append(
            when (ch) {
                'U' -> '0'
                'D' -> '1'
                'F' -> '2'
                'B' -> '3'
                'L' -> '4'
                'R' -> '5'
                else -> ch
            }
        )
    }
    val s1 = sb.toString()
    Log.d("MessageParser", "Source string      : $this")
    Log.d("MessageParser", "Reordered string   : $reordered")
    Log.d("MessageParser", "Mapped state string: $s1")

    return s1
}
