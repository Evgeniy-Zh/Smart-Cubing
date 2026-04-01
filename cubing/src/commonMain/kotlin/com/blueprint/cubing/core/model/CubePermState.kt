package com.blueprint.cubing.core.model

interface CubePermState {
    /** Corner Permutation: 8 elements, values from 0 to 7 */
    val CP: Array<Int>;

    /** Corner Orientation: 8 elements, values from 0 to 2 */
    val CO: Array<Int>;

    /** Edge Permutation: 12 elements, values from 0 to 11 */
    val EP: Array<Int>;

    /** Edge Orientation: 12 elements, values from 0 to 1 */
    val EO: Array<Int>;
}