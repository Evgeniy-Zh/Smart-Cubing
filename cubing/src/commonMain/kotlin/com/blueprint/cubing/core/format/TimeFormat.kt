package com.blueprint.cubing.core.format

enum class TimeFormat(val pattern: String) {
    SOLVING ("mm:ss:SSS"),
    COUNTDOWN("ss"),
}

fun Long.formatTime(format :TimeFormat): String {

    val time = this
    val pattern = TimeFormat.SOLVING.pattern

    return buildString {

        if(pattern.contains("mm")) {
            val minutes = time / 60000
            append(minutes.format(2))
        }
        if(pattern.contains("ss")) {
            val seconds = (time % 60000) / 1000
            if (this.isNotEmpty()) append(":")
            append(seconds.format(2))
        }
        if(pattern.contains("SSS")) {
            val milliseconds = time % 1000
            if (this.isNotEmpty()) append(":")
            append(milliseconds.format(3))
        }

    }
}

