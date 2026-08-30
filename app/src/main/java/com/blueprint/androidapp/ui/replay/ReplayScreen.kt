package com.blueprint.androidapp.ui.replay

import android.annotation.SuppressLint
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import com.blueprint.androidapp.R
import com.blueprint.androidapp.ui.cube.ext.animateSequenceAsync
import com.blueprint.androidapp.ui.cube.ext.disconnectedCubeState
import com.blueprint.androidapp.ui.cube.mapper.ANIM_CUBE_STATE
import com.blueprint.cubing.core.model.CubeEvent
import com.blueprint.cubing.replay.model.PlayingState
import com.blueprint.cubing.replay.ui.ReplayViewModel
import com.catalinjurjiu.animcubeandroid.AnimCube
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReplayScreen(
    modifier: Modifier = Modifier,
    replayViewModel: ReplayViewModel = koinViewModel(),
) {
    val state by replayViewModel.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val context = LocalContext.current
    val activity = LocalActivity.current
    var cubeView by remember { mutableStateOf<AnimCube?>(null) }

    DisposableEffect(activity) {
        val window = activity?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    LaunchedEffect(state.solvePreviews, state.selectedSolvePreview) {
        if (state.selectedSolvePreview == null && state.solvePreviews.isNotEmpty()) {
            replayViewModel.handleAction(ReplayViewModel.Action.SelectSolve(state.solvePreviews.first()))
        }
    }

    LaunchedEffect(replayViewModel) {
        launch(Dispatchers.Main) {
            replayViewModel.cubeEvents
                .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
                .collect { event ->
                    when (event) {
                        is CubeEvent.CubeStateUpdated -> {
                            cubeView?.setCubeModel(event.arbitraryFormattedStates[ANIM_CUBE_STATE])
                        }

                        is CubeEvent.Move -> {
                            val speed = 1
                            cubeView?.setSingleRotationSpeed(speed)
                            cubeView?.setDoubleRotationSpeed(speed)
                            cubeView?.animateSequenceAsync(event.moveSequence)
                        }

                        is CubeEvent.Error -> {
                            Toast.makeText(
                                context,
                                event.displayMessage ?: event.throwable.message ?: "Replay error",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        else -> Unit
                    }
                }
        }
    }

    Scaffold(
        modifier = modifier
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(paddingValues)
                .padding(paddingValues)
        ) {
            val isPortrait = maxHeight >= maxWidth
            val content = @Composable {
                if (isPortrait) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ReplayCubePanel(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            selectedSolveName = state.selectedSolvePreview?.name ?: "No replay selected",
                            timeText = state.playingState.time.ifBlank { "00:00.00" },
                            selectedSolveEnabled = state.selectedSolvePreview != null,
                            playingStatus = state.playingState.status,
                            selectedSpeed = state.playingState.speed,
                            onPlay = { replayViewModel.handleAction(ReplayViewModel.Action.Play) },
                            onPause = { replayViewModel.handleAction(ReplayViewModel.Action.Pause) },
                            onStop = { replayViewModel.handleAction(ReplayViewModel.Action.Stop) },
                            onSpeedSelected = { speed ->
                                replayViewModel.handleAction(ReplayViewModel.Action.SetSpeed(speed))
                            },
                            onCubeViewReady = { cubeView = it },
                        )

                        ReplayListPanel(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            solvePreviews = state.solvePreviews,
                            selectedSolvePreviewId = state.selectedSolvePreview?.id,
                            isLoading = state.isLoading,
                            errorText = state.errorMessage,
                            onSelectSolve = { replayViewModel.handleAction(ReplayViewModel.Action.SelectSolve(it)) },
                            onDeleteSolve = { replayViewModel.handleAction(ReplayViewModel.Action.DeleteSolve(it)) },
                            onEditSolve = { replayViewModel.handleAction(ReplayViewModel.Action.EditSolve(it)) },
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ReplayCubePanel(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            selectedSolveName = state.selectedSolvePreview?.name ?: "No replay selected",
                            timeText = state.playingState.time.ifBlank { "00:00.00" },
                            selectedSolveEnabled = state.selectedSolvePreview != null,
                            playingStatus = state.playingState.status,
                            selectedSpeed = state.playingState.speed,
                            onPlay = { replayViewModel.handleAction(ReplayViewModel.Action.Play) },
                            onPause = { replayViewModel.handleAction(ReplayViewModel.Action.Pause) },
                            onStop = { replayViewModel.handleAction(ReplayViewModel.Action.Stop) },
                            onSpeedSelected = { speed ->
                                replayViewModel.handleAction(ReplayViewModel.Action.SetSpeed(speed))
                            },
                            onCubeViewReady = { cubeView = it },
                        )

                        ReplayListPanel(
                            modifier = Modifier
                                .width(280.dp)
                                .fillMaxHeight(),
                            solvePreviews = state.solvePreviews,
                            selectedSolvePreviewId = state.selectedSolvePreview?.id,
                            isLoading = state.isLoading,
                            errorText = state.errorMessage,
                            onSelectSolve = { replayViewModel.handleAction(ReplayViewModel.Action.SelectSolve(it)) },
                            onDeleteSolve = { replayViewModel.handleAction(ReplayViewModel.Action.DeleteSolve(it)) },
                            onEditSolve = { replayViewModel.handleAction(ReplayViewModel.Action.EditSolve(it)) },
                        )
                    }
                }
            }
            content()
        }
    }
}

@Composable
private fun ReplayCubePanel(
    modifier: Modifier = Modifier,
    selectedSolveName: String,
    timeText: String,
    selectedSolveEnabled: Boolean,
    playingStatus: PlayingState.Status,
    selectedSpeed: Float,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onSpeedSelected: (ReplayViewModel.Speed) -> Unit,
    onCubeViewReady: (AnimCube) -> Unit,
) {
    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val themedContext = ContextThemeWrapper(ctx, R.style.AnimCubeDark)
                AnimCube(themedContext).apply {
                    setDebuggable(true)
                }.also(onCubeViewReady)
            },
            update = { },
            onRelease = { view ->
                view.cleanUpResources()
            },
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = selectedSolveName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.padding(vertical = 4.dp))
            Text(
                text = timeText,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isPlaying = playingStatus == PlayingState.Status.PLAYING
                val playPauseAction = if (isPlaying) onPause else onPlay

                IconButton(
                    onClick = playPauseAction,
                    enabled = selectedSolveEnabled,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play"
                    )
                }

                IconButton(
                    onClick = onStop,
                    enabled = selectedSolveEnabled,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Stop,
                        contentDescription = "Stop"
                    )
                }

                var expanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.weight(1.8f)) {
                    OutlinedButton(
                        onClick = { expanded = true },
                        enabled = selectedSolveEnabled,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = when (selectedSpeed) {
                                ReplayViewModel.Speed.SLOW.multiplier -> "x0.5"
                                ReplayViewModel.Speed.FAST.multiplier -> "x2"
                                else -> "x1"
                            }
                        )
                    }

                    androidx.compose.material3.DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        ReplayViewModel.Speed.entries.forEach { speed ->
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("x${speed.multiplier}") },
                                onClick = {
                                    onSpeedSelected(speed)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReplayListPanel(
    modifier: Modifier = Modifier,
    solvePreviews: List<com.blueprint.cubing.replay.model.SolvePreview>,
    selectedSolvePreviewId: String?,
    isLoading: Boolean,
    errorText: String?,
    onSelectSolve: (com.blueprint.cubing.replay.model.SolvePreview) -> Unit,
    onDeleteSolve: (com.blueprint.cubing.replay.model.SolvePreview) -> Unit,
    onEditSolve: (com.blueprint.cubing.replay.model.SolvePreview) -> Unit,
) {
    var expandedItemId by remember { mutableStateOf<String?>(null) }
    var pendingDelete by remember { mutableStateOf<com.blueprint.cubing.replay.model.SolvePreview?>(null) }

    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete replay") },
            text = {
                Text(
                    text = "Are you sure you want to delete \"${pendingDelete!!.name}\"?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteSolve(pendingDelete!!)
                        pendingDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.98f)
        ),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Text(
                text = "Solve replays",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (isLoading) {
                Text(
                    text = "Loading...",
                    modifier = Modifier.padding(vertical = 12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (errorText != null) {
                Text(
                    text = errorText,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(solvePreviews, key = { it.id }) { solvePreview ->
                    val isSelected = selectedSolvePreviewId == solvePreview.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    Color.Transparent
                                },
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelectSolve(solvePreview) }
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = solvePreview.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                            if (solvePreview.note.isNotBlank()) {
                                Text(
                                    text = solvePreview.note,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Box {
                            IconButton(
                                onClick = {
                                    expandedItemId = if (expandedItemId == solvePreview.id) null else solvePreview.id
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MoreVert,
                                    contentDescription = "Replay options"
                                )
                            }

                            DropdownMenu(
                                expanded = expandedItemId == solvePreview.id,
                                onDismissRequest = { expandedItemId = null }
                            ) {
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.Edit,
                                            contentDescription = null
                                        )
                                    },
                                    text = { Text("Edit") },
                                    onClick = {
                                        onEditSolve(solvePreview)
                                        expandedItemId = null
                                    }
                                )
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = null
                                        )
                                    },
                                    text = { Text("Delete") },
                                    onClick = {
                                        pendingDelete = solvePreview
                                        expandedItemId = null
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
