package com.blueprint.cubing.replay.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blueprint.cubing.core.flow.shareSuspendingWhileNoSubs
import com.blueprint.cubing.core.model.CubeEvent
import com.blueprint.cubing.replay.SolveHistoryRepository
import com.blueprint.cubing.replay.ReplayStateManager
import com.blueprint.cubing.replay.model.PlayingState
import com.blueprint.cubing.replay.model.SolvePreview
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
    private val solveHistoryRepository: SolveHistoryRepository,
    private val replayStateManager: ReplayStateManager,
) : ViewModel() {

    sealed interface Action {
        object LoadSolvePreviews : Action
        data class SelectSolve(val solvePreview: SolvePreview) : Action
        object Play : Action
        object Pause : Action
        object Stop : Action
        data class SetSpeed(val speed: Speed) : Action
        data class DeleteSolve(val solvePreview: SolvePreview) : Action
    }

    data class State(
        val solvePreviews: ImmutableList<SolvePreview> = persistentListOf(),
        val selectedSolvePreview: SolvePreview? = null,
        val playingState: PlayingState = PlayingState.Default,
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
    )

    enum class Speed(val multiplier: Float) {
        SLOW(0.5f),
        NORMAL(1f),
        FAST(2f)
    }

    private val solvePreviewListState = MutableStateFlow<List<SolvePreview>>(emptyList())
    private val _isLoadingState = MutableStateFlow(false)
    private val _errorState = MutableStateFlow<String?>(null)
    private val _selectedSolvePreviewState = MutableStateFlow<SolvePreview?>(null)

    val cubeEvents: SharedFlow<CubeEvent> = replayStateManager.observeCubeEvents()
        .shareSuspendingWhileNoSubs(scope = viewModelScope)

    val state: StateFlow<State> = combine(
        solvePreviewListState.map { it.toPersistentList() },
        _selectedSolvePreviewState,
        replayStateManager.playingState,
        _isLoadingState,
        _errorState
    ) { solvePreviews, selectedSolvePreview, playingState, isLoading, error ->
        State(
            solvePreviews = solvePreviews,
            selectedSolvePreview = selectedSolvePreview,
            playingState = playingState,
            isLoading = isLoading,
            errorMessage = error
        )
    }.onStart { loadSolvePreviews() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = State()
        )

    fun handleAction(action: Action) {
        when (action) {
            Action.LoadSolvePreviews -> loadSolvePreviews()
            is Action.SelectSolve -> selectSolvePreview(action.solvePreview)
            Action.Play -> play()
            Action.Pause -> pause()
            Action.Stop -> stop()
            is Action.SetSpeed -> setSpeed(action.speed.multiplier)
            is Action.DeleteSolve -> deleteSolve(action.solvePreview)
        }
    }

    private fun loadSolvePreviews() = viewModelScope.launch {
        _isLoadingState.update { true }
        _errorState.update { null }
        try {
            val previews = solveHistoryRepository.getSolveList(
                SolveHistoryRepository.PagingParams.Latest(count = 50)
            )
            solvePreviewListState.update { previews }
        } catch (e: Exception) {
            _errorState.update { e.message ?: "Failed to load solve previews" }
        } finally {
            _isLoadingState.update { false }
        }
    }

    private fun selectSolvePreview(solvePreview: SolvePreview) {
        _selectedSolvePreviewState.update { solvePreview }
        replayStateManager.setReplay(solvePreview)
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

    private fun deleteSolve(solvePreview: SolvePreview) = viewModelScope.launch {
        try {
            solveHistoryRepository.deleteSolve(solvePreview)
            solvePreviewListState.update { list ->
                list.filter { it.id != solvePreview.id }
            }
            if (_selectedSolvePreviewState.value?.id == solvePreview.id) {
                _selectedSolvePreviewState.update { null }
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