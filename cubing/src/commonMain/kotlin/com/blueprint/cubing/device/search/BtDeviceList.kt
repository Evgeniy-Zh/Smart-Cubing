@file:OptIn(ExperimentalUuidApi::class)

package com.blueprint.cubing.device.search

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blueprint.bleapi.model.BtDevice
import com.blueprint.bleapi.model.DeviceCharacteristic
import com.blueprint.bleapi.model.Property
import kotlinx.collections.immutable.ImmutableList
import kotlin.uuid.ExperimentalUuidApi


@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalFoundationApi::class)
@Composable
fun BtDeviceList(
    modifier: Modifier,
    list: ImmutableList<SearchDeviceViewModel.ListItem>,
    onItemClick: (BtDevice) -> Unit,
    onDisconnect: (BtDevice) -> Unit = {}
) {

    LazyColumn(
        modifier = modifier,
    ) {

        items(
            count = list.size,
            key = { i -> list[i].key },
            contentType = { i -> list[i]::class.simpleName }
        ) { i ->
            when (val item = list[i]) {
                is SearchDeviceViewModel.ListItem.Device -> {
                    val device = item.btDevice
                    BtDeviceItem(
                        modifier = Modifier.fillMaxWidth(),
                        data = device,
                        onClick = { onItemClick(device) },
                    )

                }
            }
        }
    }

}

@Composable
fun BtDeviceItem(
    modifier: Modifier,
    data: BtDevice,
    onClick: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onClick)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = data.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = data.address,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "Expand",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

    }
}

@Composable
fun BtDeviceDetails(
    modifier: Modifier = Modifier,
    state: DeviceDetailsViewModel.State,
    onAction: (DeviceDetailsViewModel.Action) -> Unit,
) {

    if (state.loading) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Loading data...", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }


    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {

        if (!state.loading) {

            Text(text = state.device?.name ?: "")
            Text(text = state.device?.address ?: "")

            Row(
                modifier = Modifier.padding(vertical = 8.dp).height(45.dp)
            ) {
                if (state.recognizedCubeDevice != null) {
                    Button(
                        modifier = Modifier.padding(2.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(),
                        enabled = !state.saved,
                        onClick = { onAction(DeviceDetailsViewModel.Action.SaveDevice()) }
                    ) {
                        Text(text = if(state.saved) "Saved" else "Save")
                    }
                }
                Button(
                    modifier = Modifier.padding(2.dp),
                    colors = ButtonDefaults.outlinedButtonColors(),
                    onClick = { onAction(DeviceDetailsViewModel.Action.Disconnect) }
                ) {
                    Text(text = "Disconnect")
                }
            }
        }
        state.list.forEachIndexed { serviceIndex, service ->
            // Service Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = "Service ${serviceIndex + 1}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = service.uuid.toString(),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )

                // Characteristics
                if (service.characteristics.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp)
                    ) {
                        service.characteristics.forEachIndexed { charIndex, characteristic ->
                            CharacteristicRow(
                                characteristic = characteristic,
                                index = charIndex + 1,
                                isLast = charIndex == service.characteristics.size - 1,
                                onClick = {
                                    onAction(
                                        DeviceDetailsViewModel.Action.OpenCharacteristicDetails(
                                            characteristic
                                        )
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // Service Separator
            if (serviceIndex < state.list.size - 1) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.outlineVariant,
                    thickness = 1.dp
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

    }
}

@Composable
fun CharacteristicRow(
    characteristic: DeviceCharacteristic,
    index: Int,
    isLast: Boolean = false,
    onClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 8.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Characteristic $index",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = characteristic.uuid.toString(),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                PropertiesRow(properties = characteristic.properties)
            }
        }

        if (!isLast) {
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(start = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 0.5.dp
            )
        }
    }
}

@Composable
fun PropertiesRow(properties: List<Property>) {
    if (properties.isEmpty()) {
        return
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        properties.forEach { property ->
            PropertyBadge(property = property)
        }
    }
}

@Composable
fun PropertyBadge(property: Property) {
    val (backgroundColor, textColor) = when (property) {
        Property.READ -> {
            MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        }

        Property.WRITE -> {
            MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        }

        Property.NOTIFY -> {
            MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        }

        Property.INDICATE -> {
            MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        }
    }

    Box(
        modifier = Modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Text(
            text = property.name,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}
