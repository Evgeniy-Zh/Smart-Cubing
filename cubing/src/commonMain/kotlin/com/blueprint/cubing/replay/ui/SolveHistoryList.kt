package com.blueprint.cubing.replay.ui

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.blueprint.cubing.replay.model.SolvePreview

@Composable
fun SolveHistoryList(
    modifier: Modifier = Modifier,
    solvePreviews: List<SolvePreview>,
) {

    val listState = rememberLazyListState()
    LaunchedEffect(solvePreviews) {
        listState.scrollToItem(0) // Scroll to the top of the list
    }
    LazyColumn(modifier = modifier, state = listState) {
        items(solvePreviews, key = { it.id }) { solvePreview ->
            SolveHistoryItem(solvePreview)
        }
    }
}

@Composable
fun SolveHistoryItem(solvePreview: SolvePreview) {
    Text(text = solvePreview.name)
}
