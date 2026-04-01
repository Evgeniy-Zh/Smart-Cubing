package com.blueprint.cubing.device.search

import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier

@Composable
fun DeviceDetailsScreen(
    viewModel: DeviceDetailsViewModel,
) {
    Scaffold { paddingValues ->
        val state by viewModel.state.collectAsState()
        LazyColumn(
            modifier = Modifier.padding(paddingValues),
        ) {
            item {
                BtDeviceDetails(
                    modifier = Modifier,
                    state = state,
                    onAction = viewModel::handleAction
                )
            }
        }

    }
}