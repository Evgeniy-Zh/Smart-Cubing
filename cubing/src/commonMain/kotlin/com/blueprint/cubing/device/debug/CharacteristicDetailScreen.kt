@file:OptIn(ExperimentalMaterial3Api::class)

package com.blueprint.cubing.device.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CharacteristicDetailScreen(
    onBack: () -> Unit = {},
    viewModel: CharacteristicDetailViewModel,
) {

    val state by viewModel.state.collectAsState()


    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "Characteristic") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        CharacteristicDetailContent(
            state = state,
            onAction = { viewModel.handleAction(it) },
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
private fun CharacteristicDetailContent(
    state: CharacteristicDetailViewModel.State,
    onAction: (CharacteristicDetailViewModel.Action) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Characteristic",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "UUID:",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = state.uuid,
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Properties",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.properties.forEach { prop ->
                PropertyChip(text = prop)
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Button(onClick = { onAction(CharacteristicDetailViewModel.Action.ToggleObserve) }) {
                Text(text = if (state.observing) "Stop observing" else "Observe")
            }
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(onClick = { onAction(CharacteristicDetailViewModel.Action.ClearLog) }) {
                Text(text = "Clear")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Values",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            val listState = rememberLazyListState()

            // Always scroll to the latest item after values update
            LaunchedEffect(state.values.size) {
                if (state.values.isNotEmpty()) {
                    listState.scrollToItem(state.values.lastIndex)
                }
            }

            if (state.values.isEmpty()) {
                Text(
                    text = "No values yet",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(state = listState, modifier = Modifier.fillMaxWidth()) {
                    items(state.values) { value ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = value,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PropertyChip(text: String) {
    val bg = MaterialTheme.colorScheme.primaryContainer
    val fg = MaterialTheme.colorScheme.onPrimaryContainer
    Box(
        modifier = Modifier
            .background(color = bg, shape = RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text = text, color = fg, fontWeight = FontWeight.SemiBold)
    }
}

//@Preview(showBackground = true)
@Composable
private fun CharacteristicDetailPreview() {
    val mockState = CharacteristicDetailViewModel.State(
        uuid = "12345678-1234-1234-1234-1234567890ab",
        properties = listOf("READ", "WRITE", "NOTIFY"),
        values = listOf("0x01 0x02", "0x0A 0xFF"),
        observing = true
    )
    Surface {
        Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text("Characteristic") }) }) { inner ->
            CharacteristicDetailContent(state = mockState, onAction = {}, modifier = Modifier.padding(inner))
        }
    }
}