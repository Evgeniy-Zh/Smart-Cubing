package com.blueprint.cubing.replay.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blueprint.cubing.core.model.CubeEvent
import com.blueprint.cubing.replay.ReplayStateManager
import com.blueprint.cubing.replay.SolveHistoryRepository
import com.blueprint.cubing.replay.model.PlayingState
import com.blueprint.cubing.replay.model.SolvePreview
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ReplayViewModel(
    private val solveHistoryRepository: SolveHistoryRepository,
    private val replayStateManager: ReplayStateManager,
) : ViewModel() {

    sealed interface Action {
        data class SelectSolve(val solvePreview: SolvePreview) : Action
        object Play : Action
        object Pause : Action
        object Stop : Action
        data object StepForward: Action
        data object StepBack: Action
        data class StepTo(val time: Long): Action
        data class SetSpeed(val speed: Speed) : Action
        data class DeleteSolve(val solvePreview: SolvePreview) : Action
        data class EditSolve(val solvePreview: SolvePreview) : Action
    }

    data class State(
        val solvePreviews: ImmutableList<SolvePreview> = persistentListOf(),
        val selectedSolvePreview: SolvePreview? = null,
        val playingState: PlayingState = PlayingState.Default,
        val isLoading: Boolean = true,
        val errorMessage: String? = null,
    )

    enum class Speed(val multiplier: Float) {
        SLOW(0.5f),
        NORMAL(1f),
        FAST(2f)
    }

    private val solvePreviewListState = solveHistoryRepository
        .observeSolveList(pagingParams = SolveHistoryRepository.PagingParams.Latest(300))
        .onEach { _isLoadingState.value = false }
    private val _isLoadingState = MutableStateFlow(true)
    private val _errorState = MutableStateFlow<String?>(null)
    private val _selectedSolvePreviewState = MutableStateFlow<SolvePreview?>(null)

    val cubeEvents: ReceiveChannel<CubeEvent> = replayStateManager.observeCubeEvents()

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
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = State()
    )

    fun handleAction(action: Action) {
        _errorState.value = null

        when (action) {
            is Action.SelectSolve -> selectSolvePreview(action.solvePreview)
            Action.Play -> play()
            Action.Pause -> pause()
            Action.Stop -> stop()
            Action.StepBack -> stepBack()
            Action.StepForward -> stepForward()
            is Action.SetSpeed -> setSpeed(action.speed.multiplier)
            is Action.DeleteSolve -> deleteSolve(action.solvePreview)
            is Action.EditSolve -> editSolve(action.solvePreview)
            is Action.StepTo -> stepTo(action.time)
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

    private fun stepForward() {
        replayStateManager.stepForward()
    }

    private fun stepBack() {
        replayStateManager.stepBackward()
    }

    private fun setSpeed(speed: Float) {
        replayStateManager.setSpeed(speed)
    }

    private fun stepTo(time: Long) {
        TODO("Not implemented")
    }

    private fun editSolve(solvePreview: SolvePreview) {
        TODO("Not implemented")
    }

    private fun deleteSolve(solvePreview: SolvePreview) = viewModelScope.launch {
        try {
            stop()
            solveHistoryRepository.deleteSolve(solvePreview)
            if (_selectedSolvePreviewState.value?.id == solvePreview.id) {
                _selectedSolvePreviewState.update { null }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _errorState.update { e.message ?: "Failed to delete replay" }
        }
    }

    override fun onCleared() {
        super.onCleared()
        replayStateManager.onClose()
    }
}