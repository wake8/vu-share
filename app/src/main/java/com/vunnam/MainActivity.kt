package com.vunnam

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vunnam.model.TransferStatus
import com.vunnam.ui.MainViewModel
import com.vunnam.ui.components.ClipboardShareDialog
import com.vunnam.ui.components.IncomingProposalDialog
import com.vunnam.ui.components.NotificationPermissionHelper
import com.vunnam.ui.components.NotificationPermissionRationaleDialog
import com.vunnam.ui.navigation.VuDestinations
import com.vunnam.ui.screens.AboutScreen
import com.vunnam.ui.screens.CrashConsentScreen
import com.vunnam.ui.screens.DiscoveryScreen
import com.vunnam.ui.screens.GroupShareScreen
import com.vunnam.ui.screens.HistoryScreen
import com.vunnam.ui.screens.HomeScreen
import com.vunnam.ui.screens.PhoneMigrationScreen
import com.vunnam.ui.screens.QrScanScreen
import com.vunnam.ui.screens.ReceiveScreen
import com.vunnam.ui.screens.SendPickerScreen
import com.vunnam.ui.screens.SettingsScreen
import com.vunnam.ui.screens.TransferProgressScreen
import com.vunnam.ui.theme.VuShareTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle external share intents if launched from another app
        handleIncomingShareIntent(intent)

        setContent {
            val userSettings by viewModel.userSettings.collectAsState()
            val incomingProposal by viewModel.incomingProposal.collectAsState()
            val activeSession by viewModel.activeSession.collectAsState()
            val activeCrashReport by viewModel.activeCrashReport.collectAsState()
            val pendingCrashCount by viewModel.pendingCrashCount.collectAsState()
            val emailAppOpenedForReportId by viewModel.emailAppOpenedForReportId.collectAsState()
            val crashEmailError by viewModel.crashEmailError.collectAsState()
            val navController = rememberNavController()
            val currentBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = currentBackStackEntry?.destination?.route
            var showClipboardDialog by remember { mutableStateOf(false) }
            var showNotificationRationale by remember { mutableStateOf(false) }

            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { _ ->
                // Transfers proceed regardless of choice; granted enables progress panel notifications.
            }

            // Contextually prompt once on Android 13+ when entering sharing/receiving/transfer flows
            LaunchedEffect(currentRoute, activeSession?.status, activeCrashReport) {
                val isTransferContext = currentRoute in setOf(
                    VuDestinations.DISCOVERY,
                    VuDestinations.RECEIVE,
                    VuDestinations.GROUP_SHARE,
                    VuDestinations.TRANSFER_PROGRESS
                ) || activeSession?.status == TransferStatus.TRANSFERRING

                if (isTransferContext &&
                    activeCrashReport == null &&
                    NotificationPermissionHelper.requiresRuntimePermission() &&
                    !NotificationPermissionHelper.isNotificationPermissionGranted(this@MainActivity) &&
                    !NotificationPermissionHelper.hasPromptedNotificationRationale(this@MainActivity)
                ) {
                    showNotificationRationale = true
                }
            }

            // If an active session starts transferring while user is in Receive screen, auto-navigate to progress
            LaunchedEffect(activeSession?.status) {
                if (activeSession?.status == TransferStatus.TRANSFERRING) {
                    val activeRoute = navController.currentBackStackEntry?.destination?.route
                    if (activeRoute != VuDestinations.TRANSFER_PROGRESS) {
                        navController.navigate(VuDestinations.TRANSFER_PROGRESS) {
                            launchSingleTop = true
                        }
                    }
                }
            }

            VuShareTheme(
                themePreference = userSettings.themeMode,
                customAccent = userSettings.customAccent
            ) {
                val vuColors = VuShareTheme.colors
                val view = androidx.compose.ui.platform.LocalView.current
                if (!view.isInEditMode) {
                    androidx.compose.runtime.SideEffect {
                        val window = (view.context as? android.app.Activity)?.window
                        if (window != null) {
                            androidx.core.view.WindowCompat.getInsetsController(window, view).apply {
                                isAppearanceLightStatusBars = vuColors.isLight
                                isAppearanceLightNavigationBars = vuColors.isLight
                            }
                        }
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = vuColors.background,
                    contentWindowInsets = WindowInsets.safeDrawing
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        NavHost(
                            navController = navController,
                            startDestination = VuDestinations.HOME
                        ) {
                            composable(VuDestinations.HOME) {
                                HomeScreen(
                                    viewModel = viewModel,
                                    onNavigateToSend = { navController.navigate(VuDestinations.SEND_PICKER) },
                                    onNavigateToReceive = { navController.navigate(VuDestinations.RECEIVE) },
                                    onNavigateToGroupShare = { navController.navigate(VuDestinations.GROUP_SHARE) },
                                    onNavigateToHistory = { navController.navigate(VuDestinations.HISTORY) },
                                    onNavigateToSettings = { navController.navigate(VuDestinations.SETTINGS) },
                                    onNavigateToQrScan = { navController.navigate(VuDestinations.QR_SCAN) },
                                    onNavigateToMigration = { navController.navigate(VuDestinations.PHONE_MIGRATION) },
                                    onNavigateToDeviceManagement = { navController.navigate(VuDestinations.DEVICE_MANAGEMENT) },
                                    onOpenClipboardShare = { showClipboardDialog = true }
                                )
                            }

                            composable(VuDestinations.SEND_PICKER) {
                                SendPickerScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() },
                                    onProceedToDiscovery = { navController.navigate(VuDestinations.DISCOVERY) }
                                )
                            }

                            composable(VuDestinations.DISCOVERY) {
                                DiscoveryScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() },
                                    onNavigateToQrScan = { navController.navigate(VuDestinations.QR_SCAN) },
                                    onTransferStarted = { navController.navigate(VuDestinations.TRANSFER_PROGRESS) }
                                )
                            }

                            composable(VuDestinations.RECEIVE) {
                                ReceiveScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(VuDestinations.GROUP_SHARE) {
                                GroupShareScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() },
                                    onNavigateToPicker = { navController.navigate(VuDestinations.SEND_PICKER) },
                                    onTransferStarted = { navController.navigate(VuDestinations.TRANSFER_PROGRESS) }
                                )
                            }

                            composable(VuDestinations.TRANSFER_PROGRESS) {
                                TransferProgressScreen(
                                    viewModel = viewModel,
                                    onDone = {
                                        navController.navigate(VuDestinations.HOME) {
                                            popUpTo(VuDestinations.HOME) { inclusive = true }
                                        }
                                    }
                                )
                            }

                            composable(VuDestinations.HISTORY) {
                                HistoryScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(VuDestinations.SETTINGS) {
                                SettingsScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() },
                                    onNavigateToHistory = { navController.navigate(VuDestinations.HISTORY) },
                                    onNavigateToAbout = { navController.navigate(VuDestinations.ABOUT) }
                                )
                            }

                            composable(VuDestinations.ABOUT) {
                                AboutScreen(
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(VuDestinations.QR_SCAN) {
                                QrScanScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() },
                                    onPairSuccess = {
                                        navController.navigate(VuDestinations.DISCOVERY)
                                    }
                                )
                            }

                            composable(VuDestinations.PHONE_MIGRATION) {
                                PhoneMigrationScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() },
                                    onProceedToDiscovery = { navController.navigate(VuDestinations.DISCOVERY) }
                                )
                            }

                            composable(VuDestinations.DEVICE_MANAGEMENT) {
                                com.vunnam.ui.screens.DeviceManagementScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }

                        // Global Incoming Proposal Dialog
                        if (incomingProposal != null) {
                            val availableBytes = remember(incomingProposal?.sessionId) {
                                viewModel.getAvailableStorageBytes()
                            }
                            IncomingProposalDialog(
                                proposal = incomingProposal!!,
                                availableStorageBytes = availableBytes,
                                onAccept = {
                                    viewModel.acceptIncomingTransfer(incomingProposal!!)
                                    navController.navigate(VuDestinations.TRANSFER_PROGRESS) {
                                        launchSingleTop = true
                                    }
                                },
                                onReject = {
                                    viewModel.rejectIncomingTransfer(incomingProposal!!)
                                }
                            )
                        }

                        // Clipboard Share Dialog
                        if (showClipboardDialog) {
                            ClipboardShareDialog(
                                initialText = viewModel.clipboardShareText.value,
                                onDismiss = { showClipboardDialog = false },
                                onSendClipboardText = { text ->
                                    viewModel.setClipboardShareText(text)
                                    showClipboardDialog = false
                                    // Save as temporary text file to beam to peer
                                    val tempFile = java.io.File(cacheDir, "clipboard_note_${System.currentTimeMillis()}.txt").apply {
                                        writeText(text)
                                    }
                                    viewModel.clearFileSelection()
                                    viewModel.addCustomPickedFiles(listOf(Uri.fromFile(tempFile)))
                                    navController.navigate(VuDestinations.DISCOVERY)
                                }
                            )
                        }
                        // Contextual Transfer Notification Permission Rationale (Android 13+)
                        if (showNotificationRationale && activeCrashReport == null) {
                            NotificationPermissionRationaleDialog(
                                onAllow = {
                                    showNotificationRationale = false
                                    NotificationPermissionHelper.markNotificationRationalePrompted(this@MainActivity)
                                    NotificationPermissionHelper.markSystemPromptRequested(this@MainActivity)
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                },
                                onNotNow = {
                                    showNotificationRationale = false
                                    NotificationPermissionHelper.markNotificationRationalePrompted(this@MainActivity)
                                }
                            )
                        }

                        // Offline Crash Report Consent Screen (next-launch recovery or simulated test report)
                        val crashReport = activeCrashReport
                        if (crashReport != null) {
                            CrashConsentScreen(
                                report = crashReport,
                                pendingCrashCount = pendingCrashCount,
                                emailAppOpened = emailAppOpenedForReportId == crashReport.id,
                                emailErrorMessage = crashEmailError,
                                onSendCrashReport = { viewModel.sendActiveCrashReport() },
                                onNotNow = { viewModel.deferActiveCrashReport() },
                                onDiscardReport = { viewModel.discardActiveCrashReport() },
                                onDismissEmailError = { viewModel.dismissCrashEmailError() }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingShareIntent(intent)
    }

    private fun handleIncomingShareIntent(intent: Intent?) {
        if (intent == null) return
        when (intent.action) {
            Intent.ACTION_SEND -> {
                val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
                if (uri != null) {
                    viewModel.clearFileSelection()
                    viewModel.addCustomPickedFiles(listOf(uri))
                } else {
                    val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                    if (!sharedText.isNullOrBlank()) {
                        viewModel.setClipboardShareText(sharedText)
                    }
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val uris = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
                }
                if (!uris.isNullOrEmpty()) {
                    viewModel.clearFileSelection()
                    viewModel.addCustomPickedFiles(uris)
                }
            }
        }
    }
}
