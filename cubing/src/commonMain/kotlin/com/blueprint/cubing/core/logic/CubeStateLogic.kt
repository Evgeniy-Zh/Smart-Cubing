package com.blueprint.cubing.core.logic


private val CORNER_FACELET_MAP = arrayOf(
    intArrayOf(8, 9, 20),   // URF
    intArrayOf(6, 18, 38),  // UFL
    intArrayOf(0, 36, 47),  // ULB
    intArrayOf(2, 45, 11),  // UBR
    intArrayOf(29, 26, 15), // DFR
    intArrayOf(27, 44, 24), // DLF
    intArrayOf(33, 53, 42), // DBL
    intArrayOf(35, 17, 51)  // DRB
)

private val EDGE_FACELET_MAP = arrayOf(
    intArrayOf(5, 10),   // UR
    intArrayOf(7, 19),   // UF
    intArrayOf(3, 37),   // UL
    intArrayOf(1, 46),   // UB
    intArrayOf(32, 16),  // DR
    intArrayOf(28, 25),  // DF
    intArrayOf(30, 43),  // DL
    intArrayOf(34, 52),  // DB
    intArrayOf(23, 12),  // FR
    intArrayOf(21, 41),  // FL
    intArrayOf(50, 39),  // BL
    intArrayOf(48, 14)   // BR
)
/**
 *
 * Convert Corner/Edge Permutation/Orientation cube state to the Kociemba facelets representation string
 *
 * Example - solved state:
 *   cp = [0, 1, 2, 3, 4, 5, 6, 7]
 *   co = [0, 0, 0, 0, 0, 0, 0, 0]
 *   ep = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11]
 *   eo = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
 *   facelets = "UUUUUUUUURRRRRRRRRFFFFFFFFFDDDDDDDDDLLLLLLLLLBBBBBBBBB"
 * Example - state after F R moves made:
 *   cp = [0, 5, 2, 1, 7, 4, 6, 3]
 *   co = [1, 2, 0, 2, 1, 1, 0, 2]
 *   ep = [1, 9, 2, 3, 11, 8, 6, 7, 4, 5, 10, 0]
 *   eo = [1, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0]
 *   facelets = "UUFUUFLLFUUURRRRRRFFRFFDFFDRRBDDBDDBLLDLLDLLDLBBUBBUBB"
 *
 * @param cp Corner Permutation
 * @param co Corner Orientation
 * @param ep Egde Permutation
 * @param eo Edge Orientation
 * @returns Cube state in the Kociemba facelets representation string
 *
 */
fun toKociembaFacelets(
    cp: Array<Int>, // Corner permutation (size 8)
    co: Array<Int>, // Corner orientation (size 8)
    ep: Array<Int>, // Edge permutation (size 12)
    eo: Array<Int>  // Edge orientation (size 12)
): String {

    val faces =   "URFDLB"
    val facelets = CharArray(54)

    // Initialize solved cube face colors
    for (i in 0 until 54) {
        facelets[i] = faces[i / 9]
    }

    // Corners
    for (i in 0 until 8) {
        for (p in 0 until 3) {
            val resStringIndex = CORNER_FACELET_MAP[i][(p + co[i]) % 3]
            val colorIndex = CORNER_FACELET_MAP[cp[i]][p] / 9
            facelets[resStringIndex] = faces[colorIndex]
        }
    }

    // Edges
    for (i in 0 until 12) {
        for (p in 0 until 2) {
            val resStringIndex = EDGE_FACELET_MAP[i][(p + eo[i]) % 2]
            val colorIndex = EDGE_FACELET_MAP[ep[i]][p] / 9
            facelets[resStringIndex] = faces[colorIndex]
        }
    }

    return facelets.concatToString()
}

/**
 *
 *         0  1  2
3  4  5
6  7  8

36 37 38  18 19 20   9 10 11  45 46 47
39 40 41  21 22 23  12 13 14  48 49 50
42 43 44  24 25 26  15 16 17  51 52 53

27 28 29
30 31 32
33 34 35
 *
 * */

object CubeMoves {

    // permutations for clockwise moves
    private val U = intArrayOf(
        6,3,0,7,4,1,8,5,2,          // u
        45,46,47,12,13,14,15,16,17,// r
        9,10,11,21,22,23,24,25,26, // f
        27,28,29,30,31,32,33,34,35,// d
        18,19,20,39,40,41,42,43,44, //l
        36,37,38,48,49,50,51,52,53 //b
    )

    private val D = intArrayOf(
        0,1,2,3,4,5,6,7,8,
        9,10,11,12,13,14,24,25,26,// r
        18,19,20,21,22,23,42,43,44,// f
        33,30,27,34,31,28,35,32,29,// d
        36,37,38,39,40,41,51,52,53,// l
        45,46,47,48,49,50,15,16,17 // b
    )

    private val R = intArrayOf(
        0,1,20,3,4,23,6,7,26,
        15,12,9,16,13,10,17,14,11,
        18,19,29,21,22,32,24,25,35,
        27,28,51,30,31,48,33,34,45,
        36,37,38,39,40,41,42,43,44,
        8,46,47,5,49,50,2,52,53
    )

    private val L = intArrayOf(
        53,1,2,50,4,5,47,7,8,
        9,10,11,12,13,14,15,16,17,
        0,19,20,3,22,23,6,25,26,
        18,28,29,21,31,32,24,34,35,// d
        42,39,36,43,40,37,44,41,38,
        45,46,33,48,49,30,51,52,27
    )

    private val F = intArrayOf(
        0,1,2,3,4,5,44,41,38, // u
        6,10,11,7,13,14,8,16,17,// r
        24,21,18,25,22,19,26,23,20,// f
        15,12,9,30,31,32,33,34,35,// d
        36,37,27,39,40,28,42,43,29, // l
        45,46,47,48,49,50,51,52,53 // b
    )

    private val B = intArrayOf(
        11,14,17,3,4,5,6,7,8,// u
        9,10,35,12,13,34,15,16,33,// r
        18,19,20,21,22,23,24,25,26,
        27,28,29,30,31,32,36,39,42,
        2,37,38,1,40,41,0,43,44,// l
        51,48,45,52,49,46,53,50,47
    )

    private val moves = mapOf(
        'U' to U,
        'D' to D,
        'R' to R,
        'L' to L,
        'F' to F,
        'B' to B
    )

    private fun applyPerm(cube: CharArray, perm: IntArray): CharArray {
        return CharArray(54) { cube[perm[it]] }
    }

    private fun applyMove(cube: CharArray, move: String): CharArray {
        val base = moves[move[0]]!!
        var result = cube

        val times = when {
            move.endsWith("2") -> 2
            move.endsWith("'") -> 3
            else -> 1
        }

        repeat(times) {
            result = applyPerm(result, base)
        }

        return result
    }

    fun applyMoves(state: String, sequence: String): String {
        var cube = state.toCharArray()

        val moves = sequence.trim().split("\\s+".toRegex())

        for (m in moves) {
            cube = applyMove(cube, m)
        }

        return cube.concatToString()
    }


}
