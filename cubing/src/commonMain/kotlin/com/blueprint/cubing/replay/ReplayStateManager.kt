package com.blueprint.cubing.replay

import com.blueprint.cubing.core.format.TimeFormat
import com.blueprint.cubing.core.model.CubeEvent
import com.blueprint.cubing.cube.timer.CubeTimer
import com.blueprint.cubing.replay.model.PlayingState
import com.blueprint.cubing.replay.model.Replay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ReplayStateManager(
    private val playbackRepository: PlaybackRepository
) {

    private val coroutineScope = CoroutineScope(kotlinx.coroutines.Dispatchers.Default)

    private val cubeTimer = CubeTimer(format = TimeFormat.SOLVING)

    private val currentReplayId: MutableStateFlow<String?> = MutableStateFlow(null)

    private val _playingState: MutableStateFlow<PlayingState> =
        MutableStateFlow(PlayingState.Default)
    val playingState: StateFlow<PlayingState> = _playingState

    fun observeCubeEvents(): Flow<CubeEvent> {
        return currentReplayId.transform { replayId ->
            var elapsed = 0L
            replayId ?: return@transform
            playbackRepository.replay(replayId).collect { event ->

                val timeMultiplier = playingState.value.speed
                val d = (elapsed * timeMultiplier).toLong()
                delay(d)

                playingState.first { it.status == PlayingState.Status.PLAYING }
                emit(event)
                if (event is CubeEvent.Move) {
                    elapsed = event.elapsed
                }

            }
        }
    }

    fun setReplay(replay: Replay) {
        currentReplayId.value = replay.id
    }

    fun play() {
        _playingState.update {
            it.copy(status = PlayingState.Status.PLAYING)
        }
        cubeTimer.start(coroutineScope = coroutineScope, speed = _playingState.value.speed)
        coroutineScope.launch {
            cubeTimer.currentTime.collect { time ->
                _playingState.update { it.copy(time = time) }
            }
        }
    }

    fun pause() {
        _playingState.update {
            it.copy(status = PlayingState.Status.PAUSED)
        }
    }

    fun stop() {
        _playingState.update {
            it.copy(status = PlayingState.Status.STOPPED)
        }
        currentReplayId.value = null
    }

    fun setSpeed(speed: Float) {
        _playingState.update {
            it.copy(speed = speed)
        }
    }

    fun onClose() {
        coroutineScope.cancel()
    }

}