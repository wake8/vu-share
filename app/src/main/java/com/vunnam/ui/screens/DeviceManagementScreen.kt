package com.vunnam.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vunnam.ui.MainViewModel
import com.vunnam.ui.components.vuFilterChipColors
import com.vunnam.ui.theme.VuShareTheme

@Composable
fun DeviceManagementScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = VuShareTheme.colors
    val userSettings by viewModel.userSettings.collectAsState()
    val discoveredDevices by viewModel.discoveredDevices.collectAsState()
    val history by viewModel.transferHistory.collectAsState()

    var selectedTab by remember { mutableStateOf("ALL") }

    val historyDevices = remember(history) {
        history.map { it.remoteDeviceName to it.remoteDeviceId }.distinctBy { it.second }
    }

    val favorites = userSettings.favoriteDeviceIds
    val blocked = userSettings.blockedDeviceIds

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
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
                    .testTag("device_mgmt_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = colors.primaryText
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Device Management",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.primaryText
                )
                Text(
                    text = "Manage trusted peers, favorites & blocked devices",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.secondaryText
                )
            }
        }

        // Scrollable Tabs
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val tabs = listOf(
                "ALL" to "Discovered (${discoveredDevices.size})",
                "FAVORITES" to "Favorites (${favorites.size})",
                "BLOCKED" to "Blocked (${blocked.size})",
                "RECENT" to "Recent (${historyDevices.size})"
            )
            items(tabs) { (tabKey, label) ->
                val isSelected = selectedTab == tabKey
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedTab = tabKey },
                    label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                    shape = RoundedCornerShape(12.dp),
                    colors = vuFilterChipColors(),
                    border = BorderStroke(1.dp, if (isSelected) colors.primaryAccent else colors.border)
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (selectedTab) {
                "ALL" -> {
                    if (discoveredDevices.isEmpty()) {
                        item { EmptyState(message = "No nearby devices currently online") }
                    } else {
                        items(discoveredDevices, key = { it.id }) { device ->
                            DeviceManagementItem(
                                name = device.nickname,
                                id = device.id,
                                emoji = device.avatarEmoji,
                                isFavorite = favorites.contains(device.id),
                                isBlocked = blocked.contains(device.id),
                                onToggleFav = { viewModel.toggleFavoriteDevice(device.id) },
                                onToggleBlock = { viewModel.toggleBlockDevice(device.id) }
                            )
                        }
                    }
                }

                "FAVORITES" -> {
                    if (favorites.isEmpty()) {
                        item { EmptyState(message = "No favorite devices saved yet") }
                    } else {
                        items(favorites.toList()) { id ->
                            val dev = discoveredDevices.find { it.id == id }
                            DeviceManagementItem(
                                name = dev?.nickname ?: "Device $id",
                                id = id,
                                emoji = dev?.avatarEmoji ?: "⭐",
                                isFavorite = true,
                                isBlocked = false,
                                onToggleFav = { viewModel.toggleFavoriteDevice(id) },
                                onToggleBlock = { viewModel.toggleBlockDevice(id) }
                            )
                        }
                    }
                }

                "BLOCKED" -> {
                    if (blocked.isEmpty()) {
                        item { EmptyState(message = "No blocked devices. Unwanted devices won't appear.") }
                    } else {
                        items(blocked.toList()) { id ->
                            val dev = discoveredDevices.find { it.id == id }
                            DeviceManagementItem(
                                name = dev?.nickname ?: "Blocked $id",
                                id = id,
                                emoji = "🚫",
                                isFavorite = false,
                                isBlocked = true,
                                onToggleFav = { viewModel.toggleFavoriteDevice(id) },
                                onToggleBlock = { viewModel.toggleBlockDevice(id) }
                            )
                        }
                    }
                }

                "RECENT" -> {
                    if (historyDevices.isEmpty()) {
                        item { EmptyState(message = "No past transfer devices") }
                    } else {
                        items(historyDevices) { (name, id) ->
                            DeviceManagementItem(
                                name = name,
                                id = id,
                                emoji = "📱",
                                isFavorite = favorites.contains(id),
                                isBlocked = blocked.contains(id),
                                onToggleFav = { viewModel.toggleFavoriteDevice(id) },
                                onToggleBlock = { viewModel.toggleBlockDevice(id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceManagementItem(
    name: String,
    id: String,
    emoji: String,
    isFavorite: Boolean,
    isBlocked: Boolean,
    onToggleFav: () -> Unit,
    onToggleBlock: () -> Unit
) {
    val colors = VuShareTheme.colors
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.primaryCard),
        border = BorderStroke(1.dp, colors.border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(colors.secondaryCard),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 22.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.primaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "ID: $id",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.secondaryText
                )
            }

            IconButton(
                onClick = onToggleFav,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) colors.favoriteRed else colors.secondaryText
                )
            }

            IconButton(
                onClick = onToggleBlock,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = if (isBlocked) Icons.Default.LockOpen else Icons.Default.Block,
                    contentDescription = if (isBlocked) "Unblock" else "Block",
                    tint = if (isBlocked) colors.primaryAccent else colors.error
                )
            }
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    val colors = VuShareTheme.colors
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.secondaryCard),
        border = BorderStroke(1.dp, colors.border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Devices,
                contentDescription = null,
                tint = colors.secondaryText.copy(alpha = 0.5f),
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.secondaryText
            )
        }
    }
}
