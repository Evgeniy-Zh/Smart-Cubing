package com.blueprint.cubing.cube.timer

import com.blueprint.cubing.core.format.TimeFormat
import com.blueprint.cubing.core.format.format
import com.blueprint.cubing.core.format.formatTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class CubeTimer(
    private val format: TimeFormat,
    private val getSysTimeStamp: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {

    private val _currentTime = MutableSharedFlow<Long>()
    val currentTime: Flow<String> = _currentTime.map { it.formatTime(format) }

    var startTimeStamp = 0L
    var endTimeStamp = 0L

    private var timerJob: kotlinx.coroutines.Job? = null

    fun start(coroutineScope: CoroutineScope) {
        startTimeStamp = getSysTimeStamp()
        timerJob = coroutineScope.launch(Dispatchers.Default) {
            while (true) {
                ensureActive()
                emitCurrentTime()
                delay(100)
            }
        }
    }

    suspend fun stop() {
        timerJob?.cancel()
        endTimeStamp = getSysTimeStamp()
        timerJob?.join()
    }

    fun reset() {
        timerJob?.cancel()
        startTimeStamp = 0L
        endTimeStamp = 0L
    }

    fun getTotalTime(): Long {
        if(startTimeStamp == 0L) return 0L
        val totalTime = endTimeStamp - startTimeStamp
        return totalTime
    }

    fun getTotalTimeFormatted(): String {
        val totalTime = getTotalTime()
        return totalTime.formatTime(format)
    }

    private suspend fun emitCurrentTime(end: Long = getSysTimeStamp()) {
        if(startTimeStamp == 0L) {
            _currentTime.emit(0L)
            return
        }
        val elapsed = end - startTimeStamp
        _currentTime.emit(elapsed)
    }

}