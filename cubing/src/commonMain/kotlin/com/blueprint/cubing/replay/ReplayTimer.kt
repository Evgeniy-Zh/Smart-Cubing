package com.blueprint.cubing.replay

import com.blueprint.cubing.core.format.TimeFormat
import com.blueprint.cubing.core.format.formatTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class ReplayTimer (
    private val format: TimeFormat,
    private val getSysTimeStamp: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {

    private val _currentTime = MutableStateFlow<Long>(0)

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    private var timerJob: Job? = null

    private var speed: Float = 1f

    private var totalTime: Long = 0L

    val currentTimeFormatted: Flow<String> = _currentTime.map {
        it.formatTime(format)
    }

    val currentTime: Flow<Long> = _currentTime.asSharedFlow()

    fun start(totalTime: Long){

        val step = 100L
        timerJob = coroutineScope.launch {
            var elapsed = 0L
            while (_currentTime.value < totalTime) {
                ensureActive()
                _currentTime.update { it + (elapsed * speed).toLong() }

                elapsed = getSysTimeStamp()
                val d = step.milliseconds
                delay(d)
                elapsed = getSysTimeStamp() - elapsed
            }
            _currentTime.tryEmit(totalTime)
        }

    }

    fun pause() {
        timerJob?.cancel()
    }

    fun stop() {
        timerJob?.cancel()
        _currentTime.update { 0L }
    }

    fun setSpeed(speed: Float) {
        this.speed = speed
    }

    fun setProgress(progress: Float) {
        _currentTime.update { (totalTime * progress).toLong() }
    }

    fun getCurrentTime(): Long {
        return _currentTime.value
    }

    fun getCurrentTimeFormatted(): String {
        return _currentTime.value.formatTime(format)
    }

    fun cancel() {
        coroutineScope.cancel()
    }

}