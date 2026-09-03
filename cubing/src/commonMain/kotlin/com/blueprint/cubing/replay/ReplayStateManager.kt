package com.blueprint.cubing.replay

import com.blueprint.cubing.core.format.TimeFormat
import com.blueprint.cubing.core.model.CubeEvent
import com.blueprint.cubing.replay.model.PlayingState
import com.blueprint.cubing.replay.model.SolvePreview
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class ReplayStateManager(
    private val playbackRepository: PlaybackRepository,
    private val getSysTimeStamp: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {

    private val coroutineScope = CoroutineScope(kotlinx.coroutines.Dispatchers.Default)

    private val timer = ReplayTimer(format = TimeFormat.SOLVING)

    private val currentReplayId: MutableStateFlow<String?> = MutableStateFlow(null)

    private var totalTime: Long = 0L

    private val _playingState: MutableStateFlow<PlayingState> =
        MutableStateFlow(PlayingState.Default)
    val playingState: StateFlow<PlayingState> = _playingState

    init {
        coroutineScope.launch {
            timer.currentTimeFormatted.collect { time ->
                _playingState.update { it.copy(time = time) }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeCubeEvents(): ReceiveChannel<CubeEvent> {

        return coroutineScope.produce(Dispatchers.Main.immediate) {

            currentReplayId.collectLatest { replayId ->
                replayId ?: return@collectLatest

                var elapsed = 0L
                val events = playbackRepository.getAllReplayEvents(replayId)

                send(events[0] as CubeEvent.CubeStateUpdated)

                val eventTimestamps = events.integrateMoveElapsedTimes()
                totalTime = eventTimestamps.last()

                var i = 1

                while(i < events.size) {

                    playingState.first { it.status == PlayingState.Status.PLAYING }

                    val t1 = getSysTimeStamp()
                    while (i < events.size && elapsed >= eventTimestamps[i]) {
                        send(events[i])
                        i++
                        yield()
                    }
                    delay(20.milliseconds)
                    val t2 = getSysTimeStamp()
                    elapsed+= ((t2 - t1) * _playingState.value.speed).toLong()

                }

                _playingState.update { it.copy(status = PlayingState.Status.FINISHED) }
            }
        }
    }

    fun setReplay(replay: SolvePreview) {
        currentReplayId.value = replay.id
        stop()
    }

    fun play() {
        _playingState.update {
            it.copy(status = PlayingState.Status.PLAYING)
        }
        timer.start(totalTime)
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

    fun stepForward() {
        TODO("Not implemented")
    }

    fun stepBackward() {
        TODO("Not implemented")
    }

    fun onClose() {
        coroutineScope.cancel()
        timer.cancel()
    }

    private fun List<CubeEvent>.integrateMoveElapsedTimes(): LongArray {
        var sum = 0L
        val arr = LongArray(this.size)
        for(i in this.indices) {
            val move = this[i] as? CubeEvent.Move ?: continue
            sum += move.elapsed
            arr[i] = sum
        }
        return arr
    }

}