package com.vunnam.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vunnam.ui.MainViewModel
import com.vunnam.ui.components.DeviceCard
import com.vunnam.ui.components.VuRadarAnimation
import com.vunnam.ui.theme.VuShareTheme

@Composable
fun DiscoveryScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigateToQrScan: () -> Unit,
    onTransferStarted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = VuShareTheme.colors
    val discoveredDevices by viewModel.discoveredDevices.collectAsState()
    val selectedDevices by viewModel.selectedTargetDevices.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val selectedFiles by viewModel.selectedFiles.collectAsState()

    DisposableEffect(Unit) {
        viewModel.startDiscovery()
        onDispose {
            viewModel.stopDiscovery()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("discovery_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = colors.primaryText
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Nearby Devices",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.primaryText
                )
                Text(
                    text = "Searching for VU Share peers...",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.primaryAccent,
                    fontWeight = FontWeight.Medium
                )
            }

            IconButton(
                onClick = onNavigateToQrScan,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("scan_qr_button")
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "Scan QR",
                    tint = colors.primaryAccent
                )
            }

            IconButton(
                onClick = {
                    viewModel.stopDiscovery()
                    viewModel.startDiscovery()
                },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("refresh_discovery_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = colors.secondaryText
                )
            }
        }

        // Radar section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            contentAlignment = Alignment.Center
        ) {
            VuRadarAnimation(size = 200.dp) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(colors.primaryCard)
                        .border(2.dp, colors.primaryAccent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = userSettings.avatarEmoji, fontSize = 28.sp)
                }
            }
        }

        // Devices List Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Discovered (${discoveredDevices.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = colors.primaryText
            )

            Text(
                text = "Tap to select",
                style = MaterialTheme.typography.bodySmall,
                color = colors.secondaryText
            )
        }

        if (discoveredDevices.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Scanning nearby devices...",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.primaryText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Ensure the other phone has opened 'RECEIVE' or is on the same Wi-Fi / Hotspot network",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.secondaryText,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(discoveredDevices, key = { it.id }) { device ->
                    val isSelected = selectedDevices.any { it.id == device.id }
                    DeviceCard(
                        device = device,
                        isSelected = isSelected,
                        onSelectToggle = { viewModel.toggleDeviceSelection(device) },
                        onFavoriteToggle = { viewModel.toggleFavoriteDevice(device.id) }
                    )
                }
            }
        }

        // Bottom Send Action Button
        AnimatedVisibility(visible = selectedDevices.isNotEmpty()) {
            Surface(
                color = colors.elevatedSurface,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                border = BorderStroke(1.dp, colors.border)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Send to ${selectedDevices.size} device(s)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.primaryText
                        )
                        Text(
                            text = "${selectedFiles.size} file(s) queued",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.primaryAccent,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.sendSelectedFilesToDevices(selectedDevices)
                            onTransferStarted()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primaryAccent,
                            contentColor = colors.onPrimaryAccent
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("start_transfer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Send Now",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
