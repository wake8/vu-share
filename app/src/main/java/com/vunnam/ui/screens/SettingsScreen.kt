package com.vunnam.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.vunnam.R
import com.vunnam.config.AppBrandConfig
import com.vunnam.preferences.DeviceIdentityManager
import com.vunnam.ui.MainViewModel
import com.vunnam.ui.components.NotificationPermissionHelper
import com.vunnam.ui.components.vuFilterChipColors
import com.vunnam.ui.components.vuSwitchColors
import com.vunnam.ui.components.vuTextFieldColors
import com.vunnam.ui.theme.VuShareTheme
import com.vunnam.ui.theme.buildVuColors
import com.vunnam.ui.theme.getAccentColor

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigateToHistory: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = VuShareTheme.colors
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val appMeta = remember(context) { AppBrandConfig.resolveAppMetadata(context) }
    val systemIsDark = isSystemInDarkTheme()
    val userSettings by viewModel.userSettings.collectAsState()
    var editingNickname by remember(userSettings.nickname) { mutableStateOf(userSettings.nickname) }
    var isEditing by remember { mutableStateOf(false) }
    var showRealCrashConfirmDialog by remember { mutableStateOf(false) }
    var notificationsGranted by remember {
        mutableStateOf(NotificationPermissionHelper.isNotificationPermissionGranted(context))
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationsGranted = NotificationPermissionHelper.isNotificationPermissionGranted(context)
        if (!granted && NotificationPermissionHelper.isPermanentlyDenied(context)) {
            NotificationPermissionHelper.openAppNotificationSettings(context)
        }
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsGranted = NotificationPermissionHelper.isNotificationPermissionGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // App Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("settings_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.primaryText
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Settings & Identity",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.primaryText
                )
            }
        }

        // Section: Device Profile & Identity
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = colors.primaryCard),
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Text(
                        text = "Device Avatar & Nickname",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.primaryText
                    )
                    Text(
                        text = "Your identity is randomly generated and never exposes your real phone model or personal info.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.secondaryText,
                        modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = editingNickname,
                            onValueChange = {
                                editingNickname = it
                                isEditing = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("nickname_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = vuTextFieldColors()
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        if (isEditing) {
                            IconButton(
                                onClick = {
                                    if (editingNickname.isNotBlank()) {
                                        viewModel.updateNickname(editingNickname.trim())
                                        isEditing = false
                                    }
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Save",
                                    tint = colors.primaryAccent
                                )
                            }
                        }

                        IconButton(
                            onClick = { viewModel.randomizeNicknameAndAvatar() },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("settings_dice_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Casino,
                                contentDescription = "Randomize",
                                tint = colors.secondaryHue
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Choose Avatar",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.primaryText
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(DeviceIdentityManager.AVATAR_OPTIONS) { opt ->
                            val isSelected = userSettings.avatarId == opt.id
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) colors.selectedSurface else colors.secondaryCard)
                                    .border(
                                        if (isSelected) 2.dp else 1.dp,
                                        if (isSelected) colors.selectedBorder else colors.border,
                                        CircleShape
                                    )
                                    .clickable { viewModel.updateAvatar(opt.id, opt.emoji) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = opt.emoji, fontSize = 24.sp)
                            }
                        }
                    }
                }
            }
        }

        // Section: Appearance & Theme (All 8 Modes + Live Swatch Preview)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = colors.primaryCard),
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Palette, contentDescription = null, tint = colors.primaryAccent)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Theme & Colors",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.primaryText
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Theme Mode",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.primaryText
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val themePresets = listOf(
                        "LIGHT" to "Light",
                        "MINIMAL_LIGHT" to "Minimal Light",
                        "DARK" to "Dark",
                        "MIDNIGHT_BLUE" to "Midnight Blue",
                        "AMOLED_BLACK" to "AMOLED Black",
                        "AURORA" to "Aurora",
                        "EMERALD" to "Emerald",
                        "SYSTEM" to "System"
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (row in themePresets.chunked(2)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for ((mode, label) in row) {
                                    val isSelected = userSettings.themeMode == mode
                                    val preview = remember(mode, userSettings.customAccent, systemIsDark) {
                                        buildVuColors(mode, userSettings.customAccent, systemIsDark)
                                    }
                                    Card(
                                        onClick = { viewModel.updateThemeMode(mode) },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) colors.selectedSurface else colors.secondaryCard
                                        ),
                                        border = BorderStroke(
                                            if (isSelected) 2.dp else 1.dp,
                                            if (isSelected) colors.selectedBorder else colors.border
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("theme_option_$mode")
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                // Live mini theme swatch (bg + accent)
                                                Box(
                                                    modifier = Modifier
                                                        .size(20.dp)
                                                        .clip(CircleShape)
                                                        .background(preview.background)
                                                        .border(1.dp, colors.border, CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(10.dp)
                                                            .clip(CircleShape)
                                                            .background(preview.primaryAccent)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = label,
                                                    fontSize = 12.sp,
                                                    color = colors.primaryText,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Active",
                                                    tint = colors.primaryAccent,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Accent Color",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.primaryText
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val accentKeys = listOf("CYAN", "VIOLET", "EMERALD", "CORAL", "GOLD", "ROSE")

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(accentKeys) { key ->
                            val isSelected = userSettings.customAccent == key
                            val isModernLight = userSettings.themeMode == "LIGHT" || (userSettings.themeMode == "SYSTEM" && !systemIsDark)
                            val swatchColor = getAccentColor(key, isLight = colors.isLight, isModernLight = isModernLight)
                            val checkTint = if (swatchColor.luminance() > 0.36f) Color(0xFF0B1320) else Color.White
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(swatchColor)
                                    .border(
                                        if (isSelected) 3.dp else 1.dp,
                                        if (isSelected) colors.primaryText else colors.border,
                                        CircleShape
                                    )
                                    .clickable { viewModel.updateCustomAccent(key) }
                                    .testTag("accent_option_$key"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = checkTint,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section: Transfer Performance & Privacy Controls
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = colors.primaryCard),
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Speed, contentDescription = null, tint = colors.primaryAccent)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Transfer Performance & Privacy",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.primaryText
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Group Broadcast Parallel Streams",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.primaryText
                    )
                    Text(
                        text = "Concurrent recipient connections during group share (higher uses more Wi-Fi bandwidth)",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.secondaryText
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1, 2, 3).forEach { count ->
                            FilterChip(
                                selected = userSettings.groupTransferConcurrency == count,
                                onClick = { viewModel.updateGroupConcurrency(count) },
                                label = { Text("${count}x Stream${if (count > 1) "s" else ""}") },
                                shape = RoundedCornerShape(10.dp),
                                colors = vuFilterChipColors(),
                                border = BorderStroke(
                                    1.dp,
                                    if (userSettings.groupTransferConcurrency == count) colors.primaryAccent else colors.border
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = "Record Transfer History",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.primaryText
                            )
                            Text(
                                text = "Log completed transfers locally",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.secondaryText
                            )
                        }
                        Switch(
                            checked = userSettings.historyRecordingEnabled,
                            onCheckedChange = {
                                viewModel.updateHistoryPrivacy(
                                    recordingEnabled = it,
                                    retentionDays = userSettings.historyRetentionDays,
                                    hideFilenamesNotif = userSettings.hideFilenamesInNotifications
                                )
                            },
                            colors = vuSwitchColors()
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = "Hide Filenames in Notifications",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.primaryText
                            )
                            Text(
                                text = "Keep transfer details private on lock screen",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.secondaryText
                            )
                        }
                        Switch(
                            checked = userSettings.hideFilenamesInNotifications,
                            onCheckedChange = {
                                viewModel.updateHistoryPrivacy(
                                    recordingEnabled = userSettings.historyRecordingEnabled,
                                    retentionDays = userSettings.historyRetentionDays,
                                    hideFilenamesNotif = it
                                )
                            },
                            colors = vuSwitchColors()
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = colors.divider)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp)
                        ) {
                            Icon(
                                imageVector = if (notificationsGranted) {
                                    Icons.Default.NotificationsActive
                                } else {
                                    Icons.Default.NotificationsOff
                                },
                                contentDescription = null,
                                tint = if (notificationsGranted) colors.success else colors.warning,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.settings_notifications_title),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.primaryText
                                )
                                Text(
                                    text = stringResource(
                                        if (notificationsGranted) {
                                            R.string.settings_notifications_enabled
                                        } else {
                                            R.string.settings_notifications_disabled
                                        }
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.secondaryText
                                )
                            }
                        }

                        if (notificationsGranted) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = colors.success.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, colors.success.copy(alpha = 0.45f)),
                                modifier = Modifier.testTag("settings_notifications_allowed_badge")
                            ) {
                                Text(
                                    text = stringResource(R.string.settings_notifications_allowed_badge),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.success,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        !NotificationPermissionHelper.isPermanentlyDenied(context)
                                    ) {
                                        NotificationPermissionHelper.markNotificationRationalePrompted(context)
                                        NotificationPermissionHelper.markSystemPromptRequested(context)
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        NotificationPermissionHelper.openAppNotificationSettings(context)
                                    }
                                },
                                border = BorderStroke(1.dp, colors.primaryAccent),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("settings_enable_notifications_button")
                            ) {
                                Text(
                                    text = stringResource(R.string.settings_notifications_enable_action),
                                    color = colors.primaryAccent,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: Privacy & Security Explanation Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = colors.primaryCard),
                border = BorderStroke(1.dp, colors.success.copy(alpha = 0.55f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = colors.success)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Privacy & Minimal Permissions",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.primaryText
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "• Zero Cloud Upload: All transfers happen 100% peer-to-peer locally.\n" +
                                "• Storage Access Framework: No broad storage or contacts access.\n" +
                                "• Single-Pass SHA-256: Zero-overhead integrity verification.\n" +
                                "• Explicit Approval: Transfers only start when the receiver accepts.\n" +
                                "• Path Traversal Protected: Malicious filenames are sanitized.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.secondaryText,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Section: Home Screen Settings
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = colors.primaryCard),
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_screen_settings_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DashboardCustomize,
                            contentDescription = null,
                            tint = colors.primaryAccent
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Home Screen Settings",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.primaryText
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp)
                        ) {
                            Text(
                                text = "Completely Hide Recent Transfers on Homepage",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.primaryText
                            )
                            Text(
                                text = "Removes the Recent Transfers section and placeholders from the home screen, overriding visibility preferences. Full history remains available under View All History.",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.secondaryText,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        Switch(
                            checked = userSettings.hideRecentTransfersOnHome,
                            onCheckedChange = { viewModel.updateHideRecentTransfersOnHome(it) },
                            colors = vuSwitchColors(),
                            modifier = Modifier.testTag("hide_recent_transfers_toggle")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = colors.divider)
                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = onNavigateToHistory,
                        modifier = Modifier.testTag("settings_view_history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = colors.primaryAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Open View All History",
                            color = colors.primaryAccent,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Section: About VU Share
        item {
            Card(
                onClick = onNavigateToAbout,
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = colors.primaryCard),
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_about_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = colors.primaryAccent)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "About ${appMeta.appName}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.primaryText
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Open About",
                            tint = colors.secondaryText
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "${appMeta.appName} v${appMeta.versionName} (Build ${appMeta.versionCode}) • ${AppBrandConfig.TAGLINE}\n" +
                                "Privacy statement, open-source licenses, inspiration notice, and developer contact.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.secondaryText
                    )
                }
            }
        }

        // ===========================================================
        // TEMPORARY DEVELOPER OPTIONS - REMOVE BEFORE PRODUCTION
        // ===========================================================
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = colors.secondaryCard),
                border = BorderStroke(
                    1.dp,
                    if (userSettings.developerOptionsEnabled) colors.warning.copy(alpha = 0.7f) else colors.border
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("developer_options_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeveloperMode,
                                contentDescription = null,
                                tint = colors.warning
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Developer Options",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.primaryText
                                )
                                Text(
                                    text = "Disabled by default • Offline crash report testing tools",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.secondaryText
                                )
                            }
                        }
                        Switch(
                            checked = userSettings.developerOptionsEnabled,
                            onCheckedChange = { viewModel.updateDeveloperOptionsEnabled(it) },
                            colors = vuSwitchColors(),
                            modifier = Modifier.testTag("developer_options_toggle")
                        )
                    }

                    if (userSettings.developerOptionsEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = colors.divider)
                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { viewModel.simulateSyntheticCrashReport() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("dev_simulate_crash_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.primaryAccent,
                                contentColor = colors.onPrimaryAccent
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Science,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Simulate Crash Report",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Opens the crash consent UI with a synthetic TEST REPORT so you can test email formatting without crashing.",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.secondaryText
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = { showRealCrashConfirmDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("dev_trigger_real_crash_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, colors.error.copy(alpha = 0.7f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.BugReport,
                                contentDescription = null,
                                tint = colors.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Trigger Test Crash (Next-Launch Recovery)",
                                color = colors.error,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }

    if (showRealCrashConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showRealCrashConfirmDialog = false },
            containerColor = colors.elevatedSurface,
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = colors.error
                )
            },
            title = {
                Text(
                    text = "Trigger Real Test Crash?",
                    color = colors.primaryText,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This intentionally throws an unhandled RuntimeException and closes VU Share so you can verify local crash persistence and the consent screen on next launch.",
                    color = colors.secondaryText
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRealCrashConfirmDialog = false
                        throw RuntimeException("VU Share Developer Options intentional crash recovery test")
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.error,
                        contentColor = colors.onError
                    )
                ) {
                    Text("Crash App Now", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showRealCrashConfirmDialog = false },
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Text("Cancel", color = colors.primaryText)
                }
            }
        )
    }
}
