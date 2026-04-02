package com.blueprint.cubing.device.search

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.blueprint.cubing.device.search.SearchDeviceViewModel.Action.OpenDeviceDetails

@Composable
fun SearchDeviceScreen(
    viewModel: SearchDeviceViewModel,
) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        val devices by viewModel.devices.collectAsState()
        BtDeviceList(
            modifier = Modifier.padding(innerPadding),
            list = devices,
            onItemClick = { viewModel.handleAction(OpenDeviceDetails(device = it)) },
        )
    }
}