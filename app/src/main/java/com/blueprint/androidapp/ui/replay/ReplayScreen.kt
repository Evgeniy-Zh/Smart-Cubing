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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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

            if (isPortrait) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { ctx ->
                                val themedContext = ContextThemeWrapper(ctx, R.style.AnimCubeDark)
                                AnimCube(themedContext).apply {
                                    setDebuggable(true)
                                    setSingleRotationSpeed(3)
                                    setDoubleRotationSpeed(3)
                                    if (state.selectedSolvePreview == null) {
                                        setCubeModel(disconnectedCubeState)
                                    }
                                }.also { cubeView = it }
                            },
                            update = { },
                            onRelease = { view ->
                                view.cleanUpResources()
                            },
                        )

                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = state.selectedSolvePreview?.name ?: "No replay selected",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.padding(vertical = 4.dp))
                            Text(
                                text = state.playingState.time.ifBlank { "00:00.00" },
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
                                val selectedSolveEnabled = state.selectedSolvePreview != null
                                OutlinedButton(
                                    onClick = { replayViewModel.handleAction(ReplayViewModel.Action.Play) },
                                    enabled = selectedSolveEnabled && state.playingState.status != PlayingState.Status.PLAYING,
                                ) { Text("Play") }
                                OutlinedButton(
                                    onClick = { replayViewModel.handleAction(ReplayViewModel.Action.Pause) },
                                    enabled = selectedSolveEnabled && state.playingState.status == PlayingState.Status.PLAYING,
                                ) { Text("Pause") }
                                OutlinedButton(
                                    onClick = { replayViewModel.handleAction(ReplayViewModel.Action.Stop) },
                                    enabled = selectedSolveEnabled,
                                ) { Text("Stop") }
                            }

                            Row(
                                modifier = Modifier.padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ReplayViewModel.Speed.entries.forEach { speed ->
                                    val selected = state.playingState.speed == speed.multiplier
                                    TextButton(
                                        onClick = {
                                            replayViewModel.handleAction(ReplayViewModel.Action.SetSpeed(speed))
                                        },
                                        colors = if (selected) {
                                            ButtonDefaults.textButtonColors(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        } else {
                                            ButtonDefaults.textButtonColors()
                                        }
                                    ) {
                                        Text(speed.name.lowercase().replaceFirstChar { it.titlecase() })
                                    }
                                }
                            }
                        }
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
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

                            if (state.isLoading) {
                                Text(
                                    text = "Loading...",
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            val errorText = state.errorMessage
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
                                items(state.solvePreviews, key = { it.id }) { solvePreview ->
                                    val isSelected = state.selectedSolvePreview?.id == solvePreview.id
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
                                            .clickable {
                                                replayViewModel.handleAction(
                                                    ReplayViewModel.Action.SelectSolve(solvePreview)
                                                )
                                            }
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
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

                                        IconButton(
                                            onClick = {
                                                replayViewModel.handleAction(
                                                    ReplayViewModel.Action.DeleteSolve(solvePreview)
                                                )
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete replay"
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { ctx ->
                                val themedContext = ContextThemeWrapper(ctx, R.style.AnimCubeDark)
                                AnimCube(themedContext).apply {
                                    setDebuggable(true)
                                    setSingleRotationSpeed(3)
                                    setDoubleRotationSpeed(3)
                                    if (state.selectedSolvePreview == null) {
                                        setCubeModel(disconnectedCubeState)
                                    }
                                }.also { cubeView = it }
                            },
                            update = { },
                            onRelease = { view ->
                                view.cleanUpResources()
                            },
                        )

                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = state.selectedSolvePreview?.name ?: "No replay selected",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.padding(vertical = 4.dp))
                            Text(
                                text = state.playingState.time.ifBlank { "00:00.00" },
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
                                val selectedSolveEnabled = state.selectedSolvePreview != null
                                OutlinedButton(
                                    onClick = { replayViewModel.handleAction(ReplayViewModel.Action.Play) },
                                    enabled = selectedSolveEnabled && state.playingState.status != PlayingState.Status.PLAYING,
                                ) { Text("Play") }
                                OutlinedButton(
                                    onClick = { replayViewModel.handleAction(ReplayViewModel.Action.Pause) },
                                    enabled = selectedSolveEnabled && state.playingState.status == PlayingState.Status.PLAYING,
                                ) { Text("Pause") }
                                OutlinedButton(
                                    onClick = { replayViewModel.handleAction(ReplayViewModel.Action.Stop) },
                                    enabled = selectedSolveEnabled,
                                ) { Text("Stop") }
                            }

                            Row(
                                modifier = Modifier.padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ReplayViewModel.Speed.entries.forEach { speed ->
                                    val selected = state.playingState.speed == speed.multiplier
                                    TextButton(
                                        onClick = {
                                            replayViewModel.handleAction(ReplayViewModel.Action.SetSpeed(speed))
                                        },
                                        colors = if (selected) {
                                            ButtonDefaults.textButtonColors(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        } else {
                                            ButtonDefaults.textButtonColors()
                                        }
                                    ) {
                                        Text(speed.name.lowercase().replaceFirstChar { it.titlecase() })
                                    }
                                }
                            }
                        }
                    }

                    Card(
                        modifier = Modifier
                            .width(280.dp)
                            .fillMaxHeight(),
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

                            if (state.isLoading) {
                                Text(
                                    text = "Loading...",
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            val errorText = state.errorMessage
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
                                items(state.solvePreviews, key = { it.id }) { solvePreview ->
                                    val isSelected = state.selectedSolvePreview?.id == solvePreview.id
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
                                            .clickable {
                                                replayViewModel.handleAction(
                                                    ReplayViewModel.Action.SelectSolve(solvePreview)
                                                )
                                            }
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
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

                                        IconButton(
                                            onClick = {
                                                replayViewModel.handleAction(
                                                    ReplayViewModel.Action.DeleteSolve(solvePreview)
                                                )
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete replay"
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
