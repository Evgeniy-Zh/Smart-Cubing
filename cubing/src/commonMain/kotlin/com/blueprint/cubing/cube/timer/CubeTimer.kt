package com.blueprint.cubing.cube.timer

import com.blueprint.cubing.cube.ui.format.format
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
class CubeTimer() {

    private val _currentTime = MutableSharedFlow<Long>()
    val currentTime: Flow<String> = _currentTime.map { formatTime(it) }

    var startTimeStamp = 0L
    var endTimeStamp = 0L

    companion object {

        fun formatTime(time: Long): String {
            val minutes = time / 60000
            val seconds = (time % 60000) / 1000
            val milliseconds = time % 1000

            return "${minutes.format(2)}:${seconds.format(2)}:${milliseconds.format(3)}"
        }
    }

    private var timerJob: kotlinx.coroutines.Job? = null

    fun start(coroutineScope: CoroutineScope) {
        startTimeStamp = Clock.System.now().toEpochMilliseconds()
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
        endTimeStamp = Clock.System.now().toEpochMilliseconds()
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
        return formatTime(totalTime)
    }

    private suspend fun emitCurrentTime(end: Long = Clock.System.now().toEpochMilliseconds()) {
        if(startTimeStamp == 0L) {
            _currentTime.emit(0L)
            return
        }
        val elapsed = end - startTimeStamp
        _currentTime.emit(elapsed)
    }

}