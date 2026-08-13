package com.blueprint.cubing.replay.ui

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.blueprint.cubing.replay.model.Replay

@Composable
fun SolveHistoryList(
    modifier: Modifier = Modifier,
    replays: List<Replay>,
) {

    val listState = rememberLazyListState()
    LaunchedEffect(replays) {
        listState.scrollToItem(0) // Scroll to the top of the list
    }
    LazyColumn(modifier = modifier, state = listState) {
        items(replays, key = { it.id }) { replay ->
            SolveHistoryItem(replay)
        }
    }
}

@Composable
fun SolveHistoryItem(replay: Replay) {
    Text(text = replay.name)
}
