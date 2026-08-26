package com.blueprint.cubing.replay.model

data class PlayingState(
    val solveName: String,
    val time: String,
    val speed: Float,
    val status: Status,
) {
    enum class Status {
        PLAYING,
        PAUSED,
        STOPPED
    }

    companion object {
        val Default = PlayingState(
            solveName = "",
            time = "",
            speed = 1f,
            status = Status.STOPPED
        )
    }
}