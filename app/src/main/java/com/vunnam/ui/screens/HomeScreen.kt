package com.vunnam.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vunnam.model.TransferDirection
import com.vunnam.model.TransferStatus
import com.vunnam.network.NetworkUtils
import com.vunnam.ui.MainViewModel
import com.vunnam.ui.components.vuSwitchColors
import com.vunnam.ui.theme.VuShareTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToSend: () -> Unit,
    onNavigateToReceive: () -> Unit,
    onNavigateToGroupShare: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToQrScan: () -> Unit,
    onNavigateToMigration: () -> Unit,
    onNavigateToDeviceManagement: () -> Unit,
    onOpenClipboardShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = VuShareTheme.colors
    val userSettings by viewModel.userSettings.collectAsState()
    val recentTransfers by viewModel.recentTransfers.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()
    val localIp = NetworkUtils.getLocalIpAddress() ?: "Offline / Hotspot"
    var showCustomizeSheet by remember { mutableStateOf(false) }

    val isCompact = userSettings.compactCardsMode
    val buttonHeight = if (isCompact) 100.dp else 126.dp

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(if (isCompact) 12.dp else 16.dp)
    ) {
        // -----------------------------------------------------------
        // Top App Bar: Title + Tagline (left) & Actions (right)
        // -----------------------------------------------------------
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "VU Share",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.primaryText
                    )
                    Text(
                        text = "Share Freely. Stay Connected.",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.secondaryText
                    )
                }

                // Actions: History, Customize, Device Mgmt, Settings
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onNavigateToHistory,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("home_history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "View All History",
                            tint = colors.secondaryText
                        )
                    }

                    IconButton(
                        onClick = { showCustomizeSheet = true },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("customize_home_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Customize Home",
                            tint = colors.secondaryText
                        )
                    }

                    IconButton(
                        onClick = onNavigateToDeviceManagement,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("device_mgmt_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Devices,
                            contentDescription = "Device Management",
                            tint = colors.secondaryText
                        )
                    }

                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = colors.secondaryText
                        )
                    }
                }
            }
        }

        // -----------------------------------------------------------
        // User Profile Card: Avatar, Funny Nickname & Roll Dice
        // -----------------------------------------------------------
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = colors.primaryCard),
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(if (isCompact) 12.dp else 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (isCompact) 44.dp else 52.dp)
                            .clip(CircleShape)
                            .background(colors.secondaryCard)
                            .border(2.dp, colors.primaryAccent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = userSettings.avatarEmoji, fontSize = if (isCompact) 24.sp else 28.sp)
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = userSettings.nickname,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.primaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "IP: $localIp",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.primaryAccent,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    IconButton(
                        onClick = { viewModel.randomizeNicknameAndAvatar() },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("dice_roll_button")
                            .clip(CircleShape)
                            .background(colors.secondaryCard)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Casino,
                            contentDescription = "Randomize Nickname",
                            tint = colors.primaryAccent
                        )
                    }
                }
            }
        }

        // -----------------------------------------------------------
        // Active Transfer Card (If enabled and in progress)
        // -----------------------------------------------------------
        val currentActive = activeSession
        if (userSettings.showActiveTransfersOnHome &&
            currentActive != null &&
            (currentActive.status == TransferStatus.TRANSFERRING ||
                currentActive.status == TransferStatus.CONNECTING ||
                currentActive.status == TransferStatus.PAUSED)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.selectedSurface),
                    border = BorderStroke(1.5.dp, colors.primaryAccent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (currentActive.isPaused) "Transfer Paused" else "Active Transfer",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = colors.primaryAccent
                            )
                            Text(
                                text = "${(currentActive.overallProgress * 100).toInt()}% • ${NetworkUtils.formatSpeed(currentActive.speedBps)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.primaryText,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { currentActive.overallProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = colors.progressIndicator,
                            trackColor = colors.progressTrack,
                            strokeCap = StrokeCap.Round
                        )
                    }
                }
            }
        }

        // -----------------------------------------------------------
        // Primary Action Buttons: SEND & RECEIVE
        // -----------------------------------------------------------
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // SEND BUTTON
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(buttonHeight)
                        .testTag("home_send_button")
                        .clickable { onNavigateToSend() },
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.sendCardBg),
                    border = BorderStroke(1.5.dp, colors.sendCardBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(if (isCompact) 12.dp else 16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (isCompact) 36.dp else 42.dp)
                                .clip(CircleShape)
                                .background(colors.sendCardIconBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Upload,
                                contentDescription = "Send",
                                tint = colors.sendCardIconTint,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "SEND",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = colors.sendCardTitle
                            )
                            if (!isCompact) {
                                Text(
                                    text = "Share files with peers",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.sendCardSubtitle
                                )
                            }
                        }
                    }
                }

                // RECEIVE BUTTON
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(buttonHeight)
                        .testTag("home_receive_button")
                        .clickable { onNavigateToReceive() },
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.receiveCardBg),
                    border = BorderStroke(1.5.dp, colors.receiveCardBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(if (isCompact) 12.dp else 16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (isCompact) 36.dp else 42.dp)
                                .clip(CircleShape)
                                .background(colors.receiveCardIconBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Receive",
                                tint = colors.receiveCardIconTint,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "RECEIVE",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = colors.receiveCardTitle
                            )
                            if (!isCompact) {
                                Text(
                                    text = "Wait for incoming files",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.receiveCardSubtitle
                                )
                            }
                        }
                    }
                }
            }
        }

        // -----------------------------------------------------------
        // Group Share Hero Banner
        // -----------------------------------------------------------
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_group_share_button")
                    .clickable { onNavigateToGroupShare() },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = colors.primaryCard),
                border = BorderStroke(1.dp, colors.border)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(if (isCompact) 12.dp else 15.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        colors.primaryAccent.copy(alpha = 0.18f),
                                        colors.secondaryHue.copy(alpha = 0.18f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = "Group Share",
                            tint = colors.primaryAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Group Broadcast Share",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.primaryText
                        )
                        Text(
                            text = "Send files to multiple devices at once",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.secondaryText
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = colors.secondaryText,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // -----------------------------------------------------------
        // Favorite Devices Section
        // -----------------------------------------------------------
        if (userSettings.showFavoriteDevicesOnHome && userSettings.favoriteDeviceIds.isNotEmpty()) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Favorite Devices",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.primaryText
                        )
                        Text(
                            text = "Manage",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.primaryAccent,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clickable { onNavigateToDeviceManagement() }
                                .padding(4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(userSettings.favoriteDeviceIds.toList()) { id ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = colors.primaryCard),
                                border = BorderStroke(1.dp, colors.border),
                                modifier = Modifier.clickable { onNavigateToSend() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = null,
                                        tint = colors.favoriteRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = id.take(12),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.primaryText
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // -----------------------------------------------------------
        // Quick Utility Actions Grid (Scan QR, Phone Migration, Clipboard)
        // -----------------------------------------------------------
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToQrScan() }
                        .testTag("home_qr_scan_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.primaryCard),
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scan QR",
                            tint = colors.primaryAccent,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Scan QR",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.primaryText
                        )
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToMigration() }
                        .testTag("home_migration_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.primaryCard),
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = "Phone Clone",
                            tint = colors.secondaryHue,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Migration",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.primaryText
                        )
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOpenClipboardShare() }
                        .testTag("home_clipboard_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.primaryCard),
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = "Share Text",
                            tint = colors.success,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Clipboard",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.primaryText
                        )
                    }
                }
            }
        }

        // -----------------------------------------------------------
        // Recent Transfers Header & List (completely removed when hidden)
        // -----------------------------------------------------------
        if (!userSettings.hideRecentTransfersOnHome && userSettings.showRecentSharesOnHome) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Transfers",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.primaryText
                    )

                    Text(
                        text = "View All",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.primaryAccent,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable { onNavigateToHistory() }
                            .padding(4.dp)
                    )
                }
            }

            if (recentTransfers.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.secondaryCard),
                        border = BorderStroke(1.dp, colors.border)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = colors.secondaryText.copy(alpha = 0.6f),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "No recent transfers",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.secondaryText
                            )
                        }
                    }
                }
            } else {
                items(recentTransfers) { record ->
                    val isSend = record.direction == TransferDirection.SEND
                    val dirTint = if (isSend) colors.primaryAccent else colors.secondaryHue
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                record.filePath?.let { path ->
                                    viewModel.openReceivedFile(path)
                                }
                            },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.primaryCard),
                        border = BorderStroke(1.dp, colors.border)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(dirTint.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isSend) Icons.Default.Upload else Icons.Default.Download,
                                    contentDescription = null,
                                    tint = dirTint,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = record.fileName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.primaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${NetworkUtils.formatFileSize(record.fileSize)} • ${record.remoteDeviceName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.secondaryText
                                )
                            }

                            if (record.sha256Checksum != null) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = "Verified Checksum",
                                    tint = colors.success,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Dashboard Customization Bottom Sheet
    if (showCustomizeSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showCustomizeSheet = false },
            sheetState = sheetState,
            containerColor = colors.elevatedSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Customize Dashboard",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.primaryText
                    )
                    TextButton(onClick = { viewModel.resetDashboardLayout() }) {
                        Icon(
                            imageVector = Icons.Default.Restore,
                            contentDescription = null,
                            tint = colors.primaryAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset", color = colors.primaryAccent, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                DashboardToggleItem(
                    title = "Completely Hide Recent Transfers",
                    subtitle = "Removes Recent Transfers from homepage while keeping View All History",
                    checked = userSettings.hideRecentTransfersOnHome,
                    onCheckedChange = { viewModel.updateHideRecentTransfersOnHome(it) }
                )

                DashboardToggleItem(
                    title = "Recent Shares",
                    subtitle = if (userSettings.hideRecentTransfersOnHome) {
                        "Overridden by 'Completely Hide Recent Transfers on Homepage'"
                    } else {
                        "Show latest file transfers on home screen"
                    },
                    checked = userSettings.showRecentSharesOnHome && !userSettings.hideRecentTransfersOnHome,
                    onCheckedChange = { enabled ->
                        if (enabled && userSettings.hideRecentTransfersOnHome) {
                            viewModel.updateHideRecentTransfersOnHome(false)
                        }
                        viewModel.updateDashboardToggles(
                            showRecent = enabled,
                            showRadar = userSettings.showNearbyRadarOnHome,
                            showFavs = userSettings.showFavoriteDevicesOnHome,
                            showActive = userSettings.showActiveTransfersOnHome,
                            compact = userSettings.compactCardsMode
                        )
                    }
                )

                DashboardToggleItem(
                    title = "Favorite Devices",
                    subtitle = "Display quick access row for pinned peers",
                    checked = userSettings.showFavoriteDevicesOnHome,
                    onCheckedChange = {
                        viewModel.updateDashboardToggles(
                            showRecent = userSettings.showRecentSharesOnHome,
                            showRadar = userSettings.showNearbyRadarOnHome,
                            showFavs = it,
                            showActive = userSettings.showActiveTransfersOnHome,
                            compact = userSettings.compactCardsMode
                        )
                    }
                )

                DashboardToggleItem(
                    title = "Active Transfers",
                    subtitle = "Show live progress banner on home screen",
                    checked = userSettings.showActiveTransfersOnHome,
                    onCheckedChange = {
                        viewModel.updateDashboardToggles(
                            showRecent = userSettings.showRecentSharesOnHome,
                            showRadar = userSettings.showNearbyRadarOnHome,
                            showFavs = userSettings.showFavoriteDevicesOnHome,
                            showActive = it,
                            compact = userSettings.compactCardsMode
                        )
                    }
                )

                DashboardToggleItem(
                    title = "Compact Cards Mode",
                    subtitle = "Use smaller density for Send/Receive buttons",
                    checked = userSettings.compactCardsMode,
                    onCheckedChange = {
                        viewModel.updateDashboardToggles(
                            showRecent = userSettings.showRecentSharesOnHome,
                            showRadar = userSettings.showNearbyRadarOnHome,
                            showFavs = userSettings.showFavoriteDevicesOnHome,
                            showActive = userSettings.showActiveTransfersOnHome,
                            compact = it
                        )
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun DashboardToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = VuShareTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.primaryText
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = colors.secondaryText
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = vuSwitchColors()
        )
    }
}
