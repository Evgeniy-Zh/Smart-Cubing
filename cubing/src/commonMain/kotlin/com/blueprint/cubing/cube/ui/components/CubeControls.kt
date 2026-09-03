package com.blueprint.cubing.cube.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.blueprint.cubing.cube.ui.CubeViewModel
import com.blueprint.cubing.core.model.ConnectionState
import com.blueprint.cubing.core.model.CubeDevice

/**
 * Row of controls that combines the device indicator (dropdown) with connection status,
 * search, reset and disconnect buttons.
 */
@Composable
fun CubeControls(
    modifier: Modifier = Modifier,
    connectionState: ConnectionState,
    cubeList: List<CubeDevice>,
    activeDevice: CubeDevice?,
    onAction: (CubeViewModel.Action) -> Unit,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        // compute status color to show inside label
        val statusColor = when (connectionState) {
            is ConnectionState.Connected -> Color(0xFF4CAF50) // green
            ConnectionState.Connecting -> Color(0xFFFFC107) // amber
            ConnectionState.Disconnecting -> Color(0xFFFFC107)
            ConnectionState.Disconnected -> Color(0xFF9E9E9E) // gray
            ConnectionState.FailedToConnect -> Color(0xFFF44336) // red
            ConnectionState.Initializing -> Color(0xFF83DC8C)
        }

        // Device label dropdown (shows only device label as the dropdown button) with status dot inside
        val expanded = remember { mutableStateOf(false) }

        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xAA111111)),
            modifier = Modifier
                .width(160.dp)
                .clickable { expanded.value = true }
                .padding(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = activeDevice?.name ?: "No Device",
                    color = Color.White
                )
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(color = statusColor, shape = CircleShape)
                )
            }
        }

        DropdownMenu(
            containerColor = Color(0xCC111111),
            expanded = expanded.value,
            onDismissRequest = { expanded.value = false }
        ) {
            if (cubeList.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("No Devices", color = Color.White) },
                    onClick = { expanded.value = false }
                )
            } else {
                cubeList.forEach { device ->
                    DropdownMenuItem(
                        text = { Text(device.name, color = Color.White) },
                        onClick = {
                            onAction(CubeViewModel.Action.SelectDevice(device))
                            expanded.value = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Search button
        IconButton(onClick = { onAction(CubeViewModel.Action.SearchDevices) }) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search Devices"
            )
        }

        // If a device is connected, show Reset and Disconnect buttons
        if (activeDevice != null && connectionState is ConnectionState.Connected) {
            IconButton(onClick = { onAction(CubeViewModel.Action.Reset) }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset"
                )
            }

            IconButton(onClick = { onAction(CubeViewModel.Action.Disconnect) }) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Disconnect"
                )
            }
        }
    }
}
