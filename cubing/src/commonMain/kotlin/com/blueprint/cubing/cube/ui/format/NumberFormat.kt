package com.blueprint.cubing.cube.ui.format

private val nine = "9999999999"

fun Long.format(digits: Int): String {
    var str = this.toString()

    if(str.length < digits)
        str = str.padStart(digits, '0')
    else if(str.length > digits) {
        str = nine.substring(0..digits)
    }

    return str
}