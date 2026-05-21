package com.blueprint.cubing.replay.model

data class PlayingState(
    val replayName: String,
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
            replayName = "",
            time = "",
            speed = 1f,
            status = Status.STOPPED
        )
    }
}