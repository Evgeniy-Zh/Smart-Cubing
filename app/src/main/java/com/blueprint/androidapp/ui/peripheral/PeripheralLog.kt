package com.blueprint.androidapp.ui.peripheral

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel

@Composable
fun BleTest(modifier: Modifier, viewModel: PeripheralLogViewModel = koinViewModel()) {
    val context = LocalContext.current

    Column(modifier = modifier) {
        val log by viewModel.log.collectAsState()

        Row(modifier = Modifier, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(modifier = Modifier, onClick = { viewModel.startService() }) {
                Text(text = "Start Peripheral")
            }
            Button(modifier = Modifier, onClick = { viewModel.advertiseConnectedDevices() }) {
                Text(text = "Advertise devices")
            }
        }

        Text(modifier = Modifier.padding(12.dp).fillMaxWidth(), text = log.joinToString (separator = "\n"))

    }
}