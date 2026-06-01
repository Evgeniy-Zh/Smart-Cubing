package com.blueprint.cubing.replay.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blueprint.cubing.core.flow.shareSuspendingWhileNoSubs
import com.blueprint.cubing.core.model.CubeEvent
import com.blueprint.cubing.replay.ReplayHistoryRepository
import com.blueprint.cubing.replay.ReplayStateManager
import com.blueprint.cubing.replay.model.PlayingState
import com.blueprint.cubing.replay.model.Replay
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ReplayViewModel(
    private val replayHistoryRepository: ReplayHistoryRepository,
    private val replayStateManager: ReplayStateManager,
) : ViewModel() {

    sealed interface Action {
        object LoadReplays : Action
        data class SelectReplay(val replay: Replay) : Action
        object Play : Action
        object Pause : Action
        object Stop : Action
        data class SetSpeed(val speed: Speed) : Action
        data class DeleteReplay(val replay: Replay) : Action
    }

    data class State(
        val replayList: ImmutableList<Replay> = persistentListOf(),
        val selectedReplay: Replay? = null,
        val playingState: PlayingState = PlayingState.Default,
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
    )

    enum class Speed(val multiplier: Float) {
        SLOW(0.5f),
        NORMAL(1f),
        FAST(2f)
    }

    private val _replayListState = MutableStateFlow<List<Replay>>(emptyList())
    private val _isLoadingState = MutableStateFlow(false)
    private val _errorState = MutableStateFlow<String?>(null)
    private val _selectedReplayState = MutableStateFlow<Replay?>(null)

    val cubeEvents: SharedFlow<CubeEvent> = replayStateManager.observeCubeEvents()
        .shareSuspendingWhileNoSubs(scope = viewModelScope)

    val state: StateFlow<State> = combine(
        _replayListState.map { it.toPersistentList() },
        _selectedReplayState,
        replayStateManager.playingState,
        _isLoadingState,
        _errorState
    ) { replayList, selectedReplay, playingState, isLoading, error ->
        State(
            replayList = replayList,
            selectedReplay = selectedReplay,
            playingState = playingState,
            isLoading = isLoading,
            errorMessage = error
        )
    }.onStart { loadReplays() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = State()
        )

    fun handleAction(action: Action) {
        when (action) {
            Action.LoadReplays -> loadReplays()
            is Action.SelectReplay -> selectReplay(action.replay)
            Action.Play -> play()
            Action.Pause -> pause()
            Action.Stop -> stop()
            is Action.SetSpeed -> setSpeed(action.speed.multiplier)
            is Action.DeleteReplay -> deleteReplay(action.replay)
        }
    }

    private fun loadReplays() = viewModelScope.launch {
        _isLoadingState.update { true }
        _errorState.update { null }
        try {
            val replays = replayHistoryRepository.getReplayList(
                ReplayHistoryRepository.PagingParams.Latest(count = 50)
            )
            _replayListState.update { replays }
        } catch (e: Exception) {
            _errorState.update { e.message ?: "Failed to load replays" }
        } finally {
            _isLoadingState.update { false }
        }
    }

    private fun selectReplay(replay: Replay) {
        _selectedReplayState.update { replay }
        replayStateManager.setReplay(replay)
    }

    private fun play() {
        replayStateManager.play()
    }

    private fun pause() {
        replayStateManager.pause()
    }

    private fun stop() {
        replayStateManager.stop()
    }

    private fun setSpeed(speed: Float) {
        replayStateManager.setSpeed(speed)
    }

    private fun deleteReplay(replay: Replay) = viewModelScope.launch {
        try {
            replayHistoryRepository.deleteReplay(replay)
            _replayListState.update { list ->
                list.filter { it.id != replay.id }
            }
            if (_selectedReplayState.value?.id == replay.id) {
                _selectedReplayState.update { null }
            }
        } catch (e: Exception) {
            _errorState.update { e.message ?: "Failed to delete replay" }
        }
    }

    override fun onCleared() {
        super.onCleared()
        replayStateManager.onClose()
    }
}