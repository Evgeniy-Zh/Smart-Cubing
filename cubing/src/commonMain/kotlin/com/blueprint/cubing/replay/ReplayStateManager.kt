package com.blueprint.cubing.replay

import com.blueprint.cubing.core.format.TimeFormat
import com.blueprint.cubing.core.model.CubeEvent
import com.blueprint.cubing.cube.timer.CubeTimer
import com.blueprint.cubing.replay.model.PlayingState
import com.blueprint.cubing.replay.model.SolvePreview
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class ReplayStateManager(
    private val playbackRepository: PlaybackRepository
) {

    private val coroutineScope = CoroutineScope(kotlinx.coroutines.Dispatchers.Default)

    private val timer = ReplayTimer(format = TimeFormat.SOLVING)

    private val currentReplayId: MutableStateFlow<String?> = MutableStateFlow(null)

    private var totalTime: Long = 0L

    private val _playingState: MutableStateFlow<PlayingState> =
        MutableStateFlow(PlayingState.Default)
    val playingState: StateFlow<PlayingState> = _playingState

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeCubeEvents(): Flow<CubeEvent> {
        return currentReplayId.transformLatest { replayId ->
            replayId ?: return@transformLatest
            stop()
            var elapsed = 0L
            val events = playbackRepository.getAllReplayEvents(replayId)
            totalTime = events.filterIsInstance<CubeEvent.Move>().sumOf { it.elapsed } //TODO: get total time

            emit(events[0] as CubeEvent.CubeStateUpdated)
            for (i in 1..events.lastIndex) {
                val event = events[i]
                val timeMultiplier = playingState.value.speed
                if(event is CubeEvent.Move) {
                    elapsed = event.elapsed
                }
                val d = (elapsed / timeMultiplier).toLong()

                playingState.first { it.status == PlayingState.Status.PLAYING }
                delay(d.milliseconds)
                emit(event)
                if (event is CubeEvent.Move) {
                    elapsed = event.elapsed
                }
            }
        }
    }

    fun setReplay(replay: SolvePreview) {
        currentReplayId.value = replay.id
    }

    fun play() {
        _playingState.update {
            it.copy(status = PlayingState.Status.PLAYING)
        }

        timer.start(totalTime)
        coroutineScope.launch {
            timer.currentTimeFormatted.collect { time ->
                _playingState.update { it.copy(time = time) }
            }
        }
    }

    fun pause() {
        _playingState.update {
            it.copy(status = PlayingState.Status.PAUSED)
        }
        timer.pause()
    }

    fun stop() {
        _playingState.update {
            it.copy(status = PlayingState.Status.STOPPED)
        }
        val id = currentReplayId.value
        currentReplayId.value = null
        currentReplayId.value = id
        timer.stop()
        _playingState.update { it.copy(time = timer.getCurrentTimeFormatted()) }
    }

    fun setSpeed(speed: Float) {
        _playingState.update {
            it.copy(speed = speed)
        }
        timer.setSpeed(speed)
    }

    fun onClose() {
        coroutineScope.cancel()
        timer.cancel()
    }

}